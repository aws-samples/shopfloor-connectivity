package com.amazonaws.sfc.awssitewiseedge

import com.amazonaws.sfc.data.ChannelOutputData
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.eclipse.paho.client.mqttv3.MqttMessage
import software.amazon.awssdk.services.iotsitewise.model.AssetPropertyValue
import software.amazon.awssdk.services.iotsitewise.model.PutAssetPropertyValueEntry
import software.amazon.awssdk.services.iotsitewise.model.Quality
import software.amazon.awssdk.services.iotsitewise.model.TimeInNanos

class TqvMessage(
    private val alias: String
) {
    private val _tqvList: MutableList<AssetPropertyValue> = mutableListOf()
    private var _payload: String = ""
    private val _gson: Gson = GsonBuilder().create()

    fun add(channelData: ChannelOutputData) {
        val propertyValue =
            assetPropertyValue(channelData)
        _tqvList.add(propertyValue)
        _payload = jsonPayload()
    }

    val payloadSize
        get() = _payload.length

    val tqvCount
        get() = _tqvList.size

    val payload
        get() = _payload

    val topic
        get() = alias

    fun mqttPayload(): MqttMessage {
        val message = MqttMessage()
        message.payload = _payload.toByteArray()
        message.qos = 1
        return message
    }

    private fun jsonPayload(): String {
        val assetPropertyValue = PutAssetPropertyValueEntry.builder()
            .propertyAlias(alias)
            .propertyValues(_tqvList)
            .build()

        return _gson.toJson(assetPropertyValue)
    }

    private fun assetPropertyValue(channelData: ChannelOutputData): AssetPropertyValue {
        val timeInNanos = TimeInNanos.builder().timeInSeconds(channelData.timestamp!!.epochSecond)
            .offsetInNanos(channelData.timestamp!!.nano).build()
        val variant = TqvVariant.fromValue(value = channelData.value!!)

        return AssetPropertyValue.builder()
            .quality(Quality.GOOD)
            .timestamp(timeInNanos)
            .value(variant)
            .build()
    }
}