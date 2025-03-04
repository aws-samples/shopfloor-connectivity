// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.data

import com.amazonaws.sfc.aggregations.AggregationConfiguration
import com.amazonaws.sfc.config.ElementNamesConfiguration
import com.amazonaws.sfc.data.JsonHelper.Companion.fromJsonExtended
import com.amazonaws.sfc.log.Logger
import org.apache.velocity.Template
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.runtime.resource.loader.StringResourceLoader
import org.apache.velocity.runtime.resource.util.StringResourceRepository
import org.apache.velocity.tools.generic.*
import java.io.File
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Applies transformation using a velocity transformation specification
 * @property templateFile File The file containing the velocity transformation template
 * @property logger Logger Logger for output
 */
class OutputTransformation(private var templateFile: File, private val logger: Logger) {

    private val className = this::class.java.simpleName

    // Velocity engine instance
    private val engine: VelocityEngine by lazy {
        val e = VelocityEngine()
        e.setProperty(Velocity.RESOURCE_LOADERS, "string")
        e.addProperty("resource.loader.string.class", StringResourceLoader::class.java.name)
        e.addProperty("resource.loader.string.repository.static", "false")
        e.init()
        e
    }

    // Velocity template
    private val template: Template? by lazy {
        try {
            val templateLines = Files.readAllLines(Paths.get(templateFile.absolutePath)).joinToString(separator = "\n")
            val repo = engine.getApplicationAttribute(StringResourceLoader.REPOSITORY_NAME_DEFAULT) as StringResourceRepository
            repo.putStringResource(templateFile.name, templateLines)
            engine.getTemplate(templateFile.name)
        } catch (e: Throwable) {
            val errorLogger = logger.getCtxErrorLog(className, "template")
            errorLogger("Error creating velocity template from file template file ${templateFile.absolutePath}, ${e.message}")
            null
        }
    }


    /**
     * Applies transformation using template
     * @param targetData TargetData Data to transform
     * @param elementNames ElementNamesConfiguration Configurable element names used for context parameters
     * @return String?
     */
    fun transform(targetData: TargetData, elementNames: ElementNamesConfiguration, epochTimestamp: Boolean): String? {

        if (template == null) {
            return null
        }

        var templateData = fromJsonExtended(targetData.toJson(elementNames, false), Map::class.java)

        val timestampName = elementNames.timestamp


        if (epochTimestamp) {
            templateData = addEpochTimestamps(templateData, targetData, elementNames)
        }

        // writer for template output
        val sw = StringWriter()

        // create context to pass to template
        val context = VelocityContext()

        return try {

            // Add schedule name
            context.put(elementNames.schedule, templateData[elementNames.schedule])
            // Add data
            context.put(elementNames.sources, templateData[elementNames.sources])
            // Add metadata
            context.put(elementNames.metadata, templateData[elementNames.metadata])
            // Add timestamp
            context.put(timestampName, templateData[timestampName])
            // Add serial
            context.put(elementNames.serial, templateData[elementNames.serial])


            // Add tab (as tabs can not be used directly in templates)
            context.put("tab", "\t")

            // Velocity tools
            context.put("collection", CollectionTool())
            context.put("math", MathTool())
            context.put("context", ContextTool())
            context.put("number", NumberTool())
            context.put("date", DateTool())

            template?.merge(context, sw)
            sw.toString()
        } catch (ex: Exception) {
            val logErrorEx = logger.getCtxErrorLogEx(className, "transform")
            logErrorEx("Error transforming data with template ${templateFile.name}", ex)
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun addEpochTimestamps(
        data: Map<*, *>,
        targetData: TargetData,
        elementNames: ElementNamesConfiguration): Map<Any, Any> {

        val log = logger.getCtxLoggers(className, "addEpochTimestamps")

        val timestampName = elementNames.timestamp
        val timestampEpocSec = "${timestampName}$EPOCH_SEC"
        val timestampEpocNanoSec = "${timestampName}$EPOCH_NANO_OFFSET"

        fun aggregations(sourceName : String, channelName : String,value: Map<*, *>, channelData: MutableMap<String, Any>) {
            // for every aggregated value
            value.forEach { aggregationName, aggregatedValue ->
                val aggregatedOutputData = aggregatedValue as ChannelOutputData
                if (aggregatedOutputData.timestamp != null) {
                    // does the aggregation have a timestamp
                    val aggregatedData = ((channelData[elementNames.value] as MutableMap<String, Any>)[aggregationName] as MutableMap<String, Any>)
                    log.trace("Adding epoch timestamps for source \"$sourceName\", channel \"$channelName\", aggregation \"$aggregationName\"")
                    aggregatedData[timestampEpocSec] = aggregatedOutputData.timestamp.epochSecond
                    aggregatedData[timestampEpocNanoSec] = aggregatedOutputData.timestamp.nano
                }
            }
        }

        fun valuesAggregation(sourceName : String, channelName : String, value: Map<*, *>, channelData: MutableMap<String, Any>) {
            // Get the "values" aggregation output
            var first = true
            val valuesAggregation = (value as MutableMap<String, Any>?)?.get(AggregationConfiguration.VALUES)
            if (valuesAggregation != null) {
                // For every value in the "values" aggregation
                ((valuesAggregation as ChannelOutputData).value as List<ChannelOutputData>).forEach { valueInAggregation ->
                    if (valueInAggregation.timestamp != null) {
                        // Does the value have a timestamp?
                        val aggregationsOutput = channelData[elementNames.value] as MutableMap<String, Any>
                        val aggregationsValuesOutput = (aggregationsOutput[AggregationConfiguration.VALUES] as MutableMap<String, Any>?)?.get(elementNames.value)
                        if (first) {
                            first = false
                            log.trace("Adding epoch timestamps for source \"$sourceName\", channel \"$channelName\", aggregation \"${AggregationConfiguration.VALUES}\"")
                        }
                        (aggregationsValuesOutput as List<MutableMap<String, Any>>).forEach { aggregationsValuesItem ->
                            aggregationsValuesItem[timestampEpocSec] = valueInAggregation.timestamp.epochSecond
                            aggregationsValuesItem[timestampEpocNanoSec] = valueInAggregation.timestamp.nano
                        }
                    }
                }
            }
        }

        return try {

            val outputData = (data.toMutableMap() as Map<String, Any>).toMutableMap()

            log.trace("Adding epoch timestamps for message with serial ${targetData.serial}")
            outputData[timestampEpocSec] = targetData.timestamp.epochSecond
            outputData[timestampEpocNanoSec] = targetData.timestamp.nano

            // Timestamps at source level
            targetData.sources.forEach { sourceName, sourceValue ->
                val sourceData = ((outputData[elementNames.sources] as MutableMap<String, MutableMap<String, Any>>)[sourceName] as MutableMap<String, Any>)
                if (sourceValue.timestamp != null) {
                    log.trace("Adding epoch timestamps for source \"$sourceName\"")
                    sourceData[timestampEpocSec] = sourceValue.timestamp.epochSecond
                    sourceData[timestampEpocNanoSec] = sourceValue.timestamp.nano
                }

                // Timestamps at channel value level
                val sourceChannels = targetData.sources[sourceName]?.channels ?: emptyMap()

                sourceChannels.forEach { channelName, channelValue ->
                    val channelData = (sourceData[elementNames.values] as MutableMap<String, Any>)[channelName] as MutableMap<String, Any>
                    if (channelValue.timestamp != null) {
                        log.trace("Adding epoch timestamps for source \"$sourceName\", channel \"$channelName\"")
                        channelData[timestampEpocSec] = channelValue.timestamp.epochSecond
                        channelData[timestampEpocNanoSec] = channelValue.timestamp.nano
                    }

                    // Aggregated data
                    if (channelValue.value is Map<*, *>) {
                        // For aggregated a subset of aggregation outputs can have timestamps for the actual aggregated value
                        aggregations(sourceName, channelName, channelValue.value, channelData)
                        // A special case is the "values" aggregation which is a list of values which can have timestamps
                        valuesAggregation(sourceName, channelName, channelValue.value, channelData)
                    }
                }
            }
            outputData
        } catch (e : Exception) {
            log.error("Error setting epoch timestamps, $e")
            data
        } as Map<Any, Any>
    }



    companion object {
        private const val EPOCH_SEC = "_epoch_sec"
        private const val EPOCH_NANO_OFFSET = "_epoch_offset_nanosec"
    }


}