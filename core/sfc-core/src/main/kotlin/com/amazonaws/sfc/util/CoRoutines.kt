/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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
    get() = (this::class.java.name == "kotlinx.coroutines.JobCancellationException")

val Exception.isJobCancellationException: Boolean
    get() = (this::class.java.name == "kotlinx.coroutines.JobCancellationException")

