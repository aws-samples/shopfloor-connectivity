// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.slmp.protocol

class SlmpDeviceItem(
    val accessPoint: SlmpAccessPoint,
    val dataType: SlmpDataType,
    val size: Int,
    val structName: String? = null,
    val structureType: SlmpStructureType? = null,
    val strLen: Int = 0
) {


    override fun toString(): String {
        return "SlmpItemDefinition(accessPoint=$accessPoint, dataType=$dataType, size=$size, structName=$structName, structureType=$structureType, strLen=$strLen)"
    }

    val devicesToRead: UShort by lazy {
        when (dataType) {
            SlmpDataType.WORD -> size
            SlmpDataType.BIT -> size
            SlmpDataType.DOUBLEWORD -> if (accessPoint.deviceType == SlmpDevice.DeviceType.DOUBLEWORD) size else size * 2
            SlmpDataType.STRING -> strLen * size
            SlmpDataType.STRUCT -> structureType!!.bytesNeeded
        }.toUShort()
    }

    val bytesNeeded: Int by lazy {
        when (dataType) {
            SlmpDataType.WORD -> size * 2
            SlmpDataType.BIT -> size / 2 + if (size % 2 != 0) 1 else 0
            SlmpDataType.DOUBLEWORD -> size * 4
            SlmpDataType.STRING -> (strLen * size) + if (strLen % 2 != 0) 1 else 0
            SlmpDataType.STRUCT -> structureType!!.bytesNeeded * size
        }
    }

    val canBeReadByDeviceReadRandom: Boolean by lazy {
        (size == 1 &&
                (dataType == SlmpDataType.WORD ||
                        dataType == SlmpDataType.DOUBLEWORD ||
                        dataType == SlmpDataType.BIT)
                )
    }


    companion object {

        fun build(accessPointStr: String, typeString: String? = null, size: Int? = null): SlmpDeviceItem =
            build(SlmpAccessPoint.fromString(accessPointStr), typeString, size)

        fun build(
            device: SlmpDevice,
            deviceNumber: UShort,
            typeString: String? = null,
            size: Int? = null
        ): SlmpDeviceItem {

            if (typeString.isNullOrEmpty()) {
                val type = when (device.deviceType) {
                    SlmpDevice.DeviceType.WORD -> SlmpDataType.WORD
                    SlmpDevice.DeviceType.DOUBLEWORD -> SlmpDataType.DOUBLEWORD
                    SlmpDevice.DeviceType.BIT -> SlmpDataType.BIT
                }
                return SlmpDeviceItem(
                    accessPoint = SlmpAccessPoint(device, deviceNumber),
                    dataType = type,
                    size = size ?: 1,
                    structName = null,
                    structureType = null,
                    strLen = 0
                )
            }

            val match =
                FIELD_REGEX.matchEntire(typeString.trim())
                    ?: throw SlmpException("Invalid field definition $typeString")

            val itemTypeName = match.groups["type"]?.value ?: ""
            val type: SlmpDataType = SlmpDataType.fromString(itemTypeName)


            val sizeStr = match.groups["size"]
            val itemsToRead = size ?: sizeStr?.value?.toInt() ?: 1

            val len = match.groups["len"]?.value?.toInt()
            val strLen = if (type == SlmpDataType.STRING) {
                if (len == null) throw SlmpException("String must have a length")
                if (len < 1) throw SlmpException("String length must be 1 or longer")
                len
            } else 1

            val structureType = if (type == SlmpDataType.STRUCT) {
                SlmpStructureType.knowStructureTypes[itemTypeName]
                    ?: throw SlmpException("Structure type \"$itemTypeName\" does not exist yet, know types are ${SlmpStructureType.knowStructureTypes.keys.joinToString()}")
            } else null

            return SlmpDeviceItem(
                accessPoint = SlmpAccessPoint(device, deviceNumber),
                dataType = type,
                size = itemsToRead,
                structName = itemTypeName,
                structureType = structureType,
                strLen = strLen
            )
        }

        fun build(accessPoint: SlmpAccessPoint, typeString: String? = null, size: Int? = null): SlmpDeviceItem =
            build(
                device = accessPoint.device,
                deviceNumber = accessPoint.deviceNumber,
                typeString = typeString,
                size = size
            )


        private val FIELD_REGEX = """^(?<type>\w+)(\((?<len>\d+)\))?(\[\s*(?<size>\d+)\s*\])?${'$'}""".toRegex()

    }


}