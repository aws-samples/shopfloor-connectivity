// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.opcuawritetarget


import com.amazonaws.sfc.config.ChannelConfiguration.Companion.CONFIG_TRANSFORMATION
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.opcuawritetarget.OpcuaDataTypes.Companion.asType
import org.eclipse.milo.opcua.stack.core.Identifiers
import org.eclipse.milo.opcua.stack.core.types.builtin.*
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.ULong
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort

enum class OpcuaDataType {

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
    REAL {
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

        val className = this::class.simpleName.toString()

        fun fromString(value: String): OpcuaDataType {
            val s = value.uppercase().trim().replace("_", "")
            return entries.find { it.name == s } ?: UNDEFINED
        }

        fun fromIdentifier(value: ExpandedNodeId): OpcuaDataType {
            return entries.find { it.identifier.identifier == value.identifier && it.identifier.namespaceIndex == value.namespaceIndex } ?: UNDEFINED
        }

        fun fromIdentifier(value: NodeId): OpcuaDataType {
            return entries.find { it.identifier.identifier == value.identifier && it.identifier.namespaceIndex == value.namespaceIndex } ?: UNDEFINED
        }

        private fun convert(value: Any?, dataTypeIdentifier: NodeId?, dimensions: List<Int>?, logger: Logger): Any? {
            var v = value
            try {
                if (v is List<*> && dimensions != null) {

                    v = when (dataTypeIdentifier) {
                        Identifiers.Boolean -> deepCast<Boolean>(dimensions, v) {asType(it, Identifiers.Boolean) as Boolean }
                        Identifiers.SByte -> deepCast<Byte>(dimensions, v) { asType(it, Identifiers.SByte) as Byte }
                        Identifiers.ByteString -> deepCast<ByteString>(dimensions, v) { asType(it, Identifiers.ByteString) as ByteString  }
                        Identifiers.String -> deepCast<String>(dimensions, v) { asType(it, Identifiers.String) as String }
                        Identifiers.Structure -> deepCast<String>(dimensions, v) { asType(it, Identifiers.Structure) as String}
                        Identifiers.DateTime -> deepCast<DateTime>(dimensions, v) { asType(it, Identifiers.DateTime) as DateTime }
                        Identifiers.Double -> deepCast<Double>(dimensions, v) { asType(it, Identifiers.Double) as Double }
                        Identifiers.ExpandedNodeId -> deepCast<ExpandedNodeId>(dimensions, v) { asType(it, Identifiers.ExpandedNodeId) as ExpandedNodeId }
                        Identifiers.Float -> deepCast<Float>(dimensions, v) { asType(it, Identifiers.Float) as Float }
                        Identifiers.Int16 -> deepCast<Short>(dimensions, v) { asType(it, Identifiers.Int16) as Short }
                        Identifiers.Int32 -> deepCast<Int>(dimensions, v) { asType(it, Identifiers.Int32) as Int }
                        Identifiers.Int64 -> deepCast<Long>(dimensions, v) { asType(it, Identifiers.Int64) as Long }
                        Identifiers.Byte -> deepCast<UByte>(dimensions, v) { UByte.valueOf(asType(it, Identifiers.Int16) as Short) }
                        Identifiers.UInt16 -> deepCast<UShort>(dimensions, v) { UShort.valueOf(asType(it, Identifiers.Int32) as Int) }
                        Identifiers.UInt32 -> deepCast<UInteger>(dimensions, v) { UInteger.valueOf(asType(it, Identifiers.Int64) as Long) }
                        Identifiers.UInt64 -> deepCast<ULong>(dimensions, v) { asType(it, Identifiers.Int64) as ULong }
                        Identifiers.NodeId -> deepCast<NodeId>(dimensions, v) { NodeId.parse(it.toString()) }
                        Identifiers.XmlElement -> deepCast<XmlElement>(dimensions, v) { asType(it, Identifiers.XmlElement) as XmlElement }
                        else -> v
                    }
                } else {
                    v = if (v != null) {
                        asType(v, dataTypeIdentifier!!)
                    } else v
                }
            } catch (_: Exception) {
                logger.getCtxErrorLog(
                    className,
                    "convert")("Error converting value $value:${value!!::class.simpleName} to OPCUA data type ${fromIdentifier(dataTypeIdentifier!!)}, set the $CONFIG_TRANSFORMATION property of the node configuration to convert to the required type")
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

        fun Any?.toVariant(dataTypeIdentifier: NodeId?, dimensions: List<Int>?, logger: Logger): Variant = Variant(convert(this, dataTypeIdentifier, dimensions, logger))
    }

}