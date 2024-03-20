package com.amazonaws.sfc.util

import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.Exception

fun Throwable.toStringEx(): String {
    return exceptionToStringEx(this)
}

fun exceptionToStringEx(t : Throwable): String {
    val buffer = ByteArrayOutputStream()
    val printWriter = PrintWriter(buffer)
    t.printStackTrace(printWriter)
    printWriter.flush()
    return String(buffer.toByteArray())
}

fun Exception.toStringEx(): String = exceptionToStringEx(this)

