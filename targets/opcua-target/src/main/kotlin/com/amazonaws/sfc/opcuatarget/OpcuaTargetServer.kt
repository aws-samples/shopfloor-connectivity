// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.opcuatarget


import com.amazonaws.sfc.config.ElementNamesConfiguration
import com.amazonaws.sfc.config.SelfSignedCertificateConfig
import com.amazonaws.sfc.config.SelfSignedCertificateConfig.Companion.CONFIG_CERT_DEFAULT_VALIDITY_PERIOD_DAYS
import com.amazonaws.sfc.crypto.CertificateConfiguration
import com.amazonaws.sfc.crypto.CertificateHelper
import com.amazonaws.sfc.data.ChannelOutputData
import com.amazonaws.sfc.data.JsonHelper
import com.amazonaws.sfc.data.SourceOutputData
import com.amazonaws.sfc.data.TargetData
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.opcuatarget.OpcuaServerDataTypes.Companion.toVariant
import com.amazonaws.sfc.opcuatarget.config.OpcuaServerMessageSecurityMode
import com.amazonaws.sfc.opcuatarget.config.OpcuaServerSecurityPolicy
import com.amazonaws.sfc.opcuatarget.config.OpcuaTargetConfiguration
import com.amazonaws.sfc.system.DateTime.add
import com.amazonaws.sfc.system.DateTime.systemDateUTC
import com.amazonaws.sfc.transformations.Transformation
import com.amazonaws.sfc.transformations.invoke
import com.amazonaws.sfc.util.*
import io.burt.jmespath.Expression
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.eclipse.milo.opcua.sdk.server.OpcUaServer
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig
import org.eclipse.milo.opcua.sdk.server.identity.IdentityValidator
import org.eclipse.milo.opcua.sdk.server.identity.X509IdentityValidator
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.BaseModelChangeEventTypeNode
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode
import org.eclipse.milo.opcua.sdk.server.nodes.filters.AttributeFilter
import org.eclipse.milo.opcua.sdk.server.util.HostnameUtil
import org.eclipse.milo.opcua.stack.core.Identifiers
import org.eclipse.milo.opcua.stack.core.StatusCodes
import org.eclipse.milo.opcua.stack.core.UaRuntimeException
import org.eclipse.milo.opcua.stack.core.security.DefaultCertificateManager
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy
import org.eclipse.milo.opcua.stack.core.transport.TransportProfile
import org.eclipse.milo.opcua.stack.core.types.builtin.*
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo
import org.eclipse.milo.opcua.stack.server.EndpointConfiguration
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.Period
import java.time.temporal.ChronoUnit
import java.util.*


class OpcuaTargetServer(private val targetConfiguration: OpcuaTargetConfiguration,
                        private val transformations: Map<String,Transformation>,
                        private val attributeFilter: AttributeFilter?,
                        private val elementNames: ElementNamesConfiguration,
                        private val logger: Logger) {

    private val className = this::class.java.simpleName

    private val serverScope = buildScope(className)
    private var server: OpcUaServer? = null

    val opcuaServer
        get() = server

    private val namespaces = mutableListOf<OpcuaNamespaceBuilder>()

    private var dataModelHelper: ServerDataModelHelper? = null

    private val modelUpdateChannel = Channel<Unit>()
    private val modelChangedEventTask = serverScope.launch(context = Dispatchers.IO, name = "ModelChanged") { modelChangedEvent() }

    private fun fnModelChanged(node: UaNode) {
        logger.getCtxTraceLog("Node ${node.nodeId.toParseableString()} updated")
        modelUpdateChannel.trySend(Unit)
    }

    private var certificateExpiryChecker: Job? = null

    private val defaultSelfSignedCertificateConfiguration by lazy {
        val (addresses, hostNames) = getAddressesAndHostNames()
        val defaultSelfSignedCertificateConfig = SelfSignedCertificateConfig.create(
            commonName = "$COMMON_NAME-${getHostName().uppercase()}",
            dnsNames = hostNames + getHostName(),
            ipAddress = addresses,
            applicationUri = PRODUCT_URI,
            organization = "AWS",
            validityPeriodDays = CONFIG_CERT_DEFAULT_VALIDITY_PERIOD_DAYS
        )
        defaultSelfSignedCertificateConfig
    }

    private val defaultCertificateConfiguration: CertificateConfiguration by lazy {
        val defaultCertificateConfig = CertificateConfiguration.create(
            certificate = "$COMMON_NAME-${getHostName()}.cert",
            key = "$COMMON_NAME-${getHostName()}.key",
            selfSignedCertificateConfig = defaultSelfSignedCertificateConfiguration
        )
        defaultCertificateConfig
    }

    fun initialize(): OpcuaTargetServer {

        val log = logger.getCtxLoggers(className, "initialize")

        val certificateConfiguration = targetConfiguration.certificateConfiguration ?: defaultCertificateConfiguration

        if (certificateConfiguration.selfSignedCertificateConfig == null) {
            certificateConfiguration.selfSignedCertificateConfig = defaultSelfSignedCertificateConfiguration
        }

        if (certificateConfiguration.selfSignedCertificateConfig?.applicationUri.isNullOrEmpty()) certificateConfiguration.selfSignedCertificateConfig?.applicationUri = PRODUCT_URI

        val certificateHelper = CertificateHelper(certificateConfiguration, logger)

        val serverTrustListManager = ServerTrustListManager(targetConfiguration.certificateValidationConfiguration.directory, logger) { dir ->
            log.info("Certificate or CLR update in directory \"$dir\"")
        }

        if (serverTrustListManager.trustedCertificates.isEmpty() && serverTrustListManager.issuerCertificates.isEmpty() ) {
            log.info("There are no trusted or issuer certificates in directories ${serverTrustListManager.trustedCertificatesDirectory} or ${serverTrustListManager.issuerCertificatePath}")
        }

        val validations = if (!targetConfiguration.certificateValidationConfiguration.active)
            emptySet()
        else
            targetConfiguration.certificateValidationConfiguration.validationOptions.options
        val certificateValidator = ServerCertificateValidator(serverTrustListManager, validations, logger)

        val usernameIdentifyValidator = UserNameValidator(targetConfiguration.serverSecurityPolicies.contains(OpcuaServerSecurityPolicy.None) || targetConfiguration.anonymousDiscoveryEndPoint)


        val secureMode = targetConfiguration.serverSecurityPolicies.any { it != OpcuaServerSecurityPolicy.None }
        val x509IdentityValidator = if (secureMode) X509IdentityValidator { _ -> true } else null


        val (certificate, httpKeypair) = if (secureMode) certificateHelper.getCertificateAndKeyPair() else null to null
        if (secureMode) {
            if (certificate == null) {
                throw UaRuntimeException(StatusCodes.Bad_ConfigurationError, "No certificate required for security policies ${targetConfiguration.serverSecurityPolicies.joinToString { it.name }}")
            }
            if (httpKeypair == null) {
                throw UaRuntimeException(StatusCodes.Bad_ConfigurationError, "No keypair required for security policies ${targetConfiguration.serverSecurityPolicies.joinToString { it.name }}")
            }
        }

        certificateExpiryChecker = startCertificateExpiryChecker(certificate)

        val certificateManager = if (secureMode) DefaultCertificateManager(httpKeypair, certificate) else null
        val endpointConfigurations: Set<EndpointConfiguration> = createEndpointConfigurations(certificate)



        val serverConfigBuilder = OpcUaServerConfig.builder()
            .setApplicationUri(PRODUCT_URI)
            .setApplicationName(LocalizedText.english(PRODUCT_NAME))
            .setEndpoints(endpointConfigurations)
            .setBuildInfo(
                BuildInfo(
                    PRODUCT_URI,
                    MANUFACTURER_NAME,
                    PRODUCT_NAME,
                    BuildConfig.VERSION,
                    "", DateTime.now()))
            .setProductUri(PRODUCT_URI)


        if (secureMode) {
            serverConfigBuilder.setCertificateManager(certificateManager)
                .setTrustListManager(serverTrustListManager)
                .setHttpsKeyPair(httpKeypair)
                .setHttpsCertificateChain(arrayOf(certificate))
            
                serverConfigBuilder.setCertificateValidator(certificateValidator)

        }

        val identityValidator =
            if (secureMode && x509IdentityValidator != null) CompositedValidator<IdentityValidator<*>>(usernameIdentifyValidator, x509IdentityValidator)
            else
                usernameIdentifyValidator
        serverConfigBuilder.setIdentityValidator(identityValidator)


        val serverConfig = serverConfigBuilder.build()

        server = OpcUaServer(serverConfig)

        dataModelHelper = ServerDataModelHelper(server!!, targetConfiguration, elementNames, transformations, attributeFilter, logger)
        dataModelHelper!!.createServerDataModels()

        return this
    }


    private suspend fun CoroutineScope.modelChangedEvent() {
        while (isActive) {
            modelUpdateChannel.receive()
            raiseDataModelChangedEvent()
        }
    }

    private fun writeChannelData(scheduleName: String,
                                 sourceName: String,
                                 channelName: String,
                                 channelData: ChannelOutputData,
                                 sourceFolder: UaFolderNode,
                                 isAggregated: Boolean,
                                 timeStamp: Instant) {


        if (dataModelHelper == null) return

        if (isAggregated) {
            writeAggregatedChannelData(scheduleName, sourceName, channelName, channelData, timeStamp)
        } else {
            writeChannelValue(channelData, scheduleName, sourceName, channelName, sourceFolder, timeStamp)
        }

        if (!channelData.metadata.isNullOrEmpty()) {
            val folderForMetadata = if (!channelData.metadata.isNullOrEmpty()) dataModelHelper!!.getChannelFolder(scheduleName, sourceName, channelName, ::fnModelChanged) else sourceFolder
            if (folderForMetadata != null) {

                channelData.metadata?.forEach { (metadataName, metadataValue) ->

                    val metadataVariable = dataModelHelper?.getChannelMetadataVariable(folderForMetadata, scheduleName, sourceName, channelName, metadataName, metadataValue, ::fnModelChanged)
                    metadataVariable?.value = DataValue(Variant(metadataValue), StatusCode.GOOD, DateTime(timeStamp), DateTime(Instant.now()))

                }
            }
        }

    }

    private fun writeChannelValue(channelData: ChannelOutputData,
                                  scheduleName: String,
                                  sourceName: String,
                                  channelName: String,
                                  sourceFolder: UaFolderNode,
                                  timeStamp: Instant) {
        if (channelData.value != null) {

            val canStoreAsVariable = channelData.metadata.isNullOrEmpty()


            val valueVariable = if (canStoreAsVariable) {
                // all metadata already mapped, so store directly in source folder
                dataModelHelper?.getChannelVariable(sourceFolder, scheduleName, sourceName, channelName, "", channelData.value!!, ::fnModelChanged)
            } else {
                // we have metadata so store data in folder containing the value and metadata
                val channelFolder = dataModelHelper!!.getChannelFolder(scheduleName, sourceName, channelName, ::fnModelChanged)
                dataModelHelper?.getChannelVariable(channelFolder, scheduleName, sourceName, channelName, "Value", channelData.value!!, ::fnModelChanged)
            }

            val value = buildValue(channelData.value!!, valueVariable, timeStamp)
            valueVariable?.value = value


        }
    }

    private fun writeAggregatedChannelData(scheduleName: String,
                                           sourceName: String,
                                           channelName: String,
                                           channelData: ChannelOutputData,
                                           timeStamp: Instant) {


        (channelData.value as Map<*, *>?)?.forEach { (aggregationName, aggregation) ->
            if (aggregation != null) {
                val aggregationValue = (aggregation as ChannelOutputData).value
                if (aggregationValue != null) {

                    val channelFolder = dataModelHelper!!.getChannelFolder(scheduleName, sourceName, channelName, ::fnModelChanged)
                    val aggregatedValueVariable =
                        dataModelHelper!!.getChannelAggregatedValueVariable(channelFolder, scheduleName, sourceName, channelName, aggregationName as String, aggregationValue, ::fnModelChanged)

                    val value = buildValue(aggregationValue, aggregatedValueVariable, timeStamp)
                    aggregatedValueVariable?.value = value

                }
            }
        }
    }

    private fun hostNamesToBind(): Set<String> {
        val hostnames: MutableSet<String> = mutableSetOf()

        val hostname = HostnameUtil.getHostname()
        val networkInterfaces = NetworkInterface.getNetworkInterfaces().toList()
            .filter { targetConfiguration.serverNetworkInterfaces.isEmpty() || targetConfiguration.serverNetworkInterfaces.contains(it.name.lowercase()) }
        val addresses: List<Inet4Address> = networkInterfaces.flatMap { it.inetAddresses.toList() }.filterIsInstance<Inet4Address>().map { it }
        if (HostnameUtil.getHostnames(hostname).any { i -> i in addresses.map { it.hostAddress } }) {
            hostnames.add(hostname)
        }
        HostnameUtil.getHostnames("localhost").forEach {
            hostnames.add(it)
        }

        networkInterfaces.forEach { networkInterface ->
            networkInterface.inetAddresses.toList().forEach { address ->
                if (address is Inet4Address) {
                    hostnames.add(address.hostAddress)
                    hostnames.add(address.hostName)
                    hostnames.add(address.canonicalHostName)
                }
            }
        }

        return hostnames
    }

    private fun createEndpointConfigurations(certificate: X509Certificate?): Set<EndpointConfiguration> {
        val endpointConfigurations: MutableSet<EndpointConfiguration> = LinkedHashSet()

        val hostNames = hostNamesToBind()

        hostNames.forEach { hostName ->
            endpointConfigurations += buildHostNameEndpoints(hostName, certificate)
        }

        return endpointConfigurations
    }

    private fun buildHostNameEndpoints(hostName: String,
                                       certificate: X509Certificate?): Set<EndpointConfiguration> {

        val bindAddress = "0.0.0.0"
        val hostEndpointConfigurations = mutableSetOf<EndpointConfiguration>()

        val builder = EndpointConfiguration.newBuilder()
            .setBindAddress(bindAddress)
            .setHostname(hostName)
            .setPath("/${targetConfiguration.serverPath}")
            .addTokenPolicies(
                OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS,
                OpcUaServerConfig.USER_TOKEN_POLICY_X509)

        if (certificate != null) {
            builder.setCertificate(certificate)
        }

        hostEndpointConfigurations.addAll(buildEndpoints(builder))

        return hostEndpointConfigurations
    }


    private fun buildEndpoints(builder: EndpointConfiguration.Builder): Set<EndpointConfiguration> {
        val endpoints = mutableSetOf<EndpointConfiguration>()

        targetConfiguration.serverSecurityPolicies.filter { it != OpcuaServerSecurityPolicy.None }.forEach { policy ->
            targetConfiguration.serverMessageSecurityModes.filter { it != OpcuaServerMessageSecurityMode.NONE }.forEach { mode ->
                endpoints.add(
                    buildTcpEndpoint(
                        builder.copy()
                            .setSecurityPolicy(policy.policy)
                            .setSecurityMode(mode.mode))
                )

            }
        }

        if (targetConfiguration.serverSecurityPolicies.contains(OpcuaServerSecurityPolicy.None)) {
            val noSecurityBuilder = builder.copy()
                .setSecurityPolicy(SecurityPolicy.None)
                .setSecurityMode(MessageSecurityMode.None)

            endpoints.add(buildTcpEndpoint(noSecurityBuilder))
        }

        if (targetConfiguration.anonymousDiscoveryEndPoint) {
            val discoveryBuilder = builder.copy()
                .setPath("/${targetConfiguration.serverPath}/discovery")
                .setSecurityPolicy(SecurityPolicy.None)
                .setSecurityMode(MessageSecurityMode.None)

            endpoints.add(buildTcpEndpoint(discoveryBuilder))
        }

        return endpoints
    }


    fun startup(): OpcUaServer {
        val log = logger.getCtxLoggers(className, "startup")
        log.info("Starting OPCUA server")
        val server = server!!.startup().get()
        log.info("OPCUA server \"${server.config.buildInfo.productName}\", version ${server.config.buildInfo.softwareVersion} started")
        return server
    }

    fun shutdown() {

        try {
            modelChangedEventTask.cancel()
            namespaces.forEach { it.shutdown() }

            serverScope.cancel()

            server?.shutdown()

        } catch (_: Exception) {
        }
    }

    private fun startCertificateExpiryChecker(certificate: X509Certificate?): Job? {

        val certificateConfiguration = targetConfiguration.certificateConfiguration
        val expirationWarningPeriod = certificateConfiguration?.expirationWarningPeriod ?: 0

        if (certificate == null || certificateConfiguration == null || expirationWarningPeriod <= 0) {
            certificateExpiryChecker?.cancel()
            return null
        }

        return serverScope.launch("OPCUA Certificate Expiry Watcher", Dispatchers.IO) {

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
                    com.amazonaws.sfc.system.DateTime.delayUntilNextMidnightUTC()
                }
            } catch (e: Exception) {
                if (!e.isJobCancellationException)
                    logger.getCtxErrorLogEx(className, "startCertificateExpiryChecker")("Error while checking certificate expiration", e)
            }
        }
    }

    fun raiseDataModelChangedEvent() {

        val log = logger.getCtxLoggers(className, "raiseDataModelChangedEvent")

        if (server != null) {
            val serverNode = server!!.addressSpaceManager.getManagedNode(Identifiers.Server).get()
            val event = server!!.eventFactory.createEvent(NodeId(0, UUID.randomUUID()), Identifiers.BaseModelChangeEventType) as BaseModelChangeEventTypeNode
            event.message = LocalizedText("Model changed")
            event.browseName = QualifiedName(0, "ModelChanged")
            event.displayName = LocalizedText("Model changed")
            event.eventId = ByteString.of(DateTime.now().toString().encodeToByteArray())
            event.eventType = Identifiers.GeneralModelChangeEventType
            event.sourceNode = serverNode.nodeId
            event.sourceName = serverNode.displayName.text
            event.time = DateTime(System.currentTimeMillis())
            event.receiveTime = DateTime.NULL_VALUE
            event.severity = UShort.valueOf(1)
            server!!.eventBus.post(event)
            log.info("Raised data model changed event, ${event.eventType.toParseableString()} from source ${serverNode.nodeId.toParseableString()}")
            event.delete()

        }
    }

    fun writeTargetData(targetData: TargetData) {

        if (targetConfiguration.autoCreate) {
            val scheduleFolderNew = dataModelHelper?.getScheduleFolder(targetData.schedule, ::fnModelChanged)

            val scheduleFolder = scheduleFolderNew ?: return//server.modelHelper?.getScheduleFolderOld(targetData.schedule)
            targetData.metadata.forEach { (metadataName, metadataValue) ->
                val metaDataVariable = dataModelHelper?.getScheduleMetadataVariable(scheduleFolder, targetData.schedule, metadataName, metadataValue, ::fnModelChanged)
                metaDataVariable?.value = DataValue(Variant(metadataValue), StatusCode.GOOD, DateTime(targetData.timestamp), DateTime(Instant.now()))
            }
            targetData.sources.forEach { (sourceName, sourceData) ->
                writeSourceData(targetData.schedule, sourceName, sourceData, targetData)
            }
        }

        handleVariableNodeSelectors(targetData)
    }

    private fun handleVariableNodeSelectors(targetData: TargetData) {

        if (dataModelHelper?.valueQueries?.isNotEmpty() == true) {

            val log = logger.getCtxLoggers(className, "handleVariableNodeSelectors")

            val targetDataMap = targetData.toMap(elementNames, true)

            dataModelHelper?.valueQueries?.forEach { (nodeId, queryData) ->
                if (queryData.query != null) {
                    var selectedValue = searchData(queryData.queryStr, queryData.query, targetDataMap)

                    if (selectedValue != null) {
                        log.trace("Using value $selectedValue (${selectedValue::class.java.simpleName}) from selector \"${queryData.queryStr}\" to variable \"${queryData.uaVariableNode.displayName.text}\" ( ${nodeId.toParseableString()})")
                        val timestamp = getTimestamp(queryData.uaVariableNode, targetDataMap, targetData)

                        if (!queryData.transformationID.isNullOrEmpty()) {
                            selectedValue = applyTransformation(selectedValue, queryData.id, queryData.transformationID,)
                        }
                        if (selectedValue != null) {
                            queryData.uaVariableNode.value = buildValue(selectedValue, queryData.uaVariableNode, timestamp)
                        }
                    }
                }
            }
        }
    }

    private fun getTimestamp(uaVariableNode: UaVariableNode, targetDataMap: Map<String, Any>, targetData: TargetData): Instant {

        val log = logger.getCtxLoggers(className, "getTimestamp")

        val timestampSelector = dataModelHelper?.timestampQueries?.get(uaVariableNode.nodeId)

        val timestampByQuery = (if (timestampSelector?.first != null && timestampSelector.second != null)
            searchData(timestampSelector.first!!, timestampSelector.second, targetDataMap) else null) as Instant?
        if (timestampByQuery == null) {
            log.trace("Query \"${timestampSelector?.first}\" dis not return a timestamp")
        }

        return if (timestampByQuery != null) {
            log.trace("Used query \"${timestampSelector?.first}\" to select timestamp $timestampByQuery for variable \"${uaVariableNode.displayName.text}\" (${uaVariableNode.nodeId.toParseableString()})")
            timestampByQuery
        } else {
            log.trace("Used target data timestamp ${targetData.timestamp} for variable \"${uaVariableNode.displayName.text}\" (${uaVariableNode.nodeId.toParseableString()})")
            targetData.timestamp
        }
    }


    private fun searchData(queryString: String, query: Expression<Any>?, data: Map<String, Any>): Any? = try {
        query?.search(data)
    } catch (_: NullPointerException) {
        null
    } catch (e: Exception) {
        val log = logger.getCtxErrorLogEx(className, "searchData")
        log("Error querying data with expression \"$queryString\"", e)
        null
    }


    private fun applyTransformation(value: Any, name: String, transformationID: String): Any? {
        val log = logger.getCtxLoggers(className, "applyTransformation")

        val transformation = transformations[transformationID] ?: return null
        return try {
            log.trace("Applying transformation \"$transformationID\" on value ${value}:${value::class.java.simpleName} to \"$name\"")
            val transformedValue = transformation.invoke(value, name, true, logger)
            log.trace("Result of transformation \"$transformationID\" is ${transformedValue}${if (transformedValue != null) ":${transformedValue::class.java.simpleName}" else ""}")
            transformedValue
        } catch (e: Exception) {
            logger.getCtxErrorLog(className, "applyTransformation")("Error applying transformation $transformationID to name \"$name\", $e")
            null
        }
    }


    private fun writeSourceData(scheduleName: String,
                                sourceName: String,
                                sourceData: SourceOutputData,
                                targetData: TargetData) {

        val sourceFolder = dataModelHelper?.getSourceFolder(scheduleName, sourceName, ::fnModelChanged) ?: return

        val sourceTimeStamp = sourceData.timestamp ?: targetData.timestamp
        sourceData.metadata?.forEach { (metadataName, metadataValue) ->

            val metadataVariable = dataModelHelper?.getSourceMetadataVariable(sourceFolder, scheduleName, sourceName, metadataName, metadataValue, ::fnModelChanged)
            if (metadataVariable != null) {
                metadataVariable.value = DataValue(Variant(metadataValue), StatusCode.GOOD, DateTime(sourceTimeStamp), DateTime(Instant.now()))
            }
        }


        sourceData.channels.forEach { (channelName, channelData) ->
            val timestamp = channelData.timestamp ?: sourceTimeStamp
            writeChannelData(scheduleName, sourceName, channelName, channelData, sourceFolder, sourceData.isAggregated, timestamp)
        }

    }

    @Suppress("UNCHECKED_CAST")
    private fun buildValue(value: Any, valueVariable: UaVariableNode?, sourceTimeStamp: Instant): DataValue {
        val variant = when {
            value is Map<*, *> -> Variant(JsonHelper.gsonExtended().toJson(value))
            value is List<*> && value.isNotEmpty() && value.first() is ChannelOutputData -> {
                val l = (value as List<ChannelOutputData>).map { it.value }
                l.toVariant(dimensions = valueVariable?.arrayDimensions?.map { it.toInt() }, dataTypeIdentifier = valueVariable?.dataType, logger = logger)
            }

            else -> value.toVariant(dimensions = valueVariable?.arrayDimensions?.map { it.toInt() }, dataTypeIdentifier = valueVariable?.dataType, logger = logger)
        }
        return DataValue(variant, StatusCode.GOOD, DateTime(sourceTimeStamp), DateTime.now())
    }


    private fun buildTcpEndpoint(base: EndpointConfiguration.Builder): EndpointConfiguration {
        return base.copy()
            .setTransportProfile(TransportProfile.TCP_UASC_UABINARY)
            .setBindPort(targetConfiguration.serverTcpPort)
            .build()
    }

    companion object {

        const val PRODUCT_URI = "urn:amazonaws:sfc:opcua-target"
        private const val MANUFACTURER_NAME = "AWS"
        private const val PRODUCT_NAME = "SFC OPCUA Target"
        private const val COMMON_NAME = "SFC-OPCUA-TARGET"

    }

}