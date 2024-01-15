package com.amazonaws.sfc.util

fun Byte.asHexString(): String =
    "%02X".format(this)

fun ByteArray.asHexString(): String =
    this.joinToString(separator = "") { it.asHexString() }

fun Int.asHexString(): String =
    arrayOf(
        this shr 24, this shr 16, this shr 8, this
    ).map { (it and 0xFF).toByte() }.toByteArray().asHexString()

fun Short.asHexString(): String =
    arrayOf(
       this.toInt() shr 8, this
    ).map { (it.toInt() and 0xFF).toByte() }.toByteArray().asHexString()