// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.slmp.protocol

class SlmpStructureType(private var fields : List<Pair<String, SlmpDeviceItem>>){

    val bytesNeeded : Int by lazy {
             fields.sumOf { (_, structuresItem) ->
                structuresItem.bytesNeeded
            }
    }

    fun decode( bytes: ByteArray, itemDefinition : SlmpDeviceItem) : Any {

        val structs = mutableListOf<Map<String,Any>>()
        var offset = 0


        val bytesPerItem = bytesNeeded / itemDefinition.size
        while(structs.size < itemDefinition.size &&  bytesPerItem + offset < bytes.size) {
            val struct = fields.associate { (name, field: SlmpDeviceItem) ->
                val numberOfBytesForField = field.bytesNeeded
                val fieldBytes = bytes.copyOfRange(offset, offset + numberOfBytesForField)
                val fieldValue = SlmpDecoder.decode(field, fieldBytes)

                offset += numberOfBytesForField

                name to fieldValue
            }.toMap()

            structs.add(struct)
        }

        return if (structs.size == 1) return structs.first() else structs

    }

    override fun toString(): String {
        return "SlmpStructureDefinition(fields=$fields)"
    }


    companion object{
        fun fromString(map : Map<String,String>) : SlmpStructureType {

            val fields = map.map {
                Pair(it.key, SlmpDeviceItem.build(SlmpDevice.NULL_WORD_DEVICE, 0U, it.value))
            }
            return SlmpStructureType(fields)

        }

        private val _knowStructureTypes = mutableMapOf<String, SlmpStructureType>()

        fun registerStructureType(name : String, structureType : SlmpStructureType){
            _knowStructureTypes[name]  = structureType
        }

        val knowStructureTypes : Map<String, SlmpStructureType>
            get() = _knowStructureTypes
    }
}