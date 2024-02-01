
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.s7

import org.apache.plc4x.java.api.messages.PlcReadRequest
import org.apache.plc4x.java.api.messages.PlcRequest
import org.apache.plc4x.java.api.value.PlcValueHandler
import org.apache.plc4x.java.s7.readwrite.S7Driver
import org.apache.plc4x.java.s7.readwrite.TPKTPacket
import org.apache.plc4x.java.s7.readwrite.configuration.S7Configuration
import org.apache.plc4x.java.s7.readwrite.context.S7DriverContext
import org.apache.plc4x.java.s7.readwrite.field.S7Field
import org.apache.plc4x.java.s7.readwrite.field.S7PlcFieldHandler
import org.apache.plc4x.java.s7.readwrite.io.TPKTPacketIO
import org.apache.plc4x.java.s7.readwrite.optimizer.S7Optimizer
import org.apache.plc4x.java.s7.readwrite.protocol.S7ProtocolLogic
import org.apache.plc4x.java.spi.configuration.Configuration
import org.apache.plc4x.java.spi.connection.ProtocolStackConfigurer
import org.apache.plc4x.java.spi.connection.SingleProtocolStackConfigurer
import org.apache.plc4x.java.spi.context.DriverContext
import org.apache.plc4x.java.spi.optimizer.BaseOptimizer
import org.apache.plc4x.java.spi.values.IEC61131ValueHandler

// Custom S7 driver class which inherits from the CustomDriver Base.
// Additionally, to the PLC4J S7 driver class it is using a function parameter that is
// called by a custom S7 optimizer to extract the S7 Driver context from the initialization request response.
internal class CustomS7Driver(private val fnDriverContext: (S7DriverContext) -> Unit) : CustomDriverBase<TPKTPacket>() {

    // Inherited class used to intercept driver context which is protected in superclass
    private class CustomS7Optimizer(private val fnDriverContext: (S7DriverContext) -> Unit) : S7Optimizer() {
        override fun processReadRequest(
            readRequest: PlcReadRequest?,
            driverContext: DriverContext?
        ): MutableList<PlcRequest> {
            // call interceptor method passing the driver context
            fnDriverContext(driverContext as S7DriverContext)
            return super.processReadRequest(readRequest, driverContext)
        }
    }


    override fun getProtocolCode(): String = "s7"

    override fun getProtocolName(): String = "Siemens S7 (Basic)"

    override fun getConfigurationType(): Class<out Configuration?> = S7Configuration::class.java

    override fun getDefaultTransport(): String = "tcp"

    override fun canRead(): Boolean = true

    override fun canWrite(): Boolean = true

    override fun canSubscribe(): Boolean = false

    // Using a custom optimizer to obtain additional driver data from initialization response
    override fun getOptimizer(): BaseOptimizer = CustomS7Optimizer(fnDriverContext)

    override fun getFieldHandler(): S7PlcFieldHandler = S7PlcFieldHandler()

    override fun getValueHandler(): PlcValueHandler = IEC61131ValueHandler()

    override fun awaitDisconnectComplete(): Boolean = false

    override fun getStackConfigurer(): ProtocolStackConfigurer<TPKTPacket>? =
        SingleProtocolStackConfigurer.builder(TPKTPacket::class.java, TPKTPacketIO::class.java)
            .withProtocol(S7ProtocolLogic::class.java)
            .withDriverContext(S7DriverContext::class.java)
            .withPacketSizeEstimator(S7Driver.ByteLengthEstimator::class.java)
            .withCorruptPacketRemover(S7Driver.CorruptPackageCleaner::class.java)
            .build()

    override fun prepareField(query: String?): S7Field = S7Field.of(query)

}
