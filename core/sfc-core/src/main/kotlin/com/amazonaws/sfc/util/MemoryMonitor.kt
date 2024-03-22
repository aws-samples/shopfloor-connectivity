package com.amazonaws.sfc.util

import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.log.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.commons.math3.stat.regression.SimpleRegression
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MemoryMonitor(
    private val interval: Duration = 1.toDuration(DurationUnit.MINUTES),
    private val trendSamplesRecent: Int = 10,
    private val trendSamples: Int = 60,
    scope: CoroutineScope,
    private val logger: Logger
) {

    private val className = this::class.java.name

    private var memoryUsage = mutableListOf<Long>()
    private var maxMemory = asMB(Runtime.getRuntime().maxMemory())

    private var recentSamplesCount = 0

    private val log = logger.getCtxLoggers(className, "monitor")
    private val monitor = scope.launch {
        while (isActive) {
            delay(interval)
            val usedMemory = getUsedMemory()
            memoryUsage.add(usedMemory)

            recentSamplesCount += 1
            if (recentSamplesCount == trendSamplesRecent) {
                val recentSamples = memoryUsage.takeLast(trendSamplesRecent)
                val recentTrend = calculateTrend(recentSamples)
                val totalTrend = calculateTrend(memoryUsage)
                if (logger.level == LogLevel.TRACE) {
                    log.trace(
                        "Using ${asMB(usedMemory)} of available $maxMemory MB, " +
                                "over last ${memUsageStr(recentSamples, recentTrend)}" +
                                if (memoryUsage.size > trendSamplesRecent) ", over last ${memUsageStr(memoryUsage, totalTrend)}" else ""
                    )
                }
                if (memoryUsage.size > trendSamplesRecent && totalTrend > 0) {
                    log.warning(memoryTrendUpStr(totalTrend, recentTrend, usedMemory))
                    Runtime.getRuntime().gc()

                }
                recentSamplesCount = 0
                if (memoryUsage.size == trendSamples) {
                    memoryUsage = memoryUsage.drop(trendSamplesRecent).toMutableList()
                }
            } else {
                log.trace("Currently using ${asMB(usedMemory)} MB  of available  ${maxMemory} MB")
            }
        }
    }

    private fun memoryTrendUpStr(totalTrend: Double, recentTrend: Double, usedMemory: Long): String {
        val trendStr = "Memory usage is growing, trend over last ${interval * memoryUsage.size} is ${(totalTrend + 0.5).toInt()}"
        val recentStr = "over the last ${interval * trendSamplesRecent} the trend is ${(recentTrend + 0.5).toInt()}"
        val currentStr = "currently using ${asMB(usedMemory)} MB of available $maxMemory MB"
        return "$trendStr, $recentStr, $currentStr"
    }

    fun stop() {
        if (monitor.isActive) {
            monitor.cancel()
        }
    }

    private fun calculateTrend(data: List<Long>): Double {

        val regression = SimpleRegression()

        val doubles = data.map { it.toDouble() }.toDoubleArray()

        doubles.forEachIndexed { index, value ->
            regression.addData(index.toDouble(), value)
        }

        return regression.slope
    }

    private fun memUsageStr(samples: List<Long>, trend: Double): String {
        val memDelta = samples.last() - samples.first()
        val memDeltaStr = when {
            memDelta > 0 -> "${asMB(memDelta)}MB up"
            memDelta < 0 -> "${asMB(abs(memDelta))}MB down"
            else -> ""
        }
        val duration = interval * samples.size
        val trendAsInt = (trend + 0.5).toInt()
        val trendStr = when{
            trendAsInt < 0 -> "down"
            trendAsInt >0 ->"up"
            else -> "flat"
        }

        return "$duration: ${memDeltaStr}, trend is $trendStr} ($trendAsInt)"
    }


    companion object {
        fun asMB(bytes: Long): Long {
            return bytes / (1024 * 1024)
        }

        fun round(value: Double, places: Int): Double {
            var bd = java.math.BigDecimal(value)
            bd = bd.setScale(places, java.math.RoundingMode.HALF_UP)
            return bd.toDouble()
        }

        fun getUsedMemory() = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

        fun getUsedMemoryMB() = asMB(getUsedMemory())


    }
}