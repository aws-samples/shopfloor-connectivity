package com.amazonaws.sfc.awssitewiseedge

import com.amazonaws.sfc.data.JsonHelper
import software.amazon.awssdk.services.iotsitewise.model.Variant

object TqvVariant {
    fun fromValue(value: Any): Variant {
        val builder = Variant.builder()
        when (value) {
            is Boolean -> builder.booleanValue(value)
            is Byte -> builder.integerValue(value.toInt())
            is Short -> builder.integerValue(value.toInt())
            is Int -> builder.integerValue(value.toInt())
            is Long -> builder.integerValue(value.toInt())
            is UByte -> builder.integerValue(value.toInt())
            is UShort -> builder.integerValue(value.toInt())
            is UInt -> builder.integerValue(value.toInt())
            is ULong -> builder.integerValue(value.toInt())
            is Double -> builder.doubleValue(value.toDouble())
            is Float -> builder.doubleValue(value.toDouble())
            else -> builder.stringValue(stringValue(value))
        }
        return builder.build()
    }

    private fun stringValue(data: Any): String = when (data) {
        is Map<*, *> -> JsonHelper.gsonExtended().toJson(data)
        is ArrayList<*> -> data.joinToString(prefix = "[", postfix = "]", separator = ",") { stringValue(it) }
        else -> data.toString()
    }

}