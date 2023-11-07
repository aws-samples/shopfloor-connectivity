package com.amazonaws.sfc.pccc.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class PcccConnectPathConfiguration {

    @SerializedName(CONFIG_BACKPLANE)
    private var _backplane = DEFAULT_BACKPLANE
    val backplane: Int
        get() = _backplane

    @SerializedName(CONFIG_SLOT)
    private var _slot = DEFAULT_SLOT
    val slot: Int
        get() = _slot

    companion object {
        const val CONFIG_BACKPLANE = "Backplane"
        const val CONFIG_SLOT = "Slot"

        const val DEFAULT_BACKPLANE = 1
        const val DEFAULT_SLOT = 0

        private val default = PcccConnectPathConfiguration()

        fun create(
            backplane: Int = default._backplane,
            slot: Int = default._slot
        ): PcccConnectPathConfiguration {

            val instance = PcccConnectPathConfiguration()

            with(instance) {
                _backplane = backplane
                _slot = slot

            }
            return instance
        }
    }
}