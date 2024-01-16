package com.amazonaws.sfc.ads.protocol

import com.amazonaws.sfc.ads.protocol.Decoder.toInt16
import com.amazonaws.sfc.ads.protocol.Decoder.toInt32
import com.google.gson.Gson

class PlcTaskSystemInfo(
    val objID: UInt = 0.toUInt(),
    val cycleTime: UInt = 0.toUInt(),
    val priority: UShort = 0.toUShort(),
    val adsPort: UShort = 0.toUShort(),
    val cycleCount: UInt = 0.toUInt(),
    val dcTaskTime: String = "",
    val lastExecTime: UInt = 0.toUInt(),
    val firstCycle: Boolean = false,
    val cycleTimeExceeded: Boolean = false,
    val taskName: String = ""
) {

    override fun toString(): String {
        return Gson().toJson(this)
    }


    class PlcTaskSystemInfoBuilder {

        private var objId: UInt = 0.toUInt()
        private var cycleTime: UInt = 0.toUInt()
        private var priority: UShort = 0.toUShort()
        private var adsPort: UShort = 0.toUShort()
        private var cycleCount: UInt = 0.toUInt()
        private var dcTaskTime: String = ""
        private var lastExecTime: UInt = 0.toUInt()
        private var firstCycle: Boolean = false
        private var cycleTimeExceeded: Boolean = false
        private var taskName: String = ""


        fun objId(objId: UInt): PlcTaskSystemInfoBuilder {
            this.objId = objId
            return this
        }

        fun cycleTime(cycleTime: UInt): PlcTaskSystemInfoBuilder {
            this.cycleTime = cycleTime
            return this
        }

        fun priority(priority: UShort): PlcTaskSystemInfoBuilder {
            this.priority = priority
            return this
        }

        fun adsPort(adsPort: UShort): PlcTaskSystemInfoBuilder {
            this.adsPort = adsPort
            return this
        }

        fun cycleCount(cycleCount: UInt): PlcTaskSystemInfoBuilder {
            this.cycleCount = cycleCount
            return this
        }

        fun dcTaskTime(dcTaskTime: String): PlcTaskSystemInfoBuilder {
            this.dcTaskTime = dcTaskTime
            return this
        }

        fun lastExecTime(lastExecTime: UInt): PlcTaskSystemInfoBuilder {
            this.lastExecTime = lastExecTime
            return this
        }

        fun firstCycle(firstCycle: Boolean): PlcTaskSystemInfoBuilder {
            this.firstCycle = firstCycle
            return this
        }

        fun cycleTimeExceeded(cycleTimeExceeded: Boolean): PlcTaskSystemInfoBuilder {
            this.cycleTimeExceeded = cycleTimeExceeded
            return this
        }

        fun taskName(taskName: String): PlcTaskSystemInfoBuilder {
            this.taskName = taskName
            return this
        }


        fun build() = PlcTaskSystemInfo(
            objId,
            cycleTime,
            priority,
            adsPort,
            cycleCount,
            dcTaskTime,
            lastExecTime,
            firstCycle,
            cycleTimeExceeded,
            taskName
        )
    }


    companion object {

        private fun builder() = PlcTaskSystemInfoBuilder()


        fun fromBytes(bytes: ByteArray): PlcTaskSystemInfo {

            return builder()
                .objId(bytes.toInt32(0).toUInt())
                .cycleTime(bytes.toInt32(4).toUInt())
                .priority(bytes.toInt16(8).toUShort())
                .adsPort(bytes.toInt16(10).toUShort())
                .cycleCount(bytes.toInt32(12).toUInt())
                .dcTaskTime(Decoder.decodeLDateTime(bytes.sliceArray(16..<24)))
                .lastExecTime(bytes.toInt32(24).toUInt())
                .firstCycle(bytes[28].toInt() == 1)
                .cycleTimeExceeded(bytes[29].toInt() == 1)
                .taskName(Decoder.decodeString(bytes.sliceArray(64..<128)))
                .build()
        }
    }


}