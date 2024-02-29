
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.modbus.tcp

import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.config.ScheduleConfiguration
import com.amazonaws.sfc.data.InProcessSourcesReader
import com.amazonaws.sfc.data.ProtocolAdapter
import com.amazonaws.sfc.data.SourceValuesReader
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_ADAPTER
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_TYPE
import com.amazonaws.sfc.modbus.adapter.ModbusAdapter
import com.amazonaws.sfc.modbus.adapter.ModbusDevice
import com.amazonaws.sfc.modbus.config.ModbusSourceConfiguration
import com.amazonaws.sfc.modbus.protocol.Modbus
import com.amazonaws.sfc.modbus.protocol.ModbusTransport
import com.amazonaws.sfc.modbus.tcp.config.ModbusTcpAdapterConfiguration
import com.amazonaws.sfc.modbus.tcp.config.ModbusTcpConfiguration
import com.amazonaws.sfc.modbus.tcp.config.ModbusTcpDeviceConfiguration
import com.amazonaws.sfc.modbus.tcp.config.ModbusTcpDeviceConfiguration.Companion.DEFAULT_REQUEST_DEPTH
import com.amazonaws.sfc.modbus.tcp.protocol.ModbusTCP
import com.amazonaws.sfc.modbus.tcp.transport.TcpTransport
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.isJobCancellationException
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration

/**
 * Protocol adapter for Modbus TCP protocol
 */
class ModbusTcpAdapter(private val adapterID: String, private val configuration: ModbusTcpConfiguration, logger: Logger) :
        ModbusAdapter(logger = logger) {

    private val className = this::class.java.simpleName

    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
    }

    private val dimensions = mapOf(
        METRICS_DIMENSION_SOURCE to adapterID,
        METRICS_DIMENSION_TYPE to className)

    /**
     * Max requests that can be sent before receiving a response
     * @param sourceConfiguration ModbusSourceConfiguration configuration of the source
     * @return UShort number of requests
     */
    override fun requestDepth(sourceConfiguration: ModbusSourceConfiguration): UShort {
        val adapterDevices = configuration.modbusTcpAdapters[sourceConfiguration.protocolAdapterID]?.devices
        return (adapterDevices?.get(sourceConfiguration.sourceAdapterDevice)?.requestDepth ?: DEFAULT_REQUEST_DEPTH)
    }

    /**
     * Modbus source devices to read from
     */
    override val sourceDevices: Map<String, ModbusDevice> by lazy {
        initializeSourceDevices()
    }

    override val metricsCollector: MetricsCollector? by lazy {
        val metricsConfigurations = configuration.modbusTcpAdapters.map { it.key to (it.value.metrics ?: MetricsSourceConfiguration()) }.toMap()

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
     * Stop the protocol adapter
     * @param timeout Duration Timeout to wait for adapter to stop
     */
    override suspend fun stop(timeout: Duration) = coroutineScope {

        val sources = sourceDevices.map {
            launch("Stop Device") {
                it.value.stop()
            }
        }
        val stop = modbusDevices.values.flatMap { adapter ->
            adapter.map { device ->
                launch("Close Modbus device  ${device.key}") {
                    device.value?.close()
                }
            }
        }

        try {
            withTimeout(timeout) {
                joinAll(*sources.toTypedArray(), *stop.toTypedArray())
            }
        } catch (_: TimeoutCancellationException) {
        }catch ( e : Exception){
            if (!e.isJobCancellationException) throw e
        }


    }

    // Startup devices for the servers from which values are read
    private fun setupModbusDevices(): Map<String, Map<String, ModbusTransport?>> {

        val trace = logger.getCtxTraceLog(className, "setupModbusDevices")

        val devices = configuration.activeDevices.map { (adapterID, adapter) ->
            adapterID to
                    adapter.map { device ->
                        device.key to TcpTransport(device.value,
                            logger = logger,
                            metrics = metricsCollector,
                            adapterID = adapterID,
                            metricDimensions = mapOf(METRICS_DIMENSION_ADAPTER to adapterID))
                    }.toMap()
        }.toMap()

        devices.forEach { adapter ->
            adapter.value.forEach { device ->
                trace("Starting device ${device.key} for adapter ${adapter.key}")
                device.value.start()
            }
        }
        return devices
    }

    /**
     * Returns all devices used by the adapter
     */
    private val modbusDevices: Map<String, Map<String, com.amazonaws.sfc.modbus.protocol.ModbusTransport?>> by lazy { setupModbusDevices() }


    private fun initializeSourceDevices(): Map<String, ModbusDevice> {

        val errorLog = logger.getCtxErrorLog(className, "initializeSourceDevices")

        return configuration.activeSources.mapNotNull { source ->
            val modbusDevice = modbusDevices[source.value.protocolAdapterID]?.get(source.value.sourceAdapterDevice)
            if (modbusDevice == null) null
            else {

                val adapterConfig = configuration.modbusTcpAdapters[source.value.protocolAdapterID]
                if (adapterConfig == null) {
                    errorLog("No MQTT adapter \"${source.value.protocolAdapterID}\" found for source \"${source.key}\", available MQTT adapters are ${configuration.modbusTcpAdapters.keys}")
                    return@mapNotNull null
                }
                val adapterDevice = adapterConfig.devices[source.value.sourceAdapterDevice]
                if (adapterDevice == null) {
                    errorLog("No device \"${source.value.sourceAdapterDevice}\" found in MQTT adapter \"${source.value.protocolAdapterID}\" for source ${source.key}\", available devices are ${adapterConfig.devices.keys}")
                    return@mapNotNull null
                }
                // Create modbus protocol handler
                val modbusHandler = ModbusTCP(modbusDevice, Modbus.READ_TIMEOUT, logger)
                // Create the modbus device
                source.key to ModbusDevice(sourceID = source.key, configuration = source.value, modbus = modbusHandler, ownerAdapter = this, deviceID = adapterDevice.deviceID)
            }

        }.toMap()
    }

    companion object {

        private val createInstanceMutex = Mutex()

        fun createModbusTcpAdapter(adapterID: String, configReader: ConfigReader, logger: Logger): ModbusTcpAdapter {
            val config: ModbusTcpConfiguration = try {
                configReader.getConfig()
            } catch (e: Exception) {
                throw TargetException("Error loading configuration: ${e.message}")
            }
            return ModbusTcpAdapter(adapterID, config, logger)
        }


        @JvmStatic
        fun newInstance(vararg createParams: Any) =
            newInstance(createParams[0] as ConfigReader, createParams[1] as String, createParams[2] as String, createParams[3] as Logger)

        /**
         * Creates a new reader for the Modbus TCP protocol adapter from its configuration
         * @param configReader ConfigReader Configuration reader for the adapter
         * @param scheduleName String Name of the schedule
         * @param logger Logger Logger for output
         * @return SourceValuesReader? Created reader
         */
        @JvmStatic
        fun newInstance(configReader: ConfigReader, scheduleName: String, adapterID: String, logger: Logger): SourceValuesReader? {


            runBlocking {
                createInstanceMutex.withLock {
                    if (adapter == null) {
                        adapter = createModbusTcpAdapter(adapterID, configReader, logger)
                    }
                }
            }

            val config = configReader.getConfig<ModbusTcpConfiguration>()
            val schedule: ScheduleConfiguration = config.schedules.firstOrNull { it.name == scheduleName } ?: return null

            val sourcesForAdapter = schedule.sources.filter { (config.sources[it.key]?.protocolAdapterID ?: "") == adapterID }
            return if (adapter != null) InProcessSourcesReader.createInProcessSourcesReader(schedule, adapter!!, sourcesForAdapter, config.metrics, logger) else null

        }

        private var adapter: ProtocolAdapter? = null

        private val ADAPTER_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER)

    }

    // All adapter configurations that are used by sources in active schedules
    private val ModbusTcpConfiguration.activeAdapters: Map<String, ModbusTcpAdapterConfiguration>
        get() {
            val activeSchedules = schedules.filter { it.active }
            val usedSources = if (activeSchedules.isNotEmpty()) activeSchedules.flatMap { it.sources.keys } else sources.keys
            val activeAdapterIds = usedSources.mapNotNull { sources[it]?.protocolAdapterID }
            return modbusTcpAdapters.filter { it.key in activeAdapterIds }
        }

    // All adapter configurations that are used by sources in active schedules
    private val ModbusTcpConfiguration.activeDevices: Map<String, Map<String, ModbusTcpDeviceConfiguration>>
        get() {
            return activeAdapters.map { adapter ->
                adapter.key to adapter.value.devices
            }.toMap()
        }


}