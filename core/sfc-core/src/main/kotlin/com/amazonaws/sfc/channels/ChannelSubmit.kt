// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.channels


import com.amazonaws.sfc.log.Logger
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeoutException
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * Submit an element to a channel.
 * @param element the element to submit
 * @param timeout the timeout for the submission
 * @param handler handled for submit events
 *
 */
fun <T> Channel<T>.submit(
    element: T,
    timeout: Duration,
    handler: (channelEvent: ChannelEvent<T>) -> Unit = { e: ChannelEvent<T> -> defaultChannelSubmitHandler(e, timeout) }
) {
    try {
        // first try to send the element to the channel without blocking
        val channelResult = this.trySend(element)
        when {
            // channel not full, element is submitted
            channelResult.isSuccess -> handler(ChannelEventSubmitted(element))
            // channel was full
            channelResult.isFailure -> {
                try {
                    // try sending the element to the channel, blocking with a timeout
                    handler(ChannelEventBlocking(element))
                    runBlocking {
                        withTimeout(timeout) {
                            val time = measureTime {
                                this@submit.send(element)
                            }
                            // element was sent within timeout
                            handler(ChannelEventSubmittedBlocking(element, time))
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    // blocking send did timeout
                    handler(ChannelEventTimeout(element, timeout))
                }
            }
        }
    } catch (e: OutOfMemoryError) {
        // submission causes an out of memory error
        handler(ChannelEventTimeout(element, timeout))
    }
}
 fun <T> defaultChannelSubmitHandler(e: ChannelEvent<T>, timeout: Duration) {
    when (e) {
        // throw timeout exception if blocking send timed out
        is ChannelEventTimeout -> throw TimeoutException("Timeout submitting element to channel after $timeout")
        // throw out of memory if the submission caused an out of memory error
        is ChannelEventOutOfMemory -> throw OutOfMemoryError("Out of memory while submitting element to channel" + e.outOfMemoryError.toString())
        else -> Unit
    }
}

fun <T>channelSubmitEventHandler(
    event: ChannelEvent<T>,
    channelName: String,
    tuningChannelSizeName: String,
    currentChannelSize: Int,
    tuningChannelTimeoutName: String,
    log: Logger.ContextLogger,
) {
    when (event) {
        is ChannelEventBlocking -> log.warning("Sending data to $channelName is blocking, consider setting tuning parameter $tuningChannelSizeName to a higher value, current value is $currentChannelSize")
        is ChannelEventTimeout -> log.error("Sending data to $channelName timeout after ${event.timeout}, consider setting tuning parameter $tuningChannelTimeoutName to a longer value")
        is ChannelEventSubmittedBlocking -> log.warning("Sending date to $channelName  was blocking for ${event.duration}, consider setting tuning parameter $tuningChannelSizeName to a higher value, current value is $currentChannelSize")
        is ChannelEventOutOfMemory -> throw OutOfMemoryError("Out of memory while submitting element to $channelName, ${event.outOfMemoryError}, consider setting tuning parameter $tuningChannelSizeName to a lower value, current value is $currentChannelSize")
        else -> {}
    }
}
