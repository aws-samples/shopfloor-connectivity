
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


@file:Suppress("unused")

package com.amazonaws.sfc.aggregations


import com.amazonaws.sfc.config.BaseConfiguration.Companion.WILD_CARD
import com.amazonaws.sfc.config.ChannelConfiguration.Companion.CHANNEL_SEPARATOR
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.data.DataTypes.asDoubleValue
import com.amazonaws.sfc.data.DataTypes.isNumeric
import com.amazonaws.sfc.data.DataTypes.safeAsList
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.transformations.Transformation
import com.amazonaws.sfc.transformations.invoke
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.Instant
import kotlin.math.sqrt
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions

// Annotation to mark aggregation functions
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class AggregationOutput(val name: String)

/**
 * Implement output data aggregation functions
 * @property aggregation AggregationConfiguration Configured aggregations
 * @property transformations Mapping<String, List<Operator>> Configured transformations for the aggregation output values
 * @property logger Logger Logger for output
 */
class Aggregator(private val aggregation: AggregationConfiguration, private val transformations: Map<String, Transformation>, val logger: Logger) {

    private val className = this::class.java.simpleName

    inner class AggregationBuffer {

        // Buffered timestamps
        val timestamps = mutableListOf<Instant>()

        // Buffered values
        val values = mutableListOf<ChannelReadValue>()

        override fun toString(): String {
            return "values=$values, timestamps=$timestamps)"
        }
    }

    // Store for values to be aggregated for each source
    private val aggregationBuffers = mutableMapOf<String, MutableMap<String, AggregationBuffer>>()

    // Adds a read result from a source channel to the internal store
    fun add(value: Map<String, SourceReadSuccess>): Int {

        var storedChannelValues: AggregationBuffer? = null

        // Loop through all sources
        for (sourceResult: Map.Entry<String, SourceReadResult> in value) {

            // Do not store failed reads
            if (sourceResult.value is SourceReadError) {
                continue
            }

            // Get the source entry
            val sourceID = sourceResult.key
            val sourceValues = aggregationBuffers.getOrPut(sourceID) {
                mutableMapOf()
            }

            // Loop through all channels in the source
            for (c: Map.Entry<String, ChannelReadValue> in (sourceResult.value as SourceReadSuccess).values) {
                val channelID = c.key
                // Get channel entry
                storedChannelValues = sourceValues.getOrPut(channelID) {
                    AggregationBuffer()
                }

                // store the values
                storedChannelValues.timestamps.add(sourceResult.value.timestamp)
                storedChannelValues.values.add(c.value)
            }
        }
        // return number of stores results
        return storedChannelValues?.timestamps?.count() ?: 0
    }


    // returns a sequence of values to aggregate by the aggregation function
    // as the values are stored in a list grouped as they were added, the actual values need to be processed
    // as values per channel the values need to be extracted as sets per source channel
    private fun aggregationSets(buffer: AggregationBuffer) = sequence {

        // Handle arrays of channel values
        if (buffer.values.any { it.isArrayValue }) {
            // create an array of values, it contains a value from each stored set for that channel

            val maxValuesPerRow = buffer.values.maxOf { (it.value as Iterable<*>).count() }
            for (x in 0 until maxValuesPerRow) {
                val set = mutableListOf<ChannelReadValue>()
                for (y in buffer.values.indices) {
                    val row = (buffer.values[y].value as Iterable<*>).toList()
                    if (x < row.size) {
                        if (row[x] != null) {
                            val value = (buffer.values[y].value as Iterable<*>).toList()[x]
                            val element = ChannelReadValue(value)
                            set.add(element)
                        }
                    } else break
                }
                yield(set.toTypedArray())
            }


            // Handle single channel values
        } else {
            // loop through stored sets
            yield(Array(buffer.values.size) { j ->
                (buffer.values[j])
            }.toList().toTypedArray())
        }
    }

    /**
     * Returns a numeric value stored in an Any typed variable to a double value
     * @receiver ChannelReadValue
     * @return Double
     */
    private fun ChannelReadValue.valueAsDouble(): Double =
        this.value?.let { asDoubleValue(it) } ?: 0.0


    /**
     *  Comparer for ChannelReadValue, handling all supported numeric typed values
     */
    private fun ChannelReadValue.numericCompare(b: ChannelReadValue): Int {
        if (this.value == null || b.value == null) throw IllegalArgumentException("Can not compare ChannelReadValues with a value of null")
        return DataTypes.numericCompare(this.value!!, b.value!!)
    }


    /**
     * Helper method to calculate the sum of a serie of values
     * @param values Array<ChannelReadValue> Array of numeric channel values
     * @return ChannelReadValue including the sum of all input value, without timestamp as it is an aggregation of multiple values with individual timestamps
     */
    private fun calcSum(values: Array<ChannelReadValue>): Double {

        if (values.isEmpty()) {
            return 0.0
        }

        // All math is done on doubles
        return if (values.size == 1) values[0].valueAsDouble() else values.fold(0.toDouble()) { sum, value -> sum + value.valueAsDouble() }

    }

    /**
     * Applies an aggregation method on a list of stored values for a channel
     * @param aggregationName String Name of the aggregation
     * @param buffer AggregationBuffer Buffered values to aggregate
     * @param aggregation Function1<Array<ChannelReadValue>, ChannelReadValue?> The aggregation function that is applied on the data
     * @return Any? Result of the aggregation
     */
    private fun applyNumericAggregation(aggregationName: String, buffer: AggregationBuffer, aggregation: (Array<ChannelReadValue>) -> Any?): Any? {

        // Nothing to process
        if (buffer.values.isEmpty()) {
            return null
        }

        // Single buffered value, just return that value
        if (buffer.values.size == 1) {
            return buffer.values[0].value
        }

        // Numeric values only
        if (!isNumeric(buffer.values[0].dataType)) {
            val logError = logger.getCtxErrorLog(className, "applyNumericAggregation")
            logError("Invalid data type ${buffer.values[0].dataType} for aggregated output $aggregationName, aggregation requires numeric datatype")
            return null
        }

        // Apply aggregation
        val aggregated = aggregationSets(buffer).map { values ->
            aggregation(values)
        }.toList()


        // As aggregationSeries always returns a sequence of values to aggregate, convert back to single value
        // if the channel value was only single value
        return if (aggregated.size == 1 && buffer.values.firstOrNull()?.isArrayValue == false) aggregated[0] else aggregated

    }

    /**
     * Implements values aggregation function
     * @param buffer AggregationBuffer aggregated values
     * @return ChannelReadValue List of all buffered values, including timestamps for each individual value.
     * The value of the returned result contains a list of all buffered ChannelReadValue values with individual timestamps if available.
     * Does not include timestamp for the returned value as a whole as each individual value has an optional timestamp.
     */
    @AggregationOutput(AggregationConfiguration.VALUES)
    internal fun values(buffer: AggregationBuffer): ChannelReadValue {

        return ChannelReadValue(buffer.values.mapIndexed { index, value ->
            ChannelReadValue(
                value.value,
                value.timestamp ?: buffer.timestamps[index]
            )
        }, null)

    }

    /**
     * Implements first aggregation function, returns the value and timestamp of first stored value
     * @param buffer AggregationBuffer Values to aggregate
     * @return ChannelReadValue First buffered value, including timestamp if available
     */
    @AggregationOutput(AggregationConfiguration.FIRST)
    internal fun first(buffer: AggregationBuffer): ChannelReadValue {
        return ChannelReadValue(buffer.values.first().value, buffer.values.first().timestamp ?: buffer.timestamps.first())
    }

    /**
     * Implements last aggregation function, returns the value and timestamp of last stored value
     * @param buffer AggregationBuffer Values to aggregate
     * @return ChannelReadValue Last buffered value, including timestamp if available
     */
    @AggregationOutput(AggregationConfiguration.LAST)
    internal fun last(buffer: AggregationBuffer): ChannelReadValue {
        return ChannelReadValue(buffer.values.last().value, buffer.values.last().timestamp ?: buffer.timestamps.last())
    }


    /**
     * Implements min aggregation function
     * @param buffer AggregationBuffer Values to aggregate
     * @return ChannelReadValue Minimum buffered value, no timestamp as multiple values with different individual timestamps could have this minimum value.
     */
    @AggregationOutput(AggregationConfiguration.MIN)
    internal fun min(buffer: AggregationBuffer): ChannelReadValue {

        val m = applyNumericAggregation(AggregationConfiguration.MIN, buffer) { values ->
            values.minWithOrNull { a, b ->
                val ab = a.numericCompare(b)
                ab
            }?.value
        }

        return ChannelReadValue(m, null)

    }

    /**
     * Implements max aggregation function
     * @param buffer AggregationBuffer Values to aggregate
     * @return ChannelReadValue Maximum buffered value, no timestamp as multiple values with different individual timestamps could have this maximum value.
     */
    @AggregationOutput(AggregationConfiguration.MAX)
    internal fun max(buffer: AggregationBuffer): ChannelReadValue {
        val m = applyNumericAggregation(AggregationConfiguration.MAX, buffer)
        { values ->
            values.maxWithOrNull { a, b ->
                a.numericCompare(b)
            }?.value
        }

        return ChannelReadValue(m, null)
    }

    /**
     * Implements sum aggregation function
     * @param buffer AggregationBuffer Values to aggregate
     * @return ChannelReadValue Value is the sum of all values, does not contain timestamp as it is calculated from multiple values that have different timestamps.
     */
    @AggregationOutput(AggregationConfiguration.SUM)
    internal fun sum(buffer: AggregationBuffer): ChannelReadValue {
        val sum = applyNumericAggregation(AggregationConfiguration.SUM, buffer) { values ->
            calcSum(values)
        }

        if (buffer.values.firstOrNull()?.dataType in notInts) {
            return ChannelReadValue(sum, null)
        }

        return ChannelReadValue(
            value =
            if (sum is Iterable<*>)
                (sum.toList().map { asDoubleValue(it)?.toLong() })
            else
                asDoubleValue(sum)?.toLong(),
            timestamp = null
        )

    }

    /**
     * Implements avg aggregation function
     * @param buffer AggregationBuffer Values to aggregate
     * @return ChannelReadValue Value is the average of all values, does not contain timestamp as it is calculated from multiple values that have different timestamps.
     */
    @AggregationOutput(AggregationConfiguration.AVERAGE)
    internal fun avg(buffer: AggregationBuffer): ChannelReadValue {
        val a = applyNumericAggregation(AggregationConfiguration.AVERAGE, buffer) { values ->

            if (values.isEmpty()) {
                return@applyNumericAggregation 0.0
            }

            return@applyNumericAggregation (calcSum(values) / values.size.toDouble())
        }
        return ChannelReadValue(a, null)
    }

    /**
     * Implements stddev aggregation function
     * @param buffer AggregationBuffer Values to aggregate
     * @return ChannelReadValue Value is the stddev of all values, does not contain timestamp as it is calculated from multiple values that have different timestamps.
     */
    @AggregationOutput(AggregationConfiguration.STDDEV)
    internal fun stddev(buffer: AggregationBuffer): ChannelReadValue {
        val s = applyNumericAggregation(AggregationConfiguration.STDDEV, buffer) { values ->

            // stddev for less than 2 values is 0
            if (values.size < 2) {
                return@applyNumericAggregation 0.0
            }

            val sum = calcSum(values)
            val avg = sum / values.size.toDouble()

            val sumDiff = Array(values.size) { i ->
                val diff = values[i].valueAsDouble() - avg
                diff * diff
            }.sumOf { it }

            return@applyNumericAggregation sqrt(sumDiff / values.size.toDouble())
        }

        return ChannelReadValue(s, null)
    }

    /**
     * Implements median aggregation function
     * @param buffer AggregationBuffer Values to aggregate
     * @return ChannelReadValue Value is the median of all values, does not contain timestamp as it is calculated from multiple values that have different timestamps.
     */
    @AggregationOutput(AggregationConfiguration.MEDIAN)
    internal fun median(buffer: AggregationBuffer): ChannelReadValue {

        val medianValue = applyNumericAggregation(AggregationConfiguration.MEDIAN, buffer) { values ->

            if (values.isEmpty()) {
                return@applyNumericAggregation 0
            }

            val srt = values.map {
                it.valueAsDouble()
            }.sortedBy { it }
            val size = srt.size
            return@applyNumericAggregation if (size % 2 == 1) (srt[size / 2]) else (srt[size / 2 - 1] + srt[size / 2]) / 2.0
        }

        return ChannelReadValue(medianValue, null)

    }

    /**
     * Implements mode aggregation function
     * @param buffer AggregationBuffer Values to aggregate
     * @return ChannelReadValue Value is the mode of all values and can include multiple values if multiple values in the set score even on the number of times occur in the set.
     * These do not include a timestamp as this value could occur multiple times in the buffer.
     * The result itself does not contain timestamp as its value contain values with individual timestamps.
     */
    @AggregationOutput(AggregationConfiguration.MODE)
    internal fun mode(buffer: AggregationBuffer): ChannelReadValue {

        // nothing to process
        if (buffer.values.isEmpty()) {
            return ChannelReadValue(emptyArray<Any>(), null)
        }

        if (buffer.values.size == 1) {
            val ts = buffer.values[0].timestamp
            return ChannelReadValue(buffer.values[0].value, ts ?: buffer.timestamps[0])
        }

        val aggregated = aggregationSets(buffer).map { values ->
            val countPerValue = values.groupBy { it.value }.values.map { it[0] to it.size }
            val maxCount = countPerValue.maxOfOrNull { it.second }
            countPerValue.filter { it.second == maxCount }.map {
                it.first.value
            }
        }.toList()

        return ChannelReadValue(
            value = if (aggregated.size == 1) aggregated[0] else aggregated,
            timestamp = null
        )
    }

    /**
     * Implements count aggregation function
     * @param buffer AggregationBuffer Values to aggregate
     * @return ChannelReadValue Value is the count of buffered values, it does not have a timestamp
     */
    @AggregationOutput(AggregationConfiguration.COUNT)
    internal fun count(buffer: AggregationBuffer): Any = ChannelReadValue(buffer.values.size, null)


    /**
     * Executes aggregation on all buffered values
     * @return MutableMap<String, Mapping<String, ChannelOutputData>>
     */
    suspend fun aggregate(): MutableMap<String, Map<String, ChannelOutputData>> = coroutineScope {

        val log = logger.getCtxLoggers(className, "aggregate")

        // Mapping -> Source -> Channel -> Aggregation -> Value
        val aggregatedOutput = mutableMapOf<String, Map<String, ChannelOutputData>>()

        // For every stored source
        for ((sourceID, channelReadValues) in aggregationBuffers + emptyMap()) {

            // For each channel in the source channel -> aggregation name -> Value
            val aggregatedChannelValuesForSource = mutableMapOf<String, MutableMap<String, ChannelOutputData>>()

            for ((channelID, channelValues) in channelReadValues) {
                val id = channelID.split(CHANNEL_SEPARATOR)[0]

                // Get names of all aggregation functions to apply for this channel
                val (appliedAggregationNames, match) = aggregationsToApply(sourceID, id, channelValues)
                if (appliedAggregationNames.isNotEmpty()) {
                    log.trace("Aggregation functions for source  \"$sourceID\", channel \"$channelID\" are ${appliedAggregationNames.joinToString { s -> "\"$s\"" }} for using \"$match\"")
                    log.trace("Aggregation input for for source \"$sourceID\" channel \"$channelID\" is $channelValues on thread ${Thread.currentThread().name}")
                } else {
                    log.trace("No aggregated output values for for source \"$id\" channel \"$channelID\"")
                }

                // Apply all output aggregations found for the channel in parallel
                appliedAggregationNames.map { aggregationName ->

                    // async invocation of aggregation method
                    aggregationName to async {
                        applyOutputAggregation(channelValues, aggregationName, sourceID, channelID)
                    }

                }.map { (aggregationName, aggregatedDeferredValue) ->

                    // set the results from the list of returned name/deferred pairs
                    val aggregated = aggregatedDeferredValue.await()

                    if (aggregated != null) {
                        val channelOutputData = aggregated.asChannelOutputData()
                        val channelData = aggregatedChannelValuesForSource[channelID]
                        if (channelData != null) {
                            channelData[aggregationName] = channelOutputData
                        } else {
                            aggregatedChannelValuesForSource[channelID] = mutableMapOf(aggregationName to channelOutputData)
                        }

                        log.trace("Aggregation \"${aggregationName}\" output for for source \"$sourceID\" channel \"$channelID\" is $channelOutputData")
                    } else {
                        log.error("Aggregation $aggregationName on values for source \"$sourceID\" channel \"$channelID\" returns a null value and are not stored aggregation output")
                    }
                }
            }

            // If there are aggregated channel values for a source ad these to the output
            if (aggregatedChannelValuesForSource.isNotEmpty()) {
                aggregatedOutput[sourceID] = aggregatedChannelValuesForSource.map { it.key to ChannelOutputData(it.value) }.toMap()
            }
        }

        // Clear all data that was aggregated
        aggregationBuffers.clear()
        return@coroutineScope aggregatedOutput
    }


    /**
     * Returns a ChannelReadValue as ChannelOutputData
     * @receiver ChannelReadValue Receiver
     * @return ChannelOutputData Value as ChannelOutputData, note tha the actual value can be an array of ChannelOutputData values
     */
    private fun ChannelReadValue.asChannelOutputData(): ChannelOutputData {

        if (this.isNestedArrayValue) {
            val l = (this.value as List<*>).map {
                (it as ChannelReadValue).asChannelOutputData()
            }
            return ChannelOutputData(l, timestamp)
        }

        return ChannelOutputData(this.value, this.timestamp)


    }

    /**
     * Apply an output aggregation on a buffered channel values
     * @param channelValues AggregationBuffer containing the buffered values for the channel
     * @param aggregationName String Name of the aggregation function
     * @param sourceID String The source of the channel
     * @param channelID String The ID of the channel
     * @return ChannelReadValue? Result of the aggregation
     */
    private fun applyOutputAggregation(
        channelValues: AggregationBuffer,
        aggregationName: String,
        sourceID: String,
        channelID: String
    ): ChannelReadValue? {

        val logError = logger.getCtxErrorLog(className, "applyOutputAggregation")

        // Lookup the function by its name that implements the aggregation
        val aggregationFunction = aggregationOutputFunctions[aggregationName.lowercase()]
        if (aggregationFunction == null) {
            logError("Aggregation \"$aggregationName\" is not implemented")
            return null
        }

        return try {
            // Apply the found aggregation function
            val aggregated = aggregationFunction.call(this, channelValues) as? ChannelReadValue?
            if (aggregated?.value != null) {
                // Apply transformations configured for the aggregated output
                applyTransformation(aggregated, sourceID, channelID, aggregationName, transformations)
            } else {
                null
            }
        } catch (e: Throwable) {
            logError("Error applying aggregation $aggregationName on values for source \"$sourceID\" channel \"$channelID\" with values ${channelValues}, $e")
            null
        }
    }

    /**
     * Applies output transformation on an aggregated value
     * @param target ChannelReadValue The value to transform
     * @param sourceID String The source of the value
     * @param channelID String The channel of the value
     * @param aggregationName String Name of the aggregation
     * @param transformations Mapping<String, List<Operator>> Configured transformations
     * @return ChannelReadValue?
     */
    private fun applyTransformation(
        target: ChannelReadValue,
        sourceID: String,
        channelID: String,
        aggregationName: String,
        transformations: Map<String, Transformation>
    ): ChannelReadValue? {

        fun applyTransformationOnArray(outputTransformation: Transformation, valueName: String) = safeAsList(target.value as Iterable<*>).mapIndexed { i, v ->

            val itemValue = outputTransformation.invoke(
                target = if (v is ChannelReadValue) v.value else v,
                valueName = "$valueName[$i]",
                throwsException = true,
                logger = logger
            )
            ChannelReadValue(value = itemValue, timestamp = if (v is ChannelReadValue) v.timestamp else null)
        }

        val log = logger.getCtxLoggers(className, "applyTransformation")

        // channel can be a combination of the configured channel ID plus the name of the actual channel for adapters that support wildcards
        // only use the first part, which is the configured id to look up the transformation that needs to be applied
        val id = channelID.split(CHANNEL_SEPARATOR)[0]
        val (outputTransformationID, match) = aggregation.aggregationTransformation(sourceID, id, aggregationName)
        if (outputTransformationID == null) {
            return target
        }

        log.trace("Applying transformation \"$outputTransformationID\" on output value \"$aggregationName\" for \"$sourceID\", channel \"$channelID\" using \"$match\" on thread ${Thread.currentThread().name}")

        val outputTransformation: Transformation? = transformations[outputTransformationID]
        if (outputTransformation != null) {
            return try {
                val valueName = "$channelID:$aggregationName"
                val value = if (target.isArrayValue)
                    applyTransformationOnArray(outputTransformation, valueName)
                else
                    outputTransformation.invoke(target.value, valueName = valueName, throwsException = true, logger = logger)

                ChannelReadValue(value, target.timestamp)

            } catch (e: Throwable) {
                log.error("Error applying transformation $outputTransformationID to aggregated output \"$aggregationName\" \"${target.value}\" (${if (target.value != null) target.value!!::class.java.name else ""}) for source \"$sourceID\", channel \"$channelID\", $e")
                null
            }
        }
        return target
    }


    /**
     * Returns the names of the aggregation function to apply for a channel
     * @param sourceID String Source of the value
     * @param channelID String Channel of the value
     * @param channelValues AggregationBuffer ChannelValues
     * @return Pair<Set<String>, String?> Mapping containing the names and transformations to apply
     */
    private fun aggregationsToApply(sourceID: String, channelID: String, channelValues: AggregationBuffer): Pair<Set<String>, String?> {

        if (channelValues.values.size == 0) {
            return emptySet<String>() to null
        }
        var (aggregationFunctionNames, match) = aggregation.aggregationOutputs(sourceID, channelID)

        if (aggregationFunctionNames.firstOrNull() == WILD_CARD) {
            aggregationFunctionNames = when {
                isNumeric(channelValues.values.firstOrNull()?.dataType) -> NUMERIC_AGGREGATIONS
                else -> NON_NUMERIC_AGGREGATIONS
            }
        }
        return aggregationFunctionNames to match
    }

    companion object {

        // Mapping of all aggregation member functions that can be applied
        private val aggregationOutputFunctions =
            Aggregator::class.functions.asSequence()
                .filter { it.findAnnotation<AggregationOutput>() != null }
                .map { it.findAnnotation<AggregationOutput>()?.name to it }
                .toMap()

        // Names of all aggregation function names
        val aggregationOutputFunctionsNames = aggregationOutputFunctions.keys.filterNotNull().map { it.lowercase() }

        // Aggregations that can be applied to numeric values
        val NUMERIC_AGGREGATIONS = setOf(
            AggregationConfiguration.AVERAGE,
            AggregationConfiguration.COUNT,
            AggregationConfiguration.MAX,
            AggregationConfiguration.MEDIAN,
            AggregationConfiguration.MIN,
            AggregationConfiguration.MODE,
            AggregationConfiguration.STDDEV,
            AggregationConfiguration.SUM,
            AggregationConfiguration.VALUES
        )


        // Aggregations that can be applied other values
        val NON_NUMERIC_AGGREGATIONS = setOf(
            AggregationConfiguration.FIRST,
            AggregationConfiguration.LAST,
            AggregationConfiguration.MODE,
            AggregationConfiguration.COUNT,
            AggregationConfiguration.VALUES
        )


        val notInts = listOf(Double::class, Float::class)


    }
}


