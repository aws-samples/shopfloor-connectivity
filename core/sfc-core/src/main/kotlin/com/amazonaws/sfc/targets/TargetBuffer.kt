// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.amazonaws.sfc.targets

import com.amazonaws.sfc.channels.channelSubmitEventHandler
import com.amazonaws.sfc.channels.submit
import com.amazonaws.sfc.config.TargetConfiguration
import com.amazonaws.sfc.data.TargetData
import com.amazonaws.sfc.log.Logger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.selects.SelectClause1
import kotlin.time.Duration

class TargetDataChannel(private val channelName: String, private val capacity: Int, private val timeout: Duration) {

    private val _channel = Channel<TargetData>(capacity)
    val channel : ReceiveChannel<TargetData> = _channel

    fun submit(targetData: TargetData, log: Logger.ContextLogger) {
        _channel.submit(targetData, timeout = timeout) { event ->
            channelSubmitEventHandler(
                event = event,
                channelName = channelName,
                tuningChannelSizeName = TargetConfiguration.CONFIG_TARGET_CHANNEL_BUFFER_SIZE,
                currentChannelSize = capacity,
                tuningChannelTimeoutName = TargetConfiguration.CONFIG_TARGET_CHANNEL_BUFFER_TIMEOUT,
                log = log
            )
        }
    }

    fun close() {
        try {
            _channel.close()
        }catch (_ : Exception) {
            // ignore
        }
    }

    val onReceive: SelectClause1<TargetData> = _channel.onReceive

    suspend fun receive() : TargetData
    {
        return _channel.receive()
    }

    companion object{
        fun create(targetConfiguration: TargetConfiguration, channelName: String): TargetDataChannel {
            return TargetDataChannel(channelName, targetConfiguration.targetChannelSize, targetConfiguration.targetChannelChannelTimeout)
        }
    }

}
