// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.opcuawritetarget

import com.amazonaws.sfc.crypto.CertificateFormat
import com.amazonaws.sfc.crypto.CertificateHelper
import com.amazonaws.sfc.crypto.PkcsCertificateHelper
import com.amazonaws.sfc.crypto.subjectAlternativeApplicationUri
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.MetricDimensions
import com.amazonaws.sfc.metrics.MetricUnits
import com.amazonaws.sfc.metrics.MetricsCollector
import com.amazonaws.sfc.opcuawritetarget.config.OpcuaSecurityPolicy
import com.amazonaws.sfc.opcuawritetarget.config.OpcuaWriterTargetConfiguration
import com.amazonaws.sfc.system.DateTime
import com.amazonaws.sfc.system.DateTime.add
import com.amazonaws.sfc.system.DateTime.systemDateTime
import com.amazonaws.sfc.system.DateTime.systemDateUTC
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.eclipse.milo.opcua.sdk.client.OpcUaClient
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder
import org.eclipse.milo.opcua.stack.client.security.DefaultClientCertificateValidator
import org.eclipse.milo.opcua.stack.core.StatusCodes
import org.eclipse.milo.opcua.stack.core.UaException
import org.eclipse.milo.opcua.stack.core.channel.MessageLimits
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger
import org.eclipse.milo.opcua.stack.core.types.enumerated.UserTokenType
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription
import org.eclipse.milo.opcua.stack.core.types.structured.ServiceFault
import org.eclipse.milo.opcua.stack.core.types.structured.UserTokenPolicy
import org.eclipse.milo.opcua.stack.core.util.EndpointUtil
import sun.security.x509.X509CertImpl
import java.net.URI
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.Period
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.io.path.Path
import kotlin.io.path.exists


open class OpcuaWriter(
    private val targetID: String,
    private val targetConfiguration: OpcuaWriterTargetConfiguration,
    private val logger: Logger,
    private val metricsCollector: MetricsCollector?,
    adapterMetricDimensions: MetricDimensions?
) {

    private val className = this::class.simpleName.toString()
    private val targetScope = buildScope("OPCUA WRITE TARGET")

    private var isClosing: Boolean = false

    private val dimensions = mapOf(MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY_TARGET to "${targetID}AdapterID:$targetID") + adapterMetricDimensions as Map<String, String>

    // last fault whilst interacting with the server
    private var targetServerFault: ServiceFault? = null


    // backup field for client, used explicit field to allow testing the actual value without creating a new one on demand
    private var _opcuaClient: OpcUaClient? = null

    private var certificateExpiryChecker: Job? = null

    // gets a OPCUA client for the target
    suspend fun getClient(): OpcUaClient? {

        // pause after errors
        if (systemDateTime() < pauseWaitUntil) {
            return null
        }

        // use existing client
        if ((_opcuaClient != null)) {
            return _opcuaClient
        }


        // or create a new client
        createClientLock.withLock {
            if (_opcuaClient == null) {
                _opcuaClient = createOpcuaClient()
                _opcuaClient?.addFaultListener { fault ->
                    // Bad_NoSubscription is returned incorrectly after creating a new connection with subscriptions
                    if (fault.responseHeader.serviceResult != StatusCode(StatusCodes.Bad_NoSubscription))
                        targetServerFault = fault
                }
            }

            // after failing to connect temporary pause writing to the target
            if (_opcuaClient == null) {
                val waitingPeriod = targetConfiguration.waitAfterConnectError
                pauseWaitUntil = systemDateTime().plusMillis(waitingPeriod.inWholeMilliseconds)
                logger.getCtxInfoLog(className, "getClient")("Writing to target \"$targetID\" paused for $waitingPeriod until $pauseWaitUntil")
           }
        }

        return _opcuaClient
    }


    // if an error occurs the target will pause for a configured period
    private var pauseWaitUntil: Instant = Instant.ofEpochSecond(0L)

    // lock to prevent creation of clients whilst one is being created
    private val createClientLock = Mutex()

    private var trustManager: ClientTrustListManager? = null

    // creates the client to communicate with the server the target is writing to
    private fun createOpcuaClient(): OpcUaClient? {

        val log = logger.getCtxLoggers(className, "createOpcuaClient")

        log.info("Creating client for target \"$targetID\", on ${targetConfiguration.endPoint}")

        val securityPolicy = targetConfiguration.securityPolicy
        val userTokenType = UserTokenType.Anonymous

        val predicate =
            { e: EndpointDescription ->
                securityPolicy.policy.uri == e.securityPolicyUri &&
                        Arrays.stream(e.userIdentityTokens)
                            .anyMatch { p: UserTokenPolicy -> p.tokenType == userTokenType }
            }


        // build the client configuration
        val clientConfig = { configBuilder: OpcUaClientConfigBuilder ->
            configBuilder.setConnectTimeout(UInteger.valueOf(targetConfiguration.connectTimeout.inWholeMilliseconds))
                .setRequestTimeout(UInteger.valueOf(targetConfiguration.writeTimeout.inWholeMilliseconds))
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
            val host = URI(targetConfiguration.address).host
            OpcUaClient.create(
                targetConfiguration.endPoint,
                { endpoints: List<EndpointDescription> ->
                    endpoints.stream()
                        .filter(predicate)
                        .map { endpoint -> EndpointUtil.updateUrl(endpoint, host) }
                        .findFirst()
                }, clientConfig
            )

        } catch (e: UaException) {
            val cause = (if (e.cause != null) e.cause!!.message else e.message).toString()
            log.error("Error creating client for for target \"$targetID\" at  ${targetConfiguration.endPoint}, $cause")
            null
        } catch (e: Exception) {
            log.errorEx("Error creating client for for target \"$targetID\" at  ${targetConfiguration.endPoint}", e)
            null
        }

        // if the client was created connect to server
        return if (client != null) {
            try {
                val opcuaClient = client.connect().join() as OpcUaClient?
                log.info("Client for target \"$targetID\" connected to ${targetConfiguration.endPoint}")
                metricsCollector?.put(targetID, MetricsCollector.METRICS_CONNECTIONS, 1.0, MetricUnits.COUNT, dimensions)
                opcuaClient
            } catch (e: Exception) {
                log.errorEx("Error connecting at ${targetConfiguration.endPoint} for target \"$targetID\"", e)
                metricsCollector?.put(targetID, MetricsCollector.METRICS_CONNECTION_ERRORS, 1.0, MetricUnits.COUNT, dimensions)
                null
            }
        } else null

    }

    private val messageLimits: MessageLimits
        get() = MessageLimits(
            targetConfiguration.maxChunkSize,
            targetConfiguration.maxChunkCount,
            targetConfiguration.maxMessageSize
        )

    private fun OpcUaClientConfigBuilder.setupClientSecurity(): OpcUaClientConfigBuilder {

        val log = logger.getCtxLoggers(className, "OpcUaClientConfigBuilder.setupClientSecurity")

        if (targetConfiguration.securityPolicy == OpcuaSecurityPolicy.None) return this

        val certificateConfiguration = targetConfiguration.certificateConfiguration
        if (certificateConfiguration == null) {
            log.error("A certificate must be configured for a server using security policy ${targetConfiguration.securityPolicy}")
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

        val certificateConfiguration = targetConfiguration.certificateConfiguration
        val expirationWarningPeriod = certificateConfiguration?.expirationWarningPeriod ?: 0

        if (certificate == null || certificateConfiguration == null || expirationWarningPeriod <= 0) {
            certificateExpiryChecker?.cancel()
            return null
        }

        return targetScope.launch("OPCUA Certificate Expiry Watcher", Dispatchers.IO) {

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
        if (targetConfiguration.securityPolicy == OpcuaSecurityPolicy.None) return this

        val validationConfiguration = targetConfiguration.certificateValidationConfiguration ?: return this
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


    private var lock = ReentrantLock()

    private fun resetClient(waitFor: Long = 0) {

        if (lock.isHeldByCurrentThread || !lock.tryLock()) return

        try {

            if (isClosing) return

            isClosing = true

            trustManager?.close()
            trustManager = null

            targetServerFault = null

            _opcuaClient?.disconnect()
            _opcuaClient = null
            targetServerFault = null
            pauseWaitUntil = systemDateTime().plusMillis(waitFor)
        } finally {
            isClosing = false
            lock.unlock()
        }
    }

    fun close() {
        certificateExpiryChecker?.cancel()
        _opcuaClient?.disconnect()
    }


}

