
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.util

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun buildContext(name: String, scope: CoroutineScope? = null): CoroutineContext {
    if (scope != null) {
        val context = scope.coroutineContext
        val contextName = context[CoroutineName]?.name
        return context + CoroutineName(if (contextName != null) "$contextName:$name" else name)
    }
    return CoroutineName(name)
}

fun buildScope(name: String, dispatcher: CoroutineDispatcher = Dispatchers.Default): CoroutineScope =
    CoroutineScope(dispatcher) + CoroutineName(name)

fun CoroutineScope.launch(name: String, context: CoroutineContext = EmptyCoroutineContext,
                          start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit): Job =
    this.launch(context = context + buildContext(name, this), block = block, start = start)


val Throwable.isJobCancellationException: Boolean
    get() = (this::class.java.name.contains("JobCancellationException") || (this is CancellationException))

val Exception.isJobCancellationException: Boolean
    get() = (this::class.java.name.contains("JobCancellationException") || (this is CancellationException))

