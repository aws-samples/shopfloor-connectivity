package com.amazonaws.sfc.mqtt

enum class MqttConnectionProtocol {

    TCP {

        override val protocolStr = "tcp://"
    },
    SSL {
        override val protocolStr = "ssl://"
    };

    abstract val protocolStr: String
    val protocolPrefix
        get() = protocolStr

    companion object {

        val validValues = MqttConnectionProtocol.entries.map { it.protocolPrefix }
        fun fromAddress(address: String): MqttConnectionProtocol? {
            return MqttConnectionProtocol.entries.find { address.startsWith(it.protocolStr) }
        }
    }
}
