package com.amazonaws.sfc.config

import com.google.gson.annotations.SerializedName
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@ConfigurationClass
class TuningConfiguration : Validate {
    @SerializedName(CONFIG_MAX_CONCURRENT_SOURCE_READERS)
    protected var _maxConcurrentSourceReaders = 5

    val maxConcurrentSourceReaders: Int
        get() = _maxConcurrentSourceReaders

    @SerializedName(CONFIG_MAX_SOURCES_READ_TIMEOUT)
    protected var _allSourcesReadTimeout = 60000

    val allSourcesReadTimeout: Duration
        get() = _allSourcesReadTimeout.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_METRICS_CHANNEL_SIZE_PER_METRICS_PROVIDER)
    protected val _channelSizePerMetricsProvider = 100
    val channelSizePerMetricsProvider
        get() = _channelSizePerMetricsProvider

    @SerializedName(CONFIG_METRICS_CHANNEL_TIMEOUT)
    protected val _metricsChannelTimeout = 10000
    val metricsChannelTimeout
        get() = _metricsChannelTimeout.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_SCHEDULE_READER_RESULTS_CHANNEL_SIZE)
    protected val _scheduleReaderResultsChannelSize : Int = 10000
    val scheduleReaderResultsChannelSize
        get() = _scheduleReaderResultsChannelSize

    @SerializedName(CONFIG_SCHEDULE_READER_RESULTS_CHANNEL_TIMEOUT)
    protected val _scheduleReaderResultsChannelTimeout : Long = 5000
    val scheduleReaderResultsChannelTimeout
        get() = _scheduleReaderResultsChannelTimeout.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_AGGREGATOR_CHANNEL_SIZE)
    protected val _aggregatorChannelSize : Int = 1000
    val aggregatorChannelSize
        get() = _aggregatorChannelSize

    @SerializedName(CONFIG_AGGREGATOR_CHANNEL_TIMEOUT)
    protected val _aggregatorChannelTimeout : Long = 1000
    val aggregatorChannelTimeout
        get() = _aggregatorChannelTimeout.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_WRITER_INPUT_CHANNEL_SIZE)
    protected val _writerInputChannelSize : Int = 10000
    val writerInputChannelSize
        get() = _writerInputChannelSize

    @SerializedName(CONFIG_WRITER_INPUT_CHANNEL_TIMEOUT)
    protected val _writerInputChannelTimeout : Long = 1000
    val writerInputChannelTimeout
        get() = _writerInputChannelTimeout.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_TARGET_RESULTS_CHANNEL_SIZE)
    protected val _targetResultsChannelSize : Int = 1000
    val targetResultsChannelSize
        get() = _targetResultsChannelSize

    @SerializedName(CONFIG_TARGET_RESULTS_CHANNEL_TIMEOUT)
    protected val _targetResultsChannelTimeout : Long = 5000
    val targetResultsChannelTimeout
        get() = _targetResultsChannelTimeout.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_TARGET_RESUBMIT_CHANNEL_SIZE)
    protected val _targetResubmitChannelSize : Int = 100
    val targetResubmitChannelSize
        get() = _targetResubmitChannelSize

    @SerializedName(CONFIG_TARGET_RESUBMIT_CHANNEL_TIMEOUT)
    protected val _targetResubmitsChannelTimeout : Long = 10000L
    val targetResubmitsChannelTimeout
        get() = _targetResubmitsChannelTimeout.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_TARGET_FORWARDING_CHANNEL_SIZE)
    protected val _targetForwardingChannelSize : Int = 1000
    val targetForwardingChannelSize
        get() = _targetForwardingChannelSize

    @SerializedName(CONFIG_TARGET_FORWARDING_CHANNEL_TIMEOUT)
    protected val _targetForwardingChannelTimeout : Long= 1000
    val targetForwardingChannelTimeout
        get() = _targetForwardingChannelTimeout.toDuration(DurationUnit.MILLISECONDS)


    companion object {

        const val CONFIG_SCHEDULE_READER_RESULTS_CHANNEL_SIZE = "ScheduleReaderResultsChannelSize"
        const val CONFIG_SCHEDULE_READER_RESULTS_CHANNEL_TIMEOUT = "ScheduleReaderResultsChannelTimeout"

        const val CONFIG_AGGREGATOR_CHANNEL_SIZE = "AggregatorChannelSize"
        const val CONFIG_AGGREGATOR_CHANNEL_TIMEOUT = "AggregatorChannelTimeout"

        const val CONFIG_WRITER_INPUT_CHANNEL_SIZE = "WriterInputChannelSize"
        const val CONFIG_WRITER_INPUT_CHANNEL_TIMEOUT = "WriterInputChannelTimeout"

        const val CONFIG_MAX_CONCURRENT_SOURCE_READERS = "MaxConcurrentSourceReaders"
        const val CONFIG_MAX_SOURCES_READ_TIMEOUT = "AllSourcesReadTimeout"

        const val CONFIG_METRICS_CHANNEL_SIZE_PER_METRICS_PROVIDER = "ChannelSizePerMetricsProvider"
        const val CONFIG_METRICS_CHANNEL_TIMEOUT = "MetricsChannelTimeout"

        const val CONFIG_TARGET_RESULTS_CHANNEL_SIZE = "TargetResultsChannelSize"
        const val CONFIG_TARGET_RESULTS_CHANNEL_TIMEOUT = "TargetResultsChannelTimeout"

        const val CONFIG_TARGET_FORWARDING_CHANNEL_SIZE = "TargetForwardingChannelSize"
        const val CONFIG_TARGET_FORWARDING_CHANNEL_TIMEOUT = "TargetForwardingChannelTimeout"

        const val CONFIG_TARGET_RESUBMIT_CHANNEL_SIZE = "TargetResubmitChannelSize"
        const val CONFIG_TARGET_RESUBMIT_CHANNEL_TIMEOUT = "TargetResubmitChannelTimeout"

    }

    override fun validate() {
    }

    override var validated: Boolean
        get() = true
        set(_) {}
}