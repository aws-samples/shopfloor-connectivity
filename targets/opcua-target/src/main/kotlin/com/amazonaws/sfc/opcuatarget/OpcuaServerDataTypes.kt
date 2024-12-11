// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.opcuatarget


import com.amazonaws.sfc.data.JsonHelper
import com.amazonaws.sfc.opcuatarget.OpcuaServerDataTypes.entries
import org.eclipse.milo.opcua.stack.core.Identifiers
import org.eclipse.milo.opcua.stack.core.types.builtin.*
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger

enum class OpcuaServerDataTypes {

    BOOLEAN {
        override val identifier: NodeId
            get() = Identifiers.Boolean
    },
    BYTE {
        override val identifier: NodeId
            get() = Identifiers.SByte
    },
    SBYTE {
        override val identifier: NodeId
            get() = Identifiers.SByte
    },
    BYTESTRING {
        override val identifier: NodeId
            get() = Identifiers.ByteString
    },
    DATETIME {
        override val identifier: NodeId
            get() = Identifiers.DateTime
    },
    DOUBLE {
        override val identifier: NodeId
            get() = Identifiers.Double
    },
    EXPANDEDNODEID {
        override val identifier: NodeId
            get() = Identifiers.ExpandedNodeId
    },

    FLOAT {
        override val identifier: NodeId
            get() = Identifiers.Float
    },
    INT {
        override val identifier: NodeId
            get() = Identifiers.Int32
    },
    INTEGER {
        override val identifier: NodeId
            get() = Identifiers.Int32
    },
    INT32 {
        override val identifier: NodeId
            get() = Identifiers.Int32
    },
    LOCALIZEDTEXT {
        override val identifier: NodeId
            get() = Identifiers.LocalizedText
    },
    LONG {
        override val identifier: NodeId
            get() = Identifiers.Int64
    },
    INT64 {
        override val identifier: NodeId
            get() = Identifiers.Int64
    },
    NODEID {
        override val identifier: NodeId
            get() = Identifiers.NodeId
    },
    QUALIFIEDNAME {
        override val identifier: NodeId
            get() = Identifiers.QualifiedName
    },
    REAL{
        override val identifier: NodeId
            get() = Identifiers.Float
    },
    SHORT {
        override val identifier: NodeId
            get() = Identifiers.Int16
    },
    INT16 {
        override val identifier: NodeId
            get() = Identifiers.Int16
    },
    STRING {
        override val identifier: NodeId
            get() = Identifiers.String
    },
    UINT {
        override val identifier: NodeId
            get() = Identifiers.UInt32
    },
    UINT32 {
        override val identifier: NodeId
            get() = Identifiers.UInt32
    },
    UINTEGER {
        override val identifier: NodeId
            get() = Identifiers.UInt32
    },
    UUID {
        override val identifier: NodeId
            get() = Identifiers.Guid
    },
    XML_ELEMENT {
        override val identifier: NodeId
            get() = Identifiers.XmlElement
    },
    UBYTE {
        override val identifier: NodeId
            get() = Identifiers.Byte
    },
    ULONG {
        override val identifier: NodeId
            get() = Identifiers.UInt64
    },
    UINT64 {
        override val identifier: NodeId
            get() = Identifiers.UInt64
    },
    USHORT {
        override val identifier: NodeId
            get() = Identifiers.UInt16
    },
    UINT16 {
        override val identifier: NodeId
            get() = Identifiers.UInt16
    },
    STRUCT {
        override val identifier: NodeId
            get() = Identifiers.Structure
    },
    VARIANT {
        override val identifier: NodeId
            get() = Identifiers.BaseDataVariableType
    },
    UNDEFINED {
        override val identifier: NodeId
            get() = Identifiers.BaseDataVariableType
    };

    abstract val identifier: NodeId

    companion object {
        fun fromString(value: String): OpcuaServerDataTypes {
            val s = value.uppercase().trim().replace("_", "")
            return entries.find { it.name == s } ?: UNDEFINED
        }


        private fun convert(value: Any?, dataTypeIdentifier: NodeId?, dimensions: List<Int>?): Any? {
            var v = value
            try {
                if (v is List<*> && dimensions != null) {

                    v = when (dataTypeIdentifier) {

                        Identifiers.Boolean ->
                            deepCast<Boolean>(dimensions, v) { it as Boolean }

                        Identifiers.SByte ->
                            deepCast<Byte>(dimensions, v) { it as Byte }

                        Identifiers.ByteString ->
                            deepCast<ByteString>(dimensions, v) {
                                ByteString.of(it.toString().encodeToByteArray())
                            }

                        Identifiers.String ->
                            deepCast<String>(dimensions, v) { it as String }

                        Identifiers.Structure ->
                            deepCast<String>(dimensions, v) { JsonHelper.gsonExtended().toJson(it) }

                        Identifiers.DateTime ->
                            deepCast<DateTime>(dimensions, v) { it as DateTime }

                        Identifiers.Double ->
                            deepCast<Double>(dimensions, v) { it as Double }

                        Identifiers.ExpandedNodeId ->
                            deepCast<ExpandedNodeId>(dimensions, v) { it as ExpandedNodeId }

                        Identifiers.Float ->
                            deepCast<Float>(dimensions, v) { it as Float }

                        Identifiers.Int16 ->
                            deepCast<Short>(dimensions, v) { it as Short }

                        Identifiers.Int32 ->
                            deepCast<Int>(dimensions, v) { it as Int }

                        Identifiers.Int64 ->
                            deepCast<Long>(dimensions, v) { it as Long }

                        Identifiers.Byte ->
                            deepCast<org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte>(
                                dimensions,
                                v) { org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte.valueOf(it as Byte) }

                        Identifiers.UInt16 ->
                            deepCast<org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort>(
                                dimensions,
                                v) { org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort.valueOf(it as Short) }

                        Identifiers.UInt32 ->
                            deepCast<UInteger>(dimensions, v) { UInteger.valueOf(it as Int) }

                        Identifiers.UInt64 ->
                            deepCast<org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.ULong>(
                                dimensions,
                                v) { org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.ULong.valueOf(it as Long) }

                        Identifiers.NodeId ->
                            deepCast<NodeId>(dimensions, v) { it as NodeId }

                        Identifiers.XmlElement ->
                            deepCast<XmlElement>(dimensions, v) { XmlElement(it.toString()) }

                        else -> {
                            v
                        }
                    }
                }
            } catch (e: Exception) {
                return null
            }
            return v
        }

        private inline fun <reified T> deepCast(dimensions: List<Int>, value: Any?, fn: (Any) -> T): Any? {

            return when (value) {
                null -> null
                is List<*> -> when (dimensions.size) {

                    1 -> Array(dimensions[0]) { i0 -> value[i0]?.let { fn(it) } }

                    2 -> Array(dimensions[0]) { i0 ->
                        Array(dimensions[1]) { i1 ->
                            val l1 = (value[i0]) as List<*>
                            l1[i1]?.let { fn(it) }
                        }
                    }

                    3 -> Array(dimensions[0]) { i0 ->
                        Array(dimensions[1]) { i1 ->
                            val l1 = (value[i0]) as List<*>
                            Array(dimensions[2]) { i2 ->
                                val l2 = (l1[i1]) as List<*>
                                l2[i2]?.let { fn(it) }
                            }
                        }
                    }

                    else -> {
                        value
                    }
                }

                else -> {
                    fn(value)
                }
            }
        }

        fun Any?.toVariant(dataTypeIdentifier: NodeId?, dimensions: List<Int>?): Variant = Variant(convert(this, dataTypeIdentifier, dimensions))
    }

}