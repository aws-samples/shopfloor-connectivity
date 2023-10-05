/*

Copyright (c) 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved. 
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.opcua

import org.eclipse.milo.opcua.stack.core.serialization.SerializationContext
import org.eclipse.milo.opcua.stack.core.types.builtin.*
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger
import java.time.Instant
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility

typealias OpcuaUByte = org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte
typealias OpcuaUShort = org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort
typealias OpcuaULong = org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.ULong

class OpcuaDataTypesConverter(private val context: SerializationContext? = null) {

    fun asNativeValue(value: Variant): Any? {
        return internalAsNativeValue(value.value, context)
    }

    private val reflection = ReflectionDataCache()

    private fun internalAsNativeValue(value: Any?, context: SerializationContext?): Any? =

        if (value == null) {
            null
        } else
            when (value) {
                is Array<*> -> asNativeArrayValue(value)
                is Boolean -> value
                is Byte -> value
                is ByteString -> value.bytesOrEmpty().toList()
                is DateTime -> value.javaDate.toInstant()
                is Double -> value
                is ExpandedNodeId -> value.toParseableString()
                is ExtensionObject -> extensionObjectValue(value, context)
                is Float -> value
                is Int -> value
                is LocalizedText -> value.text
                is Long -> value
                is NodeId -> value.toParseableString()
                is QualifiedName -> value.toParseableString()
                is Short -> value
                is String -> value
                is UInteger -> value.toLong()
                is UUID -> value.toString()
                is XmlElement -> value.fragmentOrEmpty
                is OpcuaUByte -> value.toInt()
                is OpcuaULong -> value.toLong().toULong()
                is OpcuaUShort -> value.toInt()
                is Map<*, *> -> value.map {
                    internalAsNativeValue(it.key, context) to internalAsNativeValue(it.value, context)
                }.toMap()

                is Variant -> asNativeValue(value)
                else -> null
            }

    private fun asNativeArrayValue(values: Array<*>): Any? =

        if (values.isEmpty()) {
            emptyArray<Any>()
        } else
            when (values[0]) {
                is Array<*> -> values.map { asNativeArrayValue(it as Array<*>) }
                is Boolean -> values.toList()
                is Byte -> values.toList()
                is ByteString -> values.map { (it as ByteString).bytesOrEmpty().toList() }
                is DateTime -> values.map { (it as DateTime).javaDate.toInstant() }
                is Double -> values.toList()
                is ExpandedNodeId -> values.map { (it as ExpandedNodeId).toParseableString() }
                is ExtensionObject -> values.map { extensionObjectValue(it as ExtensionObject, context) }
                is Float -> values.toList()
                is Int -> values.toList()
                is LocalizedText -> values.map { (it as LocalizedText).text }
                is Long -> values.toList()
                is NodeId -> values.map { (it as NodeId).toParseableString() }
                is OpcuaUByte -> values.map { (it as OpcuaUByte).toInt() }
                is OpcuaULong -> values.map { (it as OpcuaULong).toLong().toULong() }
                is OpcuaUShort -> values.map { (it as OpcuaUShort).toInt() }
                is QualifiedName -> values.map { (it as QualifiedName).toParseableString() }
                is Short -> values.toList()
                is String -> values.toList()
                is UInteger -> values.map { (it as UInteger).toLong() }
                is UUID -> values.map { it.toString() }
                is XmlElement -> values.map { (it as XmlElement).fragmentOrEmpty }
                is Variant -> values.map { asNativeValue(it as Variant) }
                else -> null
            }

    private fun extensionObjectValue(ext: ExtensionObject, serializationContext: SerializationContext?): Any? {

        val decoded = if (serializationContext != null) ext.decodeOrNull(serializationContext) else null

        return if (decoded != null) {
            decodedAsMap(decoded, serializationContext)
        } else {
            when (ext.body) {
                is ByteString -> (ext.body as ByteString).bytesOrEmpty().toList()
                is XmlElement -> (ext.body as XmlElement).fragmentOrEmpty
                else -> null
            }
        }
    }

    private fun decodedAsMap(value: Any?, context: SerializationContext?): Map<String, Any?> {

        if (value == null) {
            return emptyMap()
        }

        @Suppress("UNCHECKED_CAST")
        val clazz = value::class as KClass<Any>
        val a = reflection.propertiesForClass(clazz).associate { property ->

            val propertyValue = getPropertyValue(property, clazz, value)

            property.name to when {
                isAnArray(propertyValue) -> (propertyValue as Array<*>).map { item -> decodedAsMap(item, context) }
                isStructuredValue(propertyValue) -> decodedAsMap(propertyValue, context)
                else -> internalAsNativeValue(propertyValue, context)
            }
        }
        return a
    }

    private fun getPropertyValue(property: KProperty1<Any, *>, clazz: KClass<Any>, value: Any): Any? {
        return if (property.visibility == KVisibility.PUBLIC) {
            property.get(value)
        } else {
            reflection.getterForProperty(clazz, property)?.call(value)
        }
    }


    companion object {

        fun isStructuredValue(value: Any?): Boolean {
            if (isSimpleValueType(value)) return false
            if (value is Array<*>) {
                return ((value.size > 0) && isStructuredValue(value[0]))
            }
            return true
        }

        private fun isSimpleValueType(value: Any?): Boolean {

            if (value == null) {
                return false
            }

            if (OPCUA_TYPES.contains(value::class)) {
                return true
            }
            return false
        }


        private val OPCUA_TYPES = listOf(
            Boolean::class,
            Byte::class,
            ByteString::class,
            DateTime::class,
            Double::class,
            ExpandedNodeId::class,
            ExtensionObject::class,
            Float::class,
            Instant::class,
            Int::class,
            LocalizedText::class,
            Long::class,
            Long::class,
            NodeId::class,
            OpcuaUByte::class,
            OpcuaULong::class,
            OpcuaUShort::class,
            QualifiedName::class,
            Short::class,
            String::class,
            UByte::class,
            UInt::class,
            UInteger::class,
            UShort::class,
            UUID::class
        )
    }

    private fun isAnArray(propertyValue: Any?) = propertyValue is Array<*>
}

