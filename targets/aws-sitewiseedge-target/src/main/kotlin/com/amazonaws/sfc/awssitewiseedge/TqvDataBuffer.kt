package com.amazonaws.sfc.awssitewiseedge

import com.amazonaws.sfc.awssitewiseedge.config.SiteWiseEdgeTargetConfiguration.Companion.TEMPLATE_CHANNEL
import com.amazonaws.sfc.awssitewiseedge.config.SiteWiseEdgeTargetConfiguration.Companion.TEMPLATE_PRE_POSTFIX
import com.amazonaws.sfc.awssitewiseedge.config.SiteWiseEdgeTargetConfiguration.Companion.TEMPLATE_SOURCE
import com.amazonaws.sfc.awssitewiseedge.config.SiteWiseEdgeTargetConfiguration.Companion.TEMPLATE_TARGET
import com.amazonaws.sfc.data.TargetData
import com.amazonaws.sfc.log.Logger

class TqvDataBuffer(
    private val targetID: String,
    private val topicTemplate: String,
    private val logger: Logger
) {
    private val className = this::class.java.simpleName
    private val _tqvMessageMap: MutableMap<String, TqvMessage> = mutableMapOf()

    val payloadSize
        get() = _tqvMessageMap
            .map { entry -> entry.value.payloadSize }
            .sum()

    val size
        get() = _tqvMessageMap.size

    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
    }

    fun add(targetData: TargetData) {
        targetData.sources.forEach { (sourceName, sourceData) ->
            try {
                sourceData.channels.filter { it.value.value != null }.forEach { (channelName, channelData) ->
                    val alias = renderAliasName(targetID, sourceName, channelName)
                    val tqvMessage = _tqvMessageMap.getOrPut(alias) { TqvMessage(alias) }
                    tqvMessage.add(channelData)
                }
            } catch (e: Exception) {
                val log = logger.getCtxLoggers(className, "add")
                log.error("Error getting asset for source $sourceName, $e")
            }
        }
    }

    fun pop(tqvCount: Int = 0, messageSize: Int = 0): List<TqvMessage> {
        val messages = _tqvMessageMap.map { entry -> entry.value }
            .filter { tqvMessage -> tqvMessage.payloadSize >= messageSize || tqvMessage.tqvCount >= tqvCount }

        messages.forEach { message -> _tqvMessageMap.remove(message.topic) }

        return messages
    }

    private fun renderAliasName(target: String, source: String, channel: String): String {
        val maxLength = 1000

        val s = topicTemplate.replace(TEMPLATE_SOURCE, source.replace(TEMPLATE_PRE_POSTFIX, ""))
            .replace(TEMPLATE_TARGET, target.replace(TEMPLATE_PRE_POSTFIX, ""))
            .replace(TEMPLATE_CHANNEL, channel.replace(TEMPLATE_PRE_POSTFIX, ""))

        return s.trim().substring(0, minOf(s.length, maxLength))
    }
}