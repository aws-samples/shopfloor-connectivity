// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.opcuatarget


import com.amazonaws.sfc.config.SelfSignedCertificateConfig
import com.amazonaws.sfc.config.SelfSignedCertificateConfig.Companion.CONFIG_CERT_DEFAULT_VALIDITY_PERIOD_DAYS
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
import com.amazonaws.sfc.util.*
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
import org.eclipse.milo.opcua.stack.core.util.validation.ValidationCheck
import org.eclipse.milo.opcua.stack.server.EndpointConfiguration
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.Period
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.CompletableFuture

class OpcuaTargetServer(val config: OpcuaTargetConfiguration, private val attributeFilter: AttributeFilter?, private val logger: Logger) {

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

    fun initialize(): OpcuaTargetServer {

        val log = logger.getCtxLoggers(className, "initialize")

        val certificateConfiguration = config.certificateConfiguration

        if (certificateConfiguration != null && certificateConfiguration.selfSignedCertificateConfig == null){
            certificateConfiguration.selfSignedCertificateConfig = defaultSelfSignedCertificateConfiguration
        }

        if (certificateConfiguration?.selfSignedCertificateConfig?.applicationUri.isNullOrEmpty()) certificateConfiguration?.selfSignedCertificateConfig?.applicationUri = PRODUCT_URI

        val certificateHelper = CertificateHelper(certificateConfiguration!!, logger)

        val serverTrustListManager = ServerTrustListManager(config.certificateValidationConfiguration.directory, logger) { dir ->
            log.info("Certificate or CLR update in directory \"$dir\"")
        }

        val certificateValidator = if (serverTrustListManager.trustedCertificates.isEmpty() && serverTrustListManager.issuerCertificates.isEmpty()) {
            log.warning("There are no trusted or issuer certificates in directories ${serverTrustListManager.trustedCertificatesDirectory} or ${serverTrustListManager.issuerCertificatePath}")
            null
        } else {
            val validations = if (!config.certificateValidationConfiguration.active)
                emptySet<ValidationCheck>()
            else
                config.certificateValidationConfiguration.validationOptions.options
            ServerCertificateValidator(serverTrustListManager, validations, logger)
        }

        val usernameIdentifyValidator = UserNameValidator(config.serverSecurityPolicies.contains(OpcuaServerSecurityPolicy.None) || config.anonymousDiscoveryEndPoint)


        val secureMode = config.serverSecurityPolicies.any { it != OpcuaServerSecurityPolicy.None }
        val x509IdentityValidator = if (secureMode) X509IdentityValidator { _ -> true } else null


        val (certificate, httpKeypair) = if (secureMode) certificateHelper.getCertificateAndKeyPair() else null to null
        if (secureMode) {
            if (certificate == null) {
                throw UaRuntimeException(StatusCodes.Bad_ConfigurationError, "No certificate required for security policies ${config.serverSecurityPolicies.joinToString { it.name }}")
            }
            if (httpKeypair == null) {
                throw UaRuntimeException(StatusCodes.Bad_ConfigurationError, "No keypair required for security policies ${config.serverSecurityPolicies.joinToString { it.name }}")
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

            if (certificateValidator != null) {
                serverConfigBuilder.setCertificateValidator(certificateValidator)
            }
        }

        val identityValidator =
            if (secureMode && x509IdentityValidator != null) CompositedValidator<IdentityValidator<*>>(usernameIdentifyValidator, x509IdentityValidator)
            else
                usernameIdentifyValidator
        serverConfigBuilder.setIdentityValidator(identityValidator)


        val serverConfig = serverConfigBuilder.build()

        server = OpcUaServer(serverConfig)

        dataModelHelper = ServerDataModelHelper(server!!, config, attributeFilter, logger)
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
                                 sourceFolder: UaFolderNode?,
                                 isAggregated: Boolean,
                                 timeStamp: Instant) {


        val channelFolder = if (isAggregated || !channelData.metadata.isNullOrEmpty()) dataModelHelper?.getChannelFolder(scheduleName, sourceName, channelName, ::fnModelChanged) else null

        channelData.metadata?.forEach { (metadataName, metadataValue) ->
            val metadataVariable = dataModelHelper?.getChannelVariable(channelFolder, scheduleName, sourceName, channelName, metadataName, metadataValue, ::fnModelChanged)
            metadataVariable?.value = DataValue(Variant(metadataValue), StatusCode.GOOD, DateTime(timeStamp), DateTime(Instant.now()))

        }

        if (isAggregated) {
            (channelData.value as Map<*, *>?)?.forEach { (aggregationName, aggregation) ->
                if (aggregation != null) {
                    val aggregationValue = (aggregation as ChannelOutputData).value
                    if (aggregationValue != null) {
                        val aggregatedValueVariable =
                            dataModelHelper?.getChannelVariable(channelFolder!!, scheduleName, sourceName, channelName, aggregationName as String, aggregationValue, ::fnModelChanged)
                        val value = buildValue(aggregationValue, aggregatedValueVariable, timeStamp)
                        aggregatedValueVariable?.value = value
                    }
                }
            }
        } else {
            if (channelData.value != null) {
                val valueVariable = if (channelFolder != null)
                    dataModelHelper?.getChannelVariable(channelFolder, scheduleName, sourceName, channelName, "Value", channelData.value!!, ::fnModelChanged)
                else
                    dataModelHelper?.getChannelVariable(sourceFolder, scheduleName, sourceName, channelName, "", channelData.value!!, ::fnModelChanged)
                val value = buildValue(channelData.value!!, valueVariable, timeStamp)
                valueVariable?.value = value
            }


        }
    }

    private fun hostNamesToBind(): Set<String> {
        val hostnames: MutableSet<String> = mutableSetOf()

        val hostname = HostnameUtil.getHostname()
        val networkInterfaces = NetworkInterface.getNetworkInterfaces().toList().filter { config.serverNetworkInterfaces.isEmpty() || config.serverNetworkInterfaces.contains(it.name.lowercase()) }
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
            .setPath("/${config.serverPath}")
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

        config.serverSecurityPolicies.filter { it != OpcuaServerSecurityPolicy.None }.forEach { policy ->
            config.serverMessageSecurityModes.filter { it != OpcuaServerMessageSecurityMode.NONE }.forEach { mode ->
                endpoints.add(
                    buildTcpEndpoint(
                        builder.copy()
                            .setSecurityPolicy(policy.policy)
                            .setSecurityMode(mode.mode))
                )

            }
        }

        if (config.serverSecurityPolicies.contains(OpcuaServerSecurityPolicy.None)) {
            val noSecurityBuilder = builder.copy()
                .setSecurityPolicy(SecurityPolicy.None)
                .setSecurityMode(MessageSecurityMode.None)

            endpoints.add(buildTcpEndpoint(noSecurityBuilder))
        }

        if (config.anonymousDiscoveryEndPoint) {
            val discoveryBuilder = builder.copy()
                .setPath("/${config.serverPath}/discovery")
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

        try{
        modelChangedEventTask.cancel()
        namespaces.forEach { it.shutdown() }

        serverScope.cancel()

        if (server == null) {
            CompletableFuture.completedFuture(server)
        } else {
            server?.shutdown()
        }}catch ( _ : Exception){}
    }

    private fun startCertificateExpiryChecker(certificate: X509Certificate?): Job? {

        val certificateConfiguration = config.certificateConfiguration
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

    private fun writeSourceData(scheduleName: String,
                                sourceName: String,
                                sourceData: SourceOutputData,
                                targetData: TargetData) {
        val sourceFolder = dataModelHelper?.getSourceFolder(scheduleName, sourceName, ::fnModelChanged) ?: return

        sourceData.metadata?.forEach { (metadataName, metaDatavalue) ->
            val metadataVariable = dataModelHelper?.getSourceMetadataVariable(sourceFolder, scheduleName, sourceName, metadataName, metaDatavalue, ::fnModelChanged)
            if (metadataVariable != null) {
                metadataVariable.value = DataValue(Variant(metaDatavalue), StatusCode.GOOD, DateTime(sourceData.timestamp), DateTime(Instant.now()))
            }
        }

        val sourceTimeStamp = sourceData.timestamp ?: targetData.timestamp

        sourceData.channels.forEach { (channelName, channelData) ->
            val timestamp = channelData.timestamp ?: sourceTimeStamp
            writeChannelData(scheduleName, sourceName, channelName, channelData, sourceFolder, sourceData.isAggregated, timestamp)
        }

    }

    private fun buildValue(value: Any, valueVariable: UaVariableNode?, sourceTimeStamp: Instant): DataValue {
        val variant =
            if (value is Map<*, *>) {
                Variant(JsonHelper.gsonExtended().toJson(value))
            } else {
                value.toVariant(dimensions = valueVariable?.arrayDimensions?.map { it.toInt() }, dataTypeIdentifier = valueVariable?.dataType)
            }
        return DataValue(variant, StatusCode.GOOD, DateTime(sourceTimeStamp), DateTime.now())
    }


    private fun buildTcpEndpoint(base: EndpointConfiguration.Builder): EndpointConfiguration {
        return base.copy()
            .setTransportProfile(TransportProfile.TCP_UASC_UABINARY)
            .setBindPort(config.serverTcpPort)
            .build()
    }

    companion object {

        const val PRODUCT_URI = "urn:amazonaws:sfc:opcua-target"
        private const val MANUFACTURER_NAME = "AWS"
        private const val PRODUCT_NAME = "SFC OPCUA Target"
        private const val COMMON_NAME = "SFC-OPCUA-TARGET"

    }

}