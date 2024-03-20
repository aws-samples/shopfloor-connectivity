// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.amazonaws.sfc.channels

import kotlin.time.Duration

sealed class ChannelEvent<T>(val element: T)
class ChannelEventSubmitted<T>(element: T) : ChannelEvent<T>(element)
class ChannelEventBlocking<T>(element: T) : ChannelEvent<T>(element)
class ChannelEventSubmittedBlocking<T>(element: T, val duration: Duration) : ChannelEvent<T>(element)
class ChannelEventTimeout<T>(element: T, val timeout: Duration) : ChannelEvent<T>(element)
class ChannelEventOutOfMemory<T>(element: T, val outOfMemoryError: OutOfMemoryError) : ChannelEvent<T>(element)
