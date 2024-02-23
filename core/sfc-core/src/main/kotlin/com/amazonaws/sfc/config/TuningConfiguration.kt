package com.amazonaws.sfc.config

import com.google.gson.annotations.SerializedName

@ConfigurationClass
open class TuningConfiguration {
    @SerializedName(CONFIG_MAX_CONCURRENT_SOURCE_READERS)
    protected var _maxConcurrentSourceReaders = 5

    val maxConcurrentSourceReaders: Int
        get() = _maxConcurrentSourceReaders

    companion object {
        const val CONFIG_MAX_CONCURRENT_SOURCE_READERS = "MaxConcurrentSourceReaders"
    }
}