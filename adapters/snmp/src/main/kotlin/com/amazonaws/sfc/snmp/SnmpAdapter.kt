/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.snmp


import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_ADAPTER
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_READS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_READ_DURATION
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_READ_ERRORS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_READ_SUCCESS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_VALUES_READ
import com.amazonaws.sfc.snmp.config.SnmpConfiguration
import com.amazonaws.sfc.snmp.config.SnmpDeviceConfiguration
import com.amazonaws.sfc.system.DateTime
import com.amazonaws.sfc.util.LookupCacheHandler
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.snmp4j.PDU
import org.snmp4j.asn1.BER
import org.snmp4j.smi.*
import kotlin.time.Duration


class SnmpAdapter(private val adapterID: String, private val configuration: SnmpConfiguration, private val logger: Logger) : ProtocolAdapter {

    private val className = this::class.java.simpleName

    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
    }

    private val adapterMetricDimensions = mapOf(MetricsCollector.METRICS_DIMENSION_TYPE to className)


    private val snmpDevices = LookupCacheHandler<String, SnmpDevice?, List<String>>(
        supplier = { null },
        initializer = { sourceID, _, channels ->
            val adapterID = configuration.snmpSources[sourceID]?.sourceAdapterDeviceID ?: return@LookupCacheHandler null
            val metricDimensions = mapOf(METRICS_DIMENSION_SOURCE to "$adapterID:$sourceID") + adapterMetricDimensions
            createSourceDevice(sourceID, channels, metricDimensions)
        },
        isValid = { device: SnmpDevice? -> device?.isListening ?: false }
    )

    private val sources
        get() = configuration.snmpSources.filter { it.value.protocolAdapterID in configuration.snmpProtocolAdapters.keys }

    override val metricsCollector: MetricsCollector? by lazy {
        val metricsConfigurations = configuration.snmpProtocolAdapters.map { it.key to (it.value.metrics ?: MetricsSourceConfiguration()) }.toMap()
        if (configuration.isCollectingMetrics) {

            logger.metricsCollectorMethod = collectMetricsFromLogger

            MetricsCollector(metricsConfig = configuration.metrics,
                metricsSourceType = MetricsSourceType.PROTOCOL_ADAPTER,
                metricsSourceConfigurations = metricsConfigurations,
                staticDimensions = ADAPTER_METRIC_DIMENSIONS,
                logger = logger)
        } else null
    }

    private val collectMetricsFromLogger: MetricsCollectorMethod? =
        if (configuration.isCollectingMetrics) {
            { metricsList ->
                try {
                    val dimensions = mapOf(METRICS_DIMENSION_SOURCE to adapterID) + adapterMetricDimensions
                    val dataPoints = metricsList.map { MetricsDataPoint(it.metricsName, dimensions, it.metricUnit, it.metricsValue) }
                    runBlocking {
                        metricsCollector?.put(adapterID, dataPoints)
                    }
                } catch (e: java.lang.Exception) {
                    logger.getCtxErrorLog(this::class.java.simpleName, "collectMetricsFromLogger")("Error collecting metrics from logger, $e")
                }
            }
        } else null


    /**
     * Reads a values from a source
     * @param sourceID String Source ID
     * @param channels List<String>? Channels to read values for, if null then all values for the source are read
     * @return SourceReadResult
     */
    override suspend fun read(sourceID: String, channels: List<String>?): SourceReadResult {

        val log = logger.getCtxLoggers(className, "read")

        log.trace("Reading ${if (channels.isNullOrEmpty()) "all" else channels.size} channels from SNMP source \"$sourceID\"")

        val sourceDevice = getSourceDevice(sourceID, channels)
        val metricDimensions = mapOf(METRICS_DIMENSION_SOURCE to sourceID, METRICS_DIMENSION_ADAPTER to sourceDevice!!.adapterID)
        val start = DateTime.systemDateTime().toEpochMilli()

        try {
            val readValues = mutableMapOf<String, ChannelReadValue>()

            sourceDevice.lock.withLock {

                sourceDevice.read().forEach { responseEvent ->

                    val timestamp = DateTime.systemDateTime()

                    if (responseEvent == null || responseEvent.response == null) {
                        throw ProtocolAdapterException("No response from SNMP device for source \"$sourceID\"")
                    }

                    val responsePdu = responseEvent.response
                    val errorStatus = responsePdu?.errorStatus
                    if (errorStatus != PDU.noError) {

                        val errorIndex = responsePdu.errorIndex - 1
                        val errorStatusText = responsePdu.errorStatusText
                        log.error("SNMP get error, Object ID \"${responsePdu.variableBindings[errorIndex].oid}\", $errorStatusText")
                    }

                    responsePdu.variableBindings.forEach {
                        val channelId = sourceDevice.objectToToChannelID[it.oid]
                        if (channelId != null) {
                            log.trace("Mapped Object Identifier \"${it.oid} to source channel \"$channelId\"")
                            val value = variableAsNative(it.variable)
                            if (value == null) {
                                log.error("No value for source \"$sourceID\", channel \"$channelId\" Object Identifier \"${it.oid}")
                            } else {
                                val channelReadValue = ChannelReadValue(value, timestamp)
                                readValues[channelId] = channelReadValue
                                log.trace("Value for channel \"$channelId\" is $channelReadValue")
                            }
                        }
                    }
                }
            }

            log.trace("${readValues.size} values read from SNMP device")
            val readDurationInMillis = (DateTime.systemDateTime().toEpochMilli() - start).toDouble()
            createMetrics(sourceDevice.adapterID, metricDimensions, readDurationInMillis, readValues)

            return SourceReadSuccess(readValues)
        } catch (e: Exception) {
            metricsCollector?.put(sourceDevice.adapterID, METRICS_READ_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions)
            return SourceReadError("Error reading from SNMP device $sourceID, $e")
        }
    }

    private suspend fun createMetrics(protocolAdapterID: String,
                                      metricDimensions: MetricDimensions?,
                                      readDurationInMillis: Double,
                                      values: MutableMap<String, ChannelReadValue>) {
        metricsCollector?.put(protocolAdapterID,
            metricsCollector?.buildValueDataPoint(protocolAdapterID, METRICS_READS, 1.0, MetricUnits.COUNT, metricDimensions),
            metricsCollector?.buildValueDataPoint(protocolAdapterID, METRICS_READ_DURATION, readDurationInMillis, MetricUnits.MILLISECONDS, metricDimensions),
            metricsCollector?.buildValueDataPoint(protocolAdapterID, METRICS_VALUES_READ, values.size.toDouble(), MetricUnits.COUNT, metricDimensions),
            metricsCollector?.buildValueDataPoint(protocolAdapterID, METRICS_READ_SUCCESS, 1.0, MetricUnits.COUNT, metricDimensions))
    }


    private fun variableAsNative(variable: Variable): Any? {
        return when (variable.syntax.toByte()) {
            BER.INTEGER -> (variable as Integer32).value
            BER.BITSTRING -> (variable as BitString).value
            BER.OCTETSTRING -> variable.toString()
            BER.OID -> variable.toString()
            BER.TIMETICKS -> (variable as TimeTicks).value
            BER.COUNTER -> (variable as Counter32).value
            BER.COUNTER64 -> (variable as Counter64).value
            BER.ENDOFMIBVIEW.toByte() -> null
            BER.GAUGE32 -> (variable as Gauge32).value
            BER.IPADDRESS -> (variable as IpAddress).toString()
            BER.NOSUCHINSTANCE.toByte() -> null
            BER.NOSUCHOBJECT.toByte() -> null
            BER.NULL -> null
            BER.OPAQUE -> (variable as Opaque).value
            else -> variable.toString()
        }
    }

    /**
     * Stops the adapter
     * @param timeout Duration Timeout period to wait for adapter to stop
     */
    override suspend fun stop(timeout: Duration) {
        snmpDevices.items.forEach {
            withTimeoutOrNull(timeout) { it }?.close()
        }
    }

    /**
     * Get the Snmp instance and target for a source
     * @param sourceID String
     * @return Snmp instance and target
     */
    private fun createSourceDevice(sourceID: String, channelsToRead: List<String>?, metricDimensions: MetricDimensions): SnmpDevice? {
        val adapterID = configuration.snmpSources[sourceID]?.protocolAdapterID ?: return null
        val deviceConfiguration = getSourceDeviceConfiguration(sourceID) ?: return null
        val snmpChannelConfigMap = configuration.snmpSources[sourceID]?.channels?.filter { channelsToRead.isNullOrEmpty() || channelsToRead.contains(it.key) }
        return SnmpDevice(sourceID, adapterID, snmpChannelConfigMap ?: emptyMap(), deviceConfiguration, metricsCollector, metricDimensions, logger)
    }

    private suspend fun getSourceDevice(sourceID: String, channels: List<String>?) =
        withTimeoutOrNull(1000) { snmpDevices.getItemAsync(sourceID, channels).await() }


    private fun getSourceDeviceConfiguration(sourceID: String): SnmpDeviceConfiguration? {

        val log = logger.getCtxLoggers(className, "getSourceDeviceConfiguration")

        val sourceConfiguration = sources[sourceID]
        if (sourceConfiguration == null) {
            log.error("Source \"$sourceID\" does not exist, available sources are ${sources.keys}")
            return null
        }

        val adapterConfiguration = configuration.snmpProtocolAdapters[sourceConfiguration.protocolAdapterID]
        if (adapterConfiguration == null) {
            log.error("Adapter \"${sourceConfiguration.protocolAdapterID}\" for  Source \"$sourceID\" does not exist, available adapters are ${configuration.snmpProtocolAdapters.keys}")
            return null
        }
        val deviceConfiguration = adapterConfiguration.devices[sourceConfiguration.sourceAdapterDeviceID]
        if (deviceConfiguration == null) {
            log.error("Device \"${sourceConfiguration.sourceAdapterDeviceID}\" Adapter \"${sourceConfiguration.protocolAdapterID}\" for  Source \"$sourceID\" does not exist, available devices are ${adapterConfiguration.devices}")
            return null
        }

        return deviceConfiguration
    }


    companion object {

        private val createInstanceMutex = Mutex()

        @JvmStatic @Suppress("unused") fun newInstance(vararg createParams: Any) = newInstance(createParams[0] as ConfigReader, createParams[1] as String, createParams[2] as String, createParams[3] as Logger)


        /**
         * Creates a new reader for the Snmp protocol adapter from its configuration
         * @param configReader ConfigReader Configuration reader for the adapter
         * @see com.amazonaws.snmp.sfc.config.SnmpConfiguration
         * @param scheduleName String Name of the schedule
         * @param logger Logger Logger for output
         * @return SourceValuesReader? Created reader
         */
        @JvmStatic fun newInstance(configReader: ConfigReader, scheduleName: String, adapterID: String, logger: Logger): SourceValuesReader? {


            runBlocking {
                createInstanceMutex.withLock {
                    if (adapter == null) {
                        adapter = createSnmpAdapter(adapterID, configReader, logger)
                    }
                }
            }

            val config = configReader.getConfig<SnmpConfiguration>()
            val schedule = config.schedules.firstOrNull { it.name == scheduleName }
            val sourcesForAdapter = schedule?.sources?.filter { (config.snmpSources[it.key]?.protocolAdapterID ?: "") == adapterID } ?: return null

            return if (adapter != null) InProcessSourcesReader.createInProcessSourcesReader(schedule, adapter!!, sourcesForAdapter, config.metrics, logger) else null

        }

        private var adapter: ProtocolAdapter? = null

        /**
         * Creates an SNMP adapter from its configuration
         * @param configReader ConfigReader Reader for configuration
         * @param logger Logger Logger for output
         * @return ProtocolAdapter
         */
        fun createSnmpAdapter(adapterID: String, configReader: ConfigReader, logger: Logger): ProtocolAdapter {

            // obtain snmp configuration
            val config: SnmpConfiguration = try {
                configReader.getConfig()
            } catch (e: Exception) {
                throw ProtocolAdapterException("Error loading configuration: ${e.message}")
            }
            return SnmpAdapter(adapterID, config, logger)
        }

        private val ADAPTER_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER)

    }
}
