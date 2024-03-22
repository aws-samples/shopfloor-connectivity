// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.opcua

import com.amazonaws.sfc.channels.channelSubmitEventHandler
import com.amazonaws.sfc.channels.submit
import com.amazonaws.sfc.crypto.CertificateFormat
import com.amazonaws.sfc.crypto.CertificateHelper
import com.amazonaws.sfc.crypto.PkcsCertificateHelper
import com.amazonaws.sfc.crypto.subjectAlternativeApplicationUri
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.MetricDimensions
import com.amazonaws.sfc.metrics.MetricUnits
import com.amazonaws.sfc.metrics.MetricsCollector
import com.amazonaws.sfc.opcua.FilterHelper.Companion.DEFAULT_EVENT_TYPE
import com.amazonaws.sfc.opcua.FilterHelper.Companion.UNKNOWN_EVENT_TYPE
import com.amazonaws.sfc.opcua.config.*
import com.amazonaws.sfc.opcua.config.OpcuaConfiguration.Companion.CONFIG_CHANGED_DATA_CHANNEL_SIZE
import com.amazonaws.sfc.opcua.config.OpcuaConfiguration.Companion.CONFIG_CHANGED_DATA_CHANNEL_TIMEOUT
import com.amazonaws.sfc.opcua.config.OpcuaConfiguration.Companion.CONFIG_RECEIVED_EVENTS_CHANNEL_SIZE
import com.amazonaws.sfc.opcua.config.OpcuaConfiguration.Companion.CONFIG_RECEIVED_EVENTS_CHANNEL_TIMEOUT
import com.amazonaws.sfc.system.DateTime
import com.amazonaws.sfc.system.DateTime.add
import com.amazonaws.sfc.system.DateTime.systemDateTime
import com.amazonaws.sfc.system.DateTime.systemDateUTC
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.isJobCancellationException
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.eclipse.milo.opcua.sdk.client.OpcUaClient
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscriptionManager
import org.eclipse.milo.opcua.stack.client.security.DefaultClientCertificateValidator
import org.eclipse.milo.opcua.stack.core.AttributeId
import org.eclipse.milo.opcua.stack.core.Identifiers
import org.eclipse.milo.opcua.stack.core.StatusCodes
import org.eclipse.milo.opcua.stack.core.UaException
import org.eclipse.milo.opcua.stack.core.channel.MessageLimits
import org.eclipse.milo.opcua.stack.core.serialization.SerializationContext
import org.eclipse.milo.opcua.stack.core.types.builtin.*
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint
import org.eclipse.milo.opcua.stack.core.types.enumerated.*
import org.eclipse.milo.opcua.stack.core.types.structured.*
import org.eclipse.milo.opcua.stack.core.util.EndpointUtil
import sun.security.x509.X509CertImpl
import java.net.URI
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.Period
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.measureTime
import kotlin.time.toDuration


open class OpcuaSource(
    private val sourceID: String,
    private val configuration: OpcuaConfiguration,
    private val clientHandleAtomic: AtomicInteger,
    private val logger: Logger,
    private val metricsCollector: MetricsCollector?,
    adapterMetricDimensions: MetricDimensions?
) {

    // Working storage for nodes being read
    inner class OpcuaNodeData(
        val channelID: String,
        val readValueId: ReadValueId,
        val eventType: String?,
        val nodeEventSampleInterval: Int?,
        val eventProperties: List<Pair<NodeId, QualifiedName>>?
    ) {
        val isDataNode = eventType.isNullOrBlank()
        val isEventNode = !isDataNode

    }


    inner class SubscriptionListener(
        private val logger: Logger,
        private val fnOnPublishFailure: (UaException?) -> Unit,
        private val fnOnSubscriptionTransferFailed: (UaSubscription?, StatusCode?) -> Unit
    ) : UaSubscriptionManager.SubscriptionListener {
        override fun onPublishFailure(exception: UaException?) {
            val log = logger.getCtxLoggers(this::class.java.name, "fnOnPublishFailure")
            try {
                log.warning("fnOnPublishFailure event received with status ${exception?.statusCode.toString()}")
                fnOnPublishFailure(exception)
            } catch (e: Exception) {
                log.errorEx("Error executing onSubscriptionTransferFailedAction", e)
            }
        }

        override fun onSubscriptionTransferFailed(subscription: UaSubscription?, statusCode: StatusCode?) {
            val log = logger.getCtxLoggers(this::class.java.name, "onSubscriptionTransferFailed")
            try {
                log.warning("onSubscriptionTransferFailed event received with status code ${statusCode.toString()} ")
                fnOnSubscriptionTransferFailed(subscription, statusCode)
            } catch (e: Exception) {
                log.errorEx("Error executing onSubscriptionTransferFailedAction", e)
            }
        }
    }

    private val className = this::class.simpleName.toString()
    private val sourceScope = buildScope("OPCUA Source")

    private var isClosing: Boolean = false

    private var connectionWatchdog: Job? = null

    // configuration for this source instance
    private val sourceConfiguration = configuration.sources[sourceID]
        ?: throw OpcuaSourceException(sourceID, "Unknown source identifier, available sources are ${configuration.sources.keys}")

    // adapter configuration for source
    private val protocolAdapterID = sourceConfiguration.protocolAdapterID
    private val opcuaAdapterConfiguration = configuration.protocolAdapters[protocolAdapterID]
        ?: throw OpcuaSourceException(
            sourceID,
            "Unknown protocol adapter identifier \"$protocolAdapterID\", available OPCUA protocol adapters are ${configuration.protocolAdapters}"
        )


    private val dimensions = mapOf(MetricsCollector.METRICS_DIMENSION_SOURCE to "$protocolAdapterID:$sourceID") + adapterMetricDimensions as Map<String, String>

    // server from the adapter used by the source instance
    private val sourceAdapterOpcuaServerID = sourceConfiguration.sourceAdapterOpcuaServerID
    private val opcuaServerConfiguration = opcuaAdapterConfiguration.opcuaServers[sourceAdapterOpcuaServerID]
        ?: throw OpcuaSourceException(
            sourceID,
            "Unknown protocol adapter OPCUA server identifier \"$sourceAdapterOpcuaServerID\", available servers for adapter \"$protocolAdapterID\" are ${opcuaAdapterConfiguration.opcuaServers.keys}"
        )


    // optional profile for a server which does contain additional event/alarm types
    private val serverProfile =
        if (opcuaServerConfiguration.serverProfile != null)
            opcuaAdapterConfiguration.serverProfiles[opcuaServerConfiguration.serverProfile]
                ?: throw OpcuaSourceException(
                    sourceID,
                    "Unknown server profile \"${opcuaServerConfiguration.serverProfile}\", available profiles for adapter \"$protocolAdapterID\" are ${opcuaAdapterConfiguration.serverProfiles.keys}"
                )
        else OpcuaServerProfileConfiguration()

    // batch size for interacting with the server
    private val batchSize = opcuaServerConfiguration.readBatchSize

    // last fault whilst reading from the server
    private var sourceServerFault: ServiceFault? = null

    // subscription and monitored items for this source
    private var subscription: UaSubscription? = null

    // backup field for client, used explicit field to allow testing the actual value without creating a new one on demand
    private var _opcuaClient: OpcUaClient? = null

    private var certificateExpiryChecker: Job? = null

    // opcua client used to interact with server
    private val client
        get() = runBlocking { getClient() }

    // gets a OPCUA client for a source
    suspend fun getClient(): OpcUaClient? {

        // pause after errors
        if (systemDateTime() < pauseWaitUntil) {
            return null
        }

        // use existing client
        if ((_opcuaClient != null)) {
            return _opcuaClient
        }

        connectionWatchdog?.cancel()
        connectionWatchdog = null

        // or create a new client
        createClientLock.withLock {
            if (_opcuaClient == null) {
                _opcuaClient = createOpcuaClient()
                subscription = _opcuaClient?.let { createSubscriptionWithMonitoredItems(it) }
                _opcuaClient?.addFaultListener { fault ->
                    // Bad_NoSubscription is returned incorrectly after creating a new connection with subscriptions
                    if (fault.responseHeader.serviceResult != StatusCode(StatusCodes.Bad_NoSubscription))
                        sourceServerFault = fault
                }
            }

            // after failing to connect temporary pause reading from the source
            if (_opcuaClient == null) {
                val waitingPeriod = opcuaServerConfiguration.waitAfterConnectError
                pauseWaitUntil = systemDateTime().plusMillis(waitingPeriod.inWholeMilliseconds)
                logger.getCtxInfoLog(className, "getClient")("Reading from source \"$sourceID\" paused for $waitingPeriod until $pauseWaitUntil")
            } else {
                if (_opcuaClient?.subscriptionManager?.subscriptions?.isNotEmpty() == true) {
                    _opcuaClient!!.subscriptionManager.addSubscriptionListener(
                        SubscriptionListener(logger,
                            fnOnPublishFailure = { ex ->
                                if (ex?.statusCode?.value == StatusCodes.Bad_ConnectionClosed) (resetClient(0))
                            },
                            fnOnSubscriptionTransferFailed = { _, _ -> resetClient(0) })
                    )
                    connectionWatchdog = startConnectionWatchdog()
                }
            }
        }

        return _opcuaClient
    }

    private suspend fun startConnectionWatchdog(): Job? {


        // only needed in subscription mode as in read node the read will fail anyway if connection is lost
        if (sourceConfiguration.readingMode != OpcuaSourceReadingMode.SUBSCRIPTION && _opcuaClient?.subscriptionManager?.subscriptions?.isEmpty() == true) return null

        if (opcuaServerConfiguration.connectionWatchdogInterval.inWholeMilliseconds == 0L) {
            logger.getCtxInfoLog(className, "startConnectionWatchdog")("Connection watchdog disabled as it's interval is set to 0")
        }

        return sourceScope.launch(Dispatchers.Default) {
            val log = logger.getCtxLoggers(className, "connectionWatchdog")
            while (isActive) {
                try {
                    // try to read server status
                    if (!isClosing) {
                        withTimeout(opcuaServerConfiguration.readTimeout) {
                            _opcuaClient?.read(
                                0.0, TimestampsToReturn.Source, mutableListOf(
                                    ReadValueId.builder()
                                        .nodeId(Identifiers.Server_ServerStatus_State)
                                        .build()
                                )
                            )?.get()
                        }
                        log.trace("Connection to server ${opcuaServerConfiguration.endPoint} for source \"${sourceID}\" checked")
                    }
                    runBlocking {
                        delay(opcuaServerConfiguration.connectionWatchdogInterval)
                    }
                } catch (e: Exception) {
                    if (!e.isJobCancellationException) {
                        log.errorEx("Unable to read from server ${opcuaServerConfiguration.endPoint} for source \"${sourceID}\"", e)
                        resetClient(0)
                        delay(opcuaServerConfiguration.waitAfterReadError)
                    }
                }
            }
        }
    }

    // monitored data/event nodes
    private var monitoredItems: MutableList<UaMonitoredItem>? = null

    // if an error occurs the source will pause for a configured period
    private var pauseWaitUntil: Instant = Instant.ofEpochSecond(0L)
    private val readingMode = sourceConfiguration.readingMode

    // lock to prevent creation of clients whilst one is being created
    private val createClientLock = Mutex()

    // nodes indexed by client handle used to create a subscription for that node
    private val clientHandlesForNodes = mutableMapOf<Int, OpcuaNodeData>()

    private val anyEventNodes by lazy {
        sourceConfiguration.channels.values.any { channel: OpcuaNodeChannelConfiguration -> channel.isEventNode }
    }
    private val inSubscriptionReadingMode = readingMode == OpcuaSourceReadingMode.SUBSCRIPTION
    private val eventsHelper = if (anyEventNodes) OpcuaProfileEventsHelper(serverProfile) else null

    private val allValidTypesStr =
        eventsHelper?.allEventClasses?.sortedBy { it.first }?.map { "${it.first} (ns=${it.second?.namespaceIndex}i=${it.second?.identifier})" }

    private var trustManager: ClientTrustListManager? = null

    private val OpcuaNodeChannelConfiguration.eventProperties: List<Pair<NodeId, QualifiedName>>
        get() = if (this.isEventNode) {
            val event = eventsHelper?.findEvent(this.nodeEventType)
            event?.properties ?: eventsHelper?.findEvent(DEFAULT_EVENT_TYPE)?.properties ?: emptyList()
        } else emptyList()

    // working set of node data for the source instance
    private val sourceNodes by lazy {

        sourceConfiguration.channels.map { (channelID, nodeChannel) ->
            val attribute = if (nodeChannel.isDataNode) AttributeId.Value else AttributeId.EventNotifier

            val readValueId = ReadValueId(nodeChannel.nodeID, attribute.uid(), nodeChannel.indexRange, QualifiedName.NULL_VALUE)
            val configuredEventType = nodeChannel.nodeEventType

            val eventType = checkNodeEventType(configuredEventType, nodeChannel, channelID)

            channelID to OpcuaNodeData(
                channelID = channelID,
                readValueId = readValueId,
                eventType = eventType,
                nodeEventSampleInterval = nodeChannel.eventSamplingInterval,
                eventProperties = nodeChannel.eventProperties
            )
        }.toMap()
    }

    private fun checkNodeEventType(eventType: String?, nodeChannel: OpcuaNodeChannelConfiguration, channelID: String): String? =

        if (eventType.isNullOrBlank() || eventsHelper?.isKnownEvent(eventType) == true) eventType
        else {
            logger.getCtxWarningLog(className, "checkNodeEventType")(
                "Event type \"${nodeChannel.nodeEventType}\" for source \"$sourceID\" channel \"$channelID\" is not a valid event type, " +
                        "use the name or node id of any of the following valid event types $allValidTypesStr, " +
                        "adapter will collect events of any type and will use type \"$DEFAULT_EVENT_TYPE\" to collect the event properties"
            )
            UNKNOWN_EVENT_TYPE
        }


    // *** Data subscriptions ***

    // store for values changes for a monitored data node
    private val dataValueChangesStore = if (inSubscriptionReadingMode) SourceDataValuesStore<ChannelReadValue>() else null

    // channel to send data changes to the consuming coroutine that is handling data changes
    private val dataValueChangeChannel =
        if (inSubscriptionReadingMode) Channel<Pair<UaMonitoredItem, ChannelReadValue>>(opcuaAdapterConfiguration.changedDataChannelSize) else null

    // coroutine handling changed data for subscriptions
    private val changedDataWorker = if (inSubscriptionReadingMode && dataValueChangeChannel != null)
        sourceScope.launch(context = Dispatchers.Default, name = "$sourceID Data Subscription Handler") {
            dataChangeTask(dataValueChangeChannel, dataValueChangesStore!!, this)
        } else null

    private suspend fun dataChangeTask(
        channel: Channel<Pair<UaMonitoredItem, ChannelReadValue>>,
        store: SourceDataValuesStore<ChannelReadValue>,
        scope: CoroutineScope
    ) {
        val log = logger.getCtxLoggers(className, "changedDataWorker")

        while (scope.isActive) {
            val (monitoredItem, data) = channel.receive()
            try {
                // find the node using the client handle
                val clientHandle = monitoredItem.clientHandle.toInt()
                val node = clientHandlesForNodes[clientHandle]

                if (node != null) {
                    log.trace("Received subscription data for source \"$sourceID\", \"${node.channelID}\"")

                    val duration = measureTime {
                        store.add(node.channelID, data)
                    }
                    log.trace("Storing changed value took $duration, number of items with changed data is ${dataValueChangesStore?.size}")

                } else {
                    log.warning("Received subscription data for source \"$sourceID\" but $clientHandle is unknown")
                }
            } catch (e: Exception) {
                logger.getCtxLoggers(className, "onSubscribedNodeData").errorEx("Error processing subscription data for source \"$sourceID\"", e)
            }
        }
    }

    // *** Event Subscriptions ***

    // store for received event data for monitored event nodes
    private val eventStore = if (anyEventNodes) SourceDataMultiValuesStore<Map<String, Any>>() else null

    // channel to send received events to the coroutine that is handling these changes
    private val receivedEventsChannel = if (anyEventNodes) Channel<Pair<UaMonitoredItem, Map<String, Any>>>(configuration.receivedEventsChannelSize) else null

    // coroutine handling received events
    private val eventHandingWorker = if (receivedEventsChannel != null) sourceScope.launch(context = Dispatchers.Default, name = "Event Data Handler") {

        val log = logger.getCtxLoggers(className, "onSubscribedEventData")
        while (isActive) {
            val (monitoredItem, eventPropertiesData) = receivedEventsChannel.receive()
            try {
                val clientHandle = monitoredItem.clientHandle.toInt()
                val node: OpcuaNodeData? = clientHandlesForNodes[clientHandle]

                if (node != null) {
                    log.trace("Received event data for source \"$sourceID\", \"${node.channelID}\"")
                    val duration = measureTime {
                        eventStore?.add(node.channelID, eventPropertiesData)
                    }
                    log.trace("Adding event data to event data store took $duration, number of events in store is ${eventStore?.size}")
                } else {
                    log.warning("Received event data for source \"$sourceID\" but $clientHandle is unknown")
                }
            } catch (e: Exception) {
                logger.getCtxLoggers(className, "eventHandingWorker").errorEx("Error processing event data for source \"$sourceID\"", e)
            }
        }
    }
    else null


    // creates the client to communicate with the server the source is reading from
    private suspend fun createOpcuaClient(): OpcUaClient? {

        val log = logger.getCtxLoggers(className, "createServerClient")

        log.info("Creating client for source \"$sourceID\", on ${opcuaServerConfiguration.endPoint}")

        // implementing this the hard way in order to extend with authentication methods later easier
        val securityPolicy = opcuaServerConfiguration.securityPolicy
        val userTokenType = UserTokenType.Anonymous


        val predicate =
            { e: EndpointDescription ->
                securityPolicy.policy.uri == e.securityPolicyUri &&
                        Arrays.stream(e.userIdentityTokens)
                            .anyMatch { p: UserTokenPolicy -> p.tokenType == userTokenType }
            }


        // build the client configuration
        val clientConfig = { configBuilder: OpcUaClientConfigBuilder ->
            configBuilder.setConnectTimeout(UInteger.valueOf(opcuaServerConfiguration.connectTimeout.inWholeMilliseconds))
                .setRequestTimeout(UInteger.valueOf(opcuaServerConfiguration.readTimeout.inWholeMilliseconds))
                .setMessageLimits(messageLimits)
                .setupClientSecurity()
                .setupCertificateValidation { dir ->
                    log.info("Certificate or CRL Update to directory $dir, reconnecting client")
                    resetClient()
                }

            configBuilder.build()
        }

        // create the client
        val client = try {
            val host = URI(opcuaServerConfiguration.address).host
            OpcUaClient.create(
                opcuaServerConfiguration.endPoint,
                { endpoints: List<EndpointDescription> ->
                    endpoints.stream()
                        .filter(predicate)
                        .map { endpoint -> EndpointUtil.updateUrl(endpoint, host) }
                        .findFirst()
                }, clientConfig
            )

        } catch (e: UaException) {
            val cause = (if (e.cause != null) e.cause!!.message else e.message).toString()
            log.error("Error creating client for for source \"$sourceID\" at  ${opcuaServerConfiguration.endPoint}, $cause")
            null
        } catch (e: Exception) {
            log.errorEx("Error creating client for for source \"$sourceID\" at  ${opcuaServerConfiguration.endPoint}", e)
            null
        }

        // if the client was created connect to server
        return if (client != null) {
            try {
                val opcuaClient = client.connect().join() as OpcUaClient?
                log.info("Client for source \"$sourceID\" connected to ${opcuaServerConfiguration.endPoint}")
                metricsCollector?.put(protocolAdapterID, MetricsCollector.METRICS_CONNECTIONS, 1.0, MetricUnits.COUNT, dimensions)
                opcuaClient
            } catch (e: Exception) {
                log.errorEx("Error connecting at ${opcuaServerConfiguration.endPoint} for source \"$sourceID\"", e)
                metricsCollector?.put(protocolAdapterID, MetricsCollector.METRICS_CONNECTION_ERRORS, 1.0, MetricUnits.COUNT, dimensions)
                null
            }
        } else null

    }

    private val messageLimits: MessageLimits
        get() = MessageLimits(
            opcuaServerConfiguration.maxChunkSize,
            opcuaServerConfiguration.maxChunkCount,
            opcuaServerConfiguration.maxMessageSize
        )

    private fun OpcUaClientConfigBuilder.setupClientSecurity(): OpcUaClientConfigBuilder {

        val log = logger.getCtxLoggers(className, "OpcUaClientConfigBuilder.setupClientSecurity")

        if (opcuaServerConfiguration.securityPolicy == OpcuaSecurityPolicy.None) return this

        val certificateConfiguration = opcuaServerConfiguration.certificateConfiguration
        if (certificateConfiguration == null) {
            log.error("A certificate must be configured for a server using security policy ${opcuaServerConfiguration.securityPolicy}")
            return this
        }

        if (certificateConfiguration.format == CertificateFormat.Unknown) {
            log.error("Type of certificate could not be determined from filename of format configuration")
            return this
        }

        if (certificateConfiguration.format != certificateConfiguration.certificateFileFormatFromName()) {
            log.warning("WARNING certificate type from filename does possibly not match with specified format")
        }

        log.trace("Certificate is of format ${certificateConfiguration.format}")
        val certificateHelper = when (certificateConfiguration.format) {
            CertificateFormat.Pkcs12 -> PkcsCertificateHelper(certificateConfiguration, logger)
            else -> CertificateHelper(certificateConfiguration, logger)
        }

        val (certificate, keyPair) = certificateHelper.getCertificateAndKeyPair()
        certificateExpiryChecker = startCertificateExpiryChecker(certificate)

        if (certificate == null) return this
        log.trace("Certificate is $certificate")
        this.setCertificate(certificate)

        if (keyPair == null) return this
        this.setKeyPair(keyPair)


        val uri = (certificate as? X509CertImpl?)?.subjectAlternativeApplicationUri
        if (uri == null) {
            log.warning("Application URI is not set in certificate")
        } else {
            log.trace("Application URI set to $uri")
            this.setApplicationUri(uri.toASCIIString())
        }

        val applicationName = uri?.schemeSpecificPart
        if (uri == null) {
            log.warning("Application is not set in certificate")
        } else {
            val localizedApplicationName = LocalizedText(applicationName)
            log.trace("Application name set to $localizedApplicationName")
            this.setApplicationName(localizedApplicationName)
        }

        return this
    }

    private fun startCertificateExpiryChecker(certificate: X509Certificate?): Job? {

        val certificateConfiguration = opcuaServerConfiguration.certificateConfiguration
        val expirationWarningPeriod = certificateConfiguration?.expirationWarningPeriod ?: 0

        if (certificate == null || certificateConfiguration == null || expirationWarningPeriod <= 0) {
            certificateExpiryChecker?.cancel()
            return null
        }

        return sourceScope.launch("OPCUA Certificate Expiry Watcher", Dispatchers.IO) {

            try {
                while (isActive) {

                    val now = systemDateUTC()
                    if (certificate.notAfter <= now.add(Period.ofDays(expirationWarningPeriod * -1))) {
                        val ctxLog = logger.getCtxLoggers(className, "Check Certificate Expiration")
                        if (certificate.notAfter >= now) {
                            ctxLog.error("Certificate expired at ${certificate.notAfter}")
                        } else {
                            val daysBetween = ChronoUnit.DAYS.between(certificate.notAfter.toInstant(), now.toInstant())
                            ctxLog.warning("Certificate will expire in $daysBetween days at ${certificate.notAfter}")
                        }
                    }
                    DateTime.delayUntilNextMidnightUTC()
                }
            } catch (e: Exception) {
                logger.getCtxErrorLogEx(className, "startCertificateExpiryChecker")("Error while checking certificate expiration", e)
            }
        }

    }

    private fun OpcUaClientConfigBuilder.setupCertificateValidation(onUpdate: (String) -> Unit): OpcUaClientConfigBuilder {
        if (opcuaServerConfiguration.securityPolicy == OpcuaSecurityPolicy.None) return this

        val validationConfiguration = opcuaServerConfiguration.certificateValidationConfiguration ?: return this
        if (!validationConfiguration.active) return this

        val log = logger.getCtxLoggers(className, "setupCertificateValidation")

        if (!Path(validationConfiguration.directory).exists()) {
            log.error("Directory ${validationConfiguration.directory} does not exist")
            return this
        }

        log.trace("Loading certificated and CLRs from directory ${validationConfiguration.directory}")
        trustManager = ClientTrustListManager(validationConfiguration.directory, logger) { dir ->
            log.info("Certificate or CLR update in directory \"$dir\"")
            onUpdate(dir.toString())
        }

        if (trustManager?.trustedCertificates?.isEmpty() == true) {
            log.warning("No trusted certificates in trusted certificates directory ${trustManager!!.trustedCertificatesDirectory}")
            trustManager?.close()
            trustManager = null
            return this
        }

        log.trace("Certificate validation options ${validationConfiguration.configurationOptions.options}")
        val certificateValidator = DefaultClientCertificateValidator(trustManager, validationConfiguration.configurationOptions.options)
        this.setCertificateValidator(certificateValidator)
        return this

    }


    private fun createSubscriptionWithMonitoredItems(client: OpcUaClient): UaSubscription? {

        val sourceNeedsSubscription = inSubscriptionReadingMode || anyEventNodes
        if (!sourceNeedsSubscription) return null

        // get the smallest interval for the source from all schedules in which it is used
        val minIntervalUsedInSchedulesForSource: Duration =
            configuration.schedules
                .filter { sourceID in it.activeSourceIDs }
                .minByOrNull { schedule -> schedule.interval }?.interval ?: DEFAULT_INTERVAL

        val configuredSourceInterval = sourceConfiguration.subscribePublishingInterval

        // create new subscription
        val connectTimeOut = opcuaServerConfiguration.connectTimeout.inWholeMilliseconds
        val createdSubscription = run {
            val interval = (configuredSourceInterval ?: minIntervalUsedInSchedulesForSource).toDouble(DurationUnit.MILLISECONDS)
            runBlocking {
                withTimeoutOrNull(connectTimeOut) {
                    sourceScope.async(Dispatchers.IO) {
                        client.subscriptionManager.createSubscription(interval)?.get()
                    }.await()
                }
            }
        }

        if (createdSubscription != null) {
            monitoredItems = createMonitoredItems(createdSubscription)
        }

        return createdSubscription
    }

    private val dataNodeMonitoredItemRequests by lazy {
        sequence {

            if (!inSubscriptionReadingMode) return@sequence

            val dataNodes = sourceNodes.filter { it.value.isDataNode }

            dataNodes.values.forEach { node: OpcuaNodeData ->

                val clientHandle = clientHandleAtomic.getAndIncrement()
                clientHandlesForNodes[clientHandle] = node

                val dataChangeFilter: ExtensionObject? = buildDataChangeFilter(sourceConfiguration.channels[node.channelID]?.nodeChangeFilter)
                val params = MonitoringParameters(uint(clientHandle), -1.0, dataChangeFilter, uint(1), true)
                yield(MonitoredItemCreateRequest(node.readValueId, MonitoringMode.Reporting, params))
            }
        }
    }


    private fun buildDataChangeFilter(nodeChangeFilter: OpcuaNodeChangeFilter?) =
        if (nodeChangeFilter == null) null
        else {
            val deadBandType = UInteger.valueOf(
                when (nodeChangeFilter.filterType) {
                    OpcuaChangeFilterType.ABSOLUTE -> DeadbandType.Absolute
                    OpcuaChangeFilterType.PERCENT -> DeadbandType.Percent
                }.value
            )
            val filterValue = nodeChangeFilter.filterValue
            val changeFilter = DataChangeFilter(DataChangeTrigger.StatusValue, deadBandType, filterValue)
            ExtensionObject.encode(client?.serializationContext, changeFilter)
        }

    private val eventNodeMonitoredItemRequests by lazy {
        sequence {

            val eventNodes = sourceNodes.filter { it.value.isEventNode }

            if (client == null) return@sequence

            val filterHelper = FilterHelper(client!!, sourceID, configuration, logger)

            eventNodes.values.forEach { node ->
                val clientHandle = clientHandleAtomic.getAndIncrement()
                clientHandlesForNodes[clientHandle] = node

                if (node.eventType != null) {
                    val eventFilter = filterHelper[node.eventType]
                    val eventSamplingInterval = (node.nodeEventSampleInterval ?: sourceConfiguration.eventSamplingInterval).toDouble()
                    val params = MonitoringParameters(uint(clientHandle), eventSamplingInterval, eventFilter, uint(sourceConfiguration.eventQueueSize), true)

                    val request = MonitoredItemCreateRequest(node.readValueId, MonitoringMode.Reporting, params)
                    yield(request)
                }
            }
        }
    }

    private fun createMonitoredItems(subscription: UaSubscription): MutableList<UaMonitoredItem> {

        val requests =
            dataNodeMonitoredItemRequests +
                    eventNodeMonitoredItemRequests

        monitoredItems?.clear()
        monitoredItems = mutableListOf()

        requests.windowed(size = batchSize, step = batchSize, partialWindows = true).forEach { batchOfRequests ->
            val createdItems = subscription.createMonitoredItems(TimestampsToReturn.Both, batchOfRequests, onMonitoredItemCreated)?.get()
            if (!createdItems.isNullOrEmpty()) {
                monitoredItems!!.addAll(createdItems)
            }
        }

        return monitoredItems as MutableList<UaMonitoredItem>
    }


    // returns all nodes to read in polling mode for a source
    private fun nodesToReadInPollingMode(channels: List<String>?): Sequence<Pair<String, ReadValueId>> = sequence {

        val channelsToRead = sourceConfiguration.channels.filter { (channelID, channel) ->
            channel.isDataNode && (channels == null || channelID in channels)
        }.keys

        channelsToRead.forEach { ch ->
            val readNode = sourceNodes[ch]
            if (readNode != null) {
                // channel, ReadNodeId pairs
                yield(ch to readNode.readValueId)
            }
        }
    }

    // returns the nodes to read in windowed batches
    private fun nodesToReadInPollingModeBatches(channels: List<String>?): Sequence<Map<String, ReadValueId>> = sequence {

        // return batches of nodes
        nodesToReadInPollingMode(channels)
            .windowed(size = batchSize, step = batchSize, partialWindows = true).forEach { n ->
                yield(n.associate { it.first to it.second })
            }
    }

    // called for every created monitored data item, registers the consumer for the changed data
    private val onMonitoredItemCreated: (item: UaMonitoredItem, Int) -> Unit = { uaMonitoredItem: UaMonitoredItem, _: Int ->

        if (!uaMonitoredItem.statusCode.isGood) {
            val errorLog = logger.getCtxErrorLog(className, "onMonitoredItemCreated")
            val channelID = clientHandlesForNodes[uaMonitoredItem.clientHandle.toInt()]?.channelID ?: "unknown channel"
            errorLog(
                "Error creating subscription for source \"${sourceID}\" " +
                        "node \"$channelID\" " + "(${uaMonitoredItem.readValueId.nodeId}), ${uaMonitoredItem.statusCode} "
            )
        } else {

            val nodeData = clientHandlesForNodes[uaMonitoredItem.clientHandle.toInt()]

            if (nodeData != null) {
                if (nodeData.isDataNode)
                    uaMonitoredItem.setValueConsumer(onSubscribedDataReceived())
                else
                    uaMonitoredItem.setEventConsumer(onMonitoredEventReceived())
            }
        }
    }

    private fun onSubscribedDataReceived(): (context: SerializationContext, UaMonitoredItem, DataValue) -> Unit =
        { context: SerializationContext, item: UaMonitoredItem, value: DataValue ->
            if (dataValueChangeChannel!= null) handleChangedData(dataValueChangeChannel, value, context, item)
        }

    private fun handleChangedData(channel : Channel<Pair<UaMonitoredItem, ChannelReadValue>>,  value: DataValue, context: SerializationContext, item: UaMonitoredItem) {
        val log = logger.getCtxLoggers(className, "onSubscribedDataReceived")
        try {
            if ((value.statusCode ?: StatusCode.GOOD).isGood) {
                val nativeValue = OpcuaDataTypesConverter(context).asNativeValue(value.value)
                channel.submit(
                    item to ChannelReadValue(nativeValue, value.sourceTime?.javaInstant),
                    opcuaAdapterConfiguration.changedDataChannelTimeout
                ) { event ->
                    channelSubmitEventHandler(
                        event = event,
                        channelName = "$className:dataValueChangeChannel",
                        tuningChannelSizeName = CONFIG_CHANGED_DATA_CHANNEL_SIZE,
                        currentChannelSize = opcuaAdapterConfiguration.changedDataChannelSize,
                        tuningChannelTimeoutName = CONFIG_CHANGED_DATA_CHANNEL_TIMEOUT,
                        log = log
                    )
                }
            } else {
                val nodeChannelID = clientHandlesForNodes[item.clientHandle.toInt()]?.channelID ?: "unknown channel"
                val errorLog = logger.getCtxErrorLog(className, "onSubscribedDataReceived")
                errorLog("Received bad subscription data source \"$sourceID\", node \"$nodeChannelID\" (${item.readValueId}), ${value.statusCode}")
            }
        } catch (e: Exception) {
            val errorLogEx = logger.getCtxErrorLogEx(className, "onSubscribedDataReceived")
            errorLogEx("Error processing subscription data source \"$sourceID\", node \"${item.readValueId}\" (${item.statusCode})", e)
        }
    }

    private fun onMonitoredEventReceived(): (context: SerializationContext, item: UaMonitoredItem, eventValues: Array<Variant>) -> Unit =
        { context, item, eventPropertyVariantValues ->
            if (receivedEventsChannel!= null) handleEventData(receivedEventsChannel, item, eventPropertyVariantValues, context)
        }

    private fun handleEventData(
        channel: Channel<Pair<UaMonitoredItem, Map<String, Any>>>,
        item: UaMonitoredItem,
        eventPropertyVariantValues: Array<Variant>,
        context: SerializationContext
    ) {
        val log = logger.getCtxLoggers(className, "onMonitoredEventReceived")
        try {
            val node = clientHandlesForNodes[item.clientHandle.toInt()]
            if ((item.statusCode ?: StatusCode.GOOD).isGood) {
                val properties = node?.eventProperties ?: eventsHelper?.findEvent(Identifiers.BaseEventType)?.properties
                if (properties != null) {
                    val propertiesValuesMap = eventsHelper?.variantPropertiesToMap(eventPropertyVariantValues, properties, context)
                    if (propertiesValuesMap != null) channel.submit(
                        item to propertiesValuesMap,
                        configuration.receivedEventsChannelTimeout
                    ) { event ->
                        channelSubmitEventHandler(
                            event = event,
                            channelName = "$className:receivedEventsChannel",
                            tuningChannelSizeName = CONFIG_RECEIVED_EVENTS_CHANNEL_SIZE,
                            currentChannelSize = configuration.receivedEventsChannelSize,
                            tuningChannelTimeoutName = CONFIG_RECEIVED_EVENTS_CHANNEL_TIMEOUT,
                            log = log
                        )
                    }
                } else {
                    logger.getCtxErrorLog(className, "onMonitoredEvent")
                }
            } else {
                val nodeChannelID = node?.channelID ?: "unknown channel"
                val errorLog = logger.getCtxErrorLog(className, "onMonitoredEventReceived")
                errorLog("Error status on monitored event item for source \"$sourceID\", node \"$nodeChannelID\" (${item.readValueId}), ${item.statusCode}")
            }

        } catch (e: Exception) {
            val errorLogEx = logger.getCtxErrorLogEx(className, "onMonitoredEventReceived")
            errorLogEx("Error processing monitored event item for source \"$sourceID\", node \"${item.readValueId}\" (${item.statusCode})", e)
        }
    }

    private var lock = ReentrantLock()

    private fun resetClient(waitFor: Long = 0) {

        if (lock.isHeldByCurrentThread || !lock.tryLock()) return

        try {

            if (isClosing) return

            isClosing = true

            trustManager?.close()
            trustManager = null

            if (!monitoredItems.isNullOrEmpty() && subscription != null) {
                subscription?.deleteMonitoredItems(monitoredItems)
            }

            sourceServerFault = null

            subscription = null

            monitoredItems?.clear()
            monitoredItems = null

            _opcuaClient?.disconnect()
            _opcuaClient = null
            sourceServerFault = null
            pauseWaitUntil = systemDateTime().plusMillis(waitFor)
        } finally {
            isClosing = false
            lock.unlock()
        }
    }

    private suspend fun stopProcessing() {
        changedDataWorker?.cancel()
        eventHandingWorker?.join()
    }

    suspend fun close() {
        connectionWatchdog?.cancel()
        certificateExpiryChecker?.cancel()
        dataValueChangesStore?.clear()
        eventStore?.clear()
        stopProcessing()
        resetClient()

    }

    // get the flow to read the source values, the flow depends on the mode the adapter is using
    private suspend fun readSourceValues(channels: List<String>?): List<Pair<String, ChannelReadValue>> {

        val events = (eventStore?.read(channels) ?: emptyList()).map { it.first to ChannelReadValue(it.second) }

        val data = if (configuration.sources[sourceID]!!.readingMode == OpcuaSourceReadingMode.SUBSCRIPTION)
            serverReadsInSubscriptionMode(channels)
        else
            serverReadsInPollingMode(channels)

        return data + events
    }


    private fun processGoodValue(
        channelID: String,
        value: ChannelReadValue,
        serverTimestamp: Instant?
    ): ChannelReadValue {

        // don't set value timestamp if it is equal to server timestamp to reduce IPC data flow, the receiving side will add the timestamps
        val valueTimestamp = if (value.timestamp != serverTimestamp) value.timestamp else null


        val nativeValue = applyChannelValueSelector(channelID, value.value)

        return ChannelReadValue(nativeValue, valueTimestamp)
    }

    // applies selector for node to select fields from structured types
    private fun applyChannelValueSelector(channelID: String, nativeValue: Any?): Any? {

        val channel = sourceConfiguration.channels[channelID]
        val selector = channel?.selector
        return if (selector != null) {
            val log = logger.getCtxLoggers(className, "applyChannelValueSelector")
            try {
                val selected = selector.search(nativeValue)
                if (selected == null) {
                    log.warning("Applying selector \"${channel.selectorStr}\" for source \"$sourceID\", node \"$channelID\" on value \"$nativeValue\" returns null")
                } else {
                    log.trace("Applying selector \"${channel.selectorStr}\" for source \"$sourceID\", node \"$channelID\" on value \"$nativeValue\" returns \"$selected\"")
                }
                selected
            } catch (e: java.lang.Exception) {
                log.error("Error applying selector \"${channel.selectorStr}\" for source \"$sourceID\", node \"$channelID\", ${e.message}")
            }
        } else nativeValue
    }

    suspend fun read(channels: List<String>?): SourceReadResult {

        if (client == null || isClosing) return SourceReadSuccess(emptyMap(), systemDateTime())

        val log = logger.getCtxLoggers(className, "read")

        log.trace("Reading from source \"$sourceID\"")

        val sourceInSubscriptionMode = configuration.sources[sourceID]?.readingMode == OpcuaSourceReadingMode.SUBSCRIPTION
        val serverTimestamp: Instant? = if (sourceInSubscriptionMode) systemDateTime() else null

        var errorResult: SourceReadResult? = null
        val values = mutableMapOf<String, ChannelReadValue>()

        try {

            val duration = measureTime {

                // collect and process al read source values
                readSourceValues(channels).toList().forEach { (channelID, value) ->
                    values[channelID] = processGoodValue(channelID, value, serverTimestamp)
                }

                // test if a fault was stored by the fault handler used for the connection with the server of the source
                if (sourceServerFault != null) {
                    throw java.lang.Exception(sourceServerFault!!.responseHeader.serviceResult.toString())
                }
            }

            createMetrics(protocolAdapterID, duration.inWholeMilliseconds.toDouble(), values)

        } catch (e: Throwable) {

            if (!isClosing) resetClient(opcuaServerConfiguration.waitAfterReadError.inWholeMilliseconds)

            metricsCollector?.put(protocolAdapterID, MetricsCollector.METRICS_READ_ERRORS, 1.0, MetricUnits.COUNT, dimensions)
            errorResult = if (e is TimeoutException) {
                SourceReadError("Timeout reading  source $sourceID from server ${opcuaServerConfiguration.endPoint} within ${opcuaServerConfiguration.readTimeout}")
            } else {
                if (e.isJobCancellationException)
                    SourceReadSuccess(emptyMap(), systemDateTime())
                else
                    SourceReadError("Error reading source $sourceID from server (${opcuaServerConfiguration.endPoint}), ${e.message}")
            }
        }

        return errorResult ?: SourceReadSuccess(values, serverTimestamp)
    }


    private suspend fun createMetrics(
        protocolAdapterID: String,
        readDurationInMillis: Double,
        values: MutableMap<String, ChannelReadValue>
    ) {
        metricsCollector?.put(
            protocolAdapterID,
            metricsCollector.buildValueDataPoint(protocolAdapterID, MetricsCollector.METRICS_READS, 1.0, MetricUnits.COUNT, dimensions),
            metricsCollector.buildValueDataPoint(
                protocolAdapterID,
                MetricsCollector.METRICS_READ_DURATION,
                readDurationInMillis,
                MetricUnits.MILLISECONDS,
                dimensions
            ),
            metricsCollector.buildValueDataPoint(
                protocolAdapterID,
                MetricsCollector.METRICS_VALUES_READ,
                values.size.toDouble(),
                MetricUnits.COUNT,
                dimensions
            ),
            metricsCollector.buildValueDataPoint(protocolAdapterID, MetricsCollector.METRICS_READ_SUCCESS, 1.0, MetricUnits.COUNT, dimensions)
        )
    }

    private fun serverReadsInSubscriptionMode(channels: List<String>?): List<Pair<String, ChannelReadValue>> {
        var values: List<Pair<String, ChannelReadValue>>
        val duration = measureTime {
            values = dataValueChangesStore?.read(channels) ?: emptyList()
        }
        if (values.isNotEmpty()) logger.getCtxTraceLog(className, "read")("Reading ${values.size} from store took $duration")
        return values

    }

    private suspend fun serverReadsInPollingMode(channels: List<String>?): List<Pair<String, ChannelReadValue>> =


        // partition nodes in smaller batches
        sequence {

            if (client == null) return@sequence

            val opcuaDataTypesConverter = OpcuaDataTypesConverter(client?.serializationContext)

            nodesToReadInPollingModeBatches(channels).forEach { batchOfNodes ->

                // read from the server and wait for result
                val deferredResult = client!!.read(0.0, TimestampsToReturn.Both, batchOfNodes.values.toMutableList())

                try {
                    val response: ReadResponse = deferredResult.join()

                    if (!response.responseHeader.serviceResult.isGood) {
                        // to do reset connection
                        return@sequence
                    }

                    // map the read values to the nodes
                    batchOfNodes.keys.mapIndexed { i, s ->
                        val value = response.results[i]
                        if (value.statusCode?.isGood == true) {
                            val nativeValue = opcuaDataTypesConverter.asNativeValue(response.results[i].value)
                            if (nativeValue != null) {
                                yield(s to ChannelReadValue(nativeValue, value.sourceTime?.javaInstant))
                            }
                        } else {
                            logger.getCtxErrorLog(
                                className,
                                "read"
                            )("Error reading value for channel \"$s\" from source \"$sourceID\", ${value.statusCode}")
                        }
                    }

                } catch (e: Exception) {
                    throw ProtocolAdapterException("Error reading from source \"$sourceID\", $e")
                }
            }
        }.toList()

    companion object {
        val DEFAULT_INTERVAL = 1.toDuration(DurationUnit.SECONDS)
    }

}

