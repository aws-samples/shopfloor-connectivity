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

import com.amazonaws.sfc.data.ProtocolAdapterException
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.MetricDimensions
import com.amazonaws.sfc.metrics.MetricUnits
import com.amazonaws.sfc.metrics.MetricsCollector
import com.amazonaws.sfc.snmp.config.SnmpChannelConfiguration
import com.amazonaws.sfc.snmp.config.SnmpDeviceConfiguration
import com.amazonaws.sfc.snmp.config.SnmpNetworkProtocol
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.TransportMapping
import org.snmp4j.event.ResponseEvent
import org.snmp4j.smi.*
import org.snmp4j.transport.DefaultTcpTransportMapping
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.io.IOException

class SnmpDevice(
    private val sourceID: String,
    val adapterID: String,
    private val channelConfigMapping: Map<String, SnmpChannelConfiguration>,
    private val deviceConfiguration: SnmpDeviceConfiguration,
    private val metrics: MetricsCollector?,
    private val metricDimensions: MetricDimensions,
    private val logger: Logger) {

    private var transport: TransportMapping<*>? = null
    private var snmp = runBlocking { createSnmpInstance(adapterID, metrics, metricDimensions) }
    private val communityTarget = createCommunityTarget()
    private val variableBindings = createVariables()

    val lock = Mutex()

    val isListening: Boolean
        get() {
            return transport?.isListening ?: false
        }

    val objectToToChannelID = variableBindings.map {
        it.value.oid to it.key
    }.toMap()

    @Throws(IOException::class)
    fun readItems(): Iterable<ResponseEvent<*>> =
        sequence {
            variableBindings.values.windowed(size = deviceConfiguration.readBatchSize,
                step = deviceConfiguration.readBatchSize,
                partialWindows = true)
                .forEach { variables ->
                    val pdu = PDU()
                    pdu.addAll(variables)
                    pdu.requestID = Integer32(snmp.nextRequestID)
                    val response = snmp.get(pdu, communityTarget)
                    yield(response)
                }
        }.toList()


    suspend fun read(): Iterable<ResponseEvent<*>?> {
        var retries = 0
        if (!transport?.isListening!!) {
            snmp = createSnmpInstance(adapterID, metrics, metricDimensions)
        }

        while (true) {
            try {
                return readItems()
            } catch (e: IOException) {
                retries += 1

                val msg = "Error reading from SNMP device for source \"$sourceID\", $e"
                if (retries > deviceConfiguration.retries) {
                    throw ProtocolAdapterException(msg)
                }
                logger.getCtxWarningLog(msg + ", retry in ${deviceConfiguration.waitAfterReadError}")
                try {
                    snmp.close()
                } catch (_: Throwable) {
                }
                delay(deviceConfiguration.waitAfterReadError)
                snmp = createSnmpInstance(adapterID, metrics, metricDimensions)
            }
        }
    }

    private suspend fun createSnmpInstance(adapterID: String, metricsCollector: MetricsCollector?, metricsDimensions: MetricDimensions): Snmp {
        try {
            transport = createTransport()
            transport!!.listen()
            metricsCollector?.put(adapterID, MetricsCollector.METRICS_CONNECTIONS, 1.0, MetricUnits.COUNT, metricsDimensions)
        } catch (e: Exception) {
            metricsCollector?.put(adapterID, MetricsCollector.METRICS_CONNECTION_ERRORS, 1.0, MetricUnits.COUNT, metricsDimensions)
            throw e
        }
        return Snmp(transport)
    }

    private fun createTransport() = when (deviceConfiguration.networkProtocol) {
        SnmpNetworkProtocol.UDP -> DefaultUdpTransportMapping()
        SnmpNetworkProtocol.TCP -> DefaultTcpTransportMapping()
    }

    private fun createCommunityTarget(): CommunityTarget<out TransportIpAddress> {
        @Suppress("UNCHECKED_CAST") // cnn only be a list of this type
        val target = when (deviceConfiguration.networkProtocol) {
            SnmpNetworkProtocol.UDP -> CommunityTarget<UdpAddress>()
            SnmpNetworkProtocol.TCP -> CommunityTarget<TcpAddress>()
        } as CommunityTarget<TransportIpAddress>

        target.community = OctetString(deviceConfiguration.community)
        target.version = deviceConfiguration.snmpVersion
        val address = "${deviceConfiguration.address}/${deviceConfiguration.port}"
        target.address = when (deviceConfiguration.networkProtocol) {
            SnmpNetworkProtocol.UDP -> UdpAddress(address)
            SnmpNetworkProtocol.TCP -> TcpAddress(address)
        }
        target.retries = deviceConfiguration.retries
        target.timeout = deviceConfiguration.timeout

        return target
    }

    private fun createVariables(): Map<String, VariableBinding> {
        return channelConfigMapping.map { (channelID, channelConfiguration) ->
            channelID to VariableBinding(OID(channelConfiguration.objectID))
        }.toMap()
    }

    fun close() {
        snmp.close()
    }


}