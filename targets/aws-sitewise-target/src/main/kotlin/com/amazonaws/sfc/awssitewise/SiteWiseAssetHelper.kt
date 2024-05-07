package com.amazonaws.sfc.awssitewise

import com.amazonaws.sfc.awssitewise.config.AssetTimestamp
import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseAssetCreationConfiguration
import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseAssetCreationConfiguration.Companion.TEMPLATE_CHANNEL
import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseAssetCreationConfiguration.Companion.TEMPLATE_DATETIME
import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseAssetCreationConfiguration.Companion.TEMPLATE_PRE_POSTFIX
import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseAssetCreationConfiguration.Companion.TEMPLATE_SCHEDULE
import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseAssetCreationConfiguration.Companion.TEMPLATE_SOURCE
import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseAssetCreationConfiguration.Companion.TEMPLATE_TARGET
import com.amazonaws.sfc.data.ChannelOutputData
import com.amazonaws.sfc.data.SourceOutputData
import com.amazonaws.sfc.data.TargetData
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.system.DateTime.systemDateTime
import kotlinx.coroutines.delay
import software.amazon.awssdk.services.iotsitewise.model.*
import java.time.Instant

class SiteWiseAssetHelper(
    private val client: AwsSiteWiseClient,
    private val target: String,
    private val assetCreationConfiguration: AwsSiteWiseAssetCreationConfiguration,
    private val logger: Logger
) {

    private val className = this::class.java.name

    private val assetModelSummaries: List<AssetModelSummary>
        get() {
            val listAssetModelsPaginator = client.listAssetModelsPaginator(ListAssetModelsRequest.builder().build())
            return listAssetModelsPaginator.flatMap { listAssetModelsResponse: ListAssetModelsResponse ->
                listAssetModelsResponse.assetModelSummaries().filter { it.isAvailable }.map { it }
            }
        }

    private val assetModelDetailsById: MutableMap<String, DescribeAssetModelResponse> by lazy {
        val assetModelDetails = sequence {
            assetModelSummaries.map {
                val describeAssetModelRequest = DescribeAssetModelRequest.builder().assetModelId(it.id()).build()
                val describeAssetModeResponse = client.describeAssetModel(describeAssetModelRequest)
                if (describeAssetModeResponse.isAvailable) yield(it.id() to describeAssetModeResponse)
            }
        }.toMap().toMutableMap()

        logger.getCtxTraceLog(className, "assetModelDetailsById")("${assetModelDetails.size} asset models loaded")
        assetModelDetails
    }

    private val assetModelDetailsByName: MutableMap<String, DescribeAssetModelResponse> by lazy {
        assetModelDetailsById.map { it.value.assetModelName() to it.value }.toMap().toMutableMap()
    }


    private val assetSummaries: List<AssetSummary>
        get() {
            return assetModelSummaries.flatMap { model ->
                val listAssetsPaginator = client.listAssetsPaginator(ListAssetsRequest.builder().assetModelId(model.id()).build())
                listAssetsPaginator.flatMap<ListAssetsResponse, AssetSummary> { listAssetsResponse: ListAssetsResponse ->
                    listAssetsResponse.assetSummaries().filter { it.isAvailable }
                }
            }
        }

    private val assetDetailsById: MutableMap<String, DescribeAssetResponse> by lazy {
        val assetDetails = assetSummaries.associate {
            val describeAsseRequest: DescribeAssetRequest = DescribeAssetRequest.builder().assetId(it.id()).build()
            val describeAssetModelResponse: DescribeAssetResponse = client.describeAsset(describeAsseRequest)
            it.assetModelId() to describeAssetModelResponse
        }.toMutableMap()
        logger.getCtxTraceLog(className, "assetModelDetailsById")("${assetDetails.size} assets loaded")
        assetDetails
    }

    private val assetDetailsByName: MutableMap<String, DescribeAssetResponse> by lazy {
        assetDetailsById.map { it.value.assetName() to it.value }.toMap().toMutableMap()
    }

    private fun createAssetModelMeasurementPropertyDefinition(
        propertyName: String,
        channelName: String,
        channelData: ChannelOutputData
    ): AssetModelPropertyDefinition {

        val log = logger.getCtxLoggers(className, "createAssetModelMeasurementPropertyDefinition")

        val builder = AssetModelPropertyDefinition.builder()
            .name(propertyName)
            .dataType(propertyDataTypeForValue(channelData.value!!))
            .type(MEASUREMENT_TYPE)

        val unit: String? = channelData.metadata?.get(assetCreationConfiguration.assetPropertyMetadataUnitName)
        if (unit != null) builder.unit(unit)

        val assetPropertyDefinition: AssetModelPropertyDefinition = builder.build()
        log.info("Created asset property definition with name \"${propertyName}\" for channel \"${channelName}\", $assetPropertyDefinition")

        return assetPropertyDefinition
    }


    private suspend fun createAsset(name: String, description: String, assetModelId: String, tags: Map<String, String>? = emptyMap()): DescribeAssetResponse {

        val log = logger.getCtxLoggers(className, "createAsset")

        val assetModelDetail: DescribeAssetModelResponse = assetModelDetailsById[assetModelId] ?: throw Exception("Asset model $assetModelId not found")

        log.info("Creating asset \"$name\" using model \"${assetModelDetail.assetModelName()}\" ($assetModelId)")

        val createAssetRequest = buildCreateAssetRequest(name, description, assetModelId, tags)

        val createAssetResponse: CreateAssetResponse = client.createAsset(createAssetRequest)
        val describeAssetModelRequest: DescribeAssetRequest = DescribeAssetRequest.builder().assetId(createAssetResponse.assetId()).build()
        var describeAssetResponse: DescribeAssetResponse = client.describeAsset(describeAssetModelRequest)

        while (describeAssetResponse.isBusy) {
            delay(1000)
            describeAssetResponse = client.describeAsset(describeAssetModelRequest)
        }

        log.info("Created asset \"$name\" $describeAssetResponse")

        assetDetailsById[createAssetResponse.assetId()] = describeAssetResponse
        assetDetailsByName[name] = describeAssetResponse
        return describeAssetResponse
    }

    private fun buildCreateAssetRequest(
        name: String,
        description: String,
        assetModelId: String,
        tags: Map<String, String>?
    ): CreateAssetRequest {
        val builder = CreateAssetRequest.builder()
        builder
            .assetName(name)
            .assetDescription(description)
            .assetModelId(assetModelId)

        if (!tags.isNullOrEmpty()) {
            builder.tags(tags)
        }

        val createAssetRequest = builder.build()
        return createAssetRequest
    }


    private suspend fun getOrBuildAssetForSource(source: String, targetOutputData: TargetData): DescribeAssetResponse {

        val sourceOutputData: SourceOutputData? = targetOutputData.sources[source]

        val metadataForAsset = targetOutputData.metadata + (sourceOutputData?.metadata ?: emptyMap())
        val assetName = assetCreationConfiguration.renderAssetName(target, targetOutputData.schedule, source, metadataForAsset)

        var asset: DescribeAssetResponse? = assetDetailsByName[assetName]

        if (asset != null) {
            val assetModel: DescribeAssetModelResponse =
                assetModelDetailsById[asset.assetModelId()] ?: throw Exception("Asset model ${asset.assetModelId()}  for asset ${asset.assetId()} not found")
            val measurements = assetModel.measurementsMap
            val allValuesHaveMeasurementProperty = sourceOutputData?.channels?.keys?.all { measurements.containsKey(it) } ?: true
            if (allValuesHaveMeasurementProperty) return asset

            updateSourceAssetModelById(assetModel.assetModelId(), source, targetOutputData)

            val describeAssetResponse = client.describeAsset(DescribeAssetRequest.builder().assetId(asset.assetId()).build())
            asset =  describeAssetResponse
            assetDetailsById[asset.assetId()] = asset
            assetDetailsByName[asset.assetName()] = asset

            return asset

        }

        val assetModelName = assetCreationConfiguration.renderAssetModelName(target, targetOutputData.schedule, source, metadataForAsset)
        var assetModel: DescribeAssetModelResponse? = assetModelDetailsByName[assetModelName]
        if (assetModel == null) {
            assetModel = createAssetModelForSource(source, targetOutputData)
        }
        val assetDescription = assetCreationConfiguration.renderAssetDescription(target, targetOutputData.schedule, source, metadataForAsset)
        val assetTags = assetCreationConfiguration.renderAssetTags(target, targetOutputData.schedule, source, metadataForAsset)
        return createAsset(assetName, assetDescription, assetModel.assetModelId(), assetTags)
    }


    private fun measurementsMapByNameForAsset(asset: DescribeAssetResponse): Map<String, AssetProperty>? {
        val measurements: Map<String, AssetModelProperty> = assetModelDetailsById[asset.assetModelId()]?.measurementsMap ?: return null
        return asset.assetProperties().filter { measurements.containsKey(it.name()) }.associateBy { it.name() }
    }

    suspend fun assetAndPropertiesForSource(source: String, targetData: TargetData): Pair<String, Map<String, AssetProperty>> {

        val assetForSource = getOrBuildAssetForSource(source, targetData)
        val measurementPropertiesForAsset = measurementsMapByNameForAsset(assetForSource) ?: emptyMap()
        val sourceData: SourceOutputData = targetData.sources[source] ?: return assetForSource.assetId() to emptyMap()
        val sourceMetadata = targetData.metadata + (sourceData.metadata ?: emptyMap())

        return assetForSource.assetId() to (sequence {
            sourceData.channels.forEach { (channelName, channelData) ->
                val propertyMetadata = sourceMetadata + (channelData.metadata ?: emptyMap())
                val channelPropertyName =
                    assetCreationConfiguration.renderAssetPropertyName(target, targetData.schedule, source, channelName, propertyMetadata)
                val propertyForChannel = measurementPropertiesForAsset[channelPropertyName]
                if (propertyForChannel != null)
                    yield(channelName to propertyForChannel)
            }
        }.toMap())
    }

    private suspend fun updateSourceAssetModelById(assetModelId: String, source: String, targetOutputData: TargetData): DescribeAssetModelResponse {

        val log = logger.getCtxLoggers(className, "updateSourceAssetModelById")

        val assetModelDetails: DescribeAssetModelResponse = assetModelDetailsById[assetModelId] ?: throw Exception("Asset model $assetModelId not found")

        log.info("Updating asset model \"${assetModelDetailsById[assetModelId]?.assetModelName()}\" ($assetModelId) for source \"$source\"")

        val sourceOutputData = targetOutputData.sources[source]

        val assetModelProperties: MutableList<AssetModelProperty> = assetModelDetails.assetModelProperties().toMutableList()

        addMissingChannelProperties(assetModelId, sourceOutputData, targetOutputData, source, assetModelProperties)

        val assetModelUpdateRequest = buildUpdateAssetModelRequest(assetModelDetails, assetModelProperties)

        client.updateAssetModel(assetModelUpdateRequest)

        var describeAssetModelResponse: DescribeAssetModelResponse =
            client.describeAssetModel(DescribeAssetModelRequest.builder().assetModelId(assetModelId).build())

        while (describeAssetModelResponse.isBusy) {
            delay(1000)
            describeAssetModelResponse =
                client.describeAssetModel(DescribeAssetModelRequest.builder().assetModelId(assetModelId).build())
        }

        assetModelDetailsById[assetModelId] = describeAssetModelResponse
        assetModelDetailsByName[describeAssetModelResponse.assetModelName()] = describeAssetModelResponse

        return describeAssetModelResponse

    }

    private fun buildUpdateAssetModelRequest(
        assetModelDetails: DescribeAssetModelResponse,
        assetModelProperties: MutableList<AssetModelProperty>
    ): UpdateAssetModelRequest =

        UpdateAssetModelRequest.builder()
            .assetModelId(assetModelDetails.assetModelId())
            .assetModelName(assetModelDetails.assetModelName())
            .assetModelProperties(assetModelProperties)
            .assetModelDescription(assetModelDetails.assetModelDescription())
            .assetModelCompositeModels(assetModelDetails.assetModelCompositeModels())
            .assetModelHierarchies(assetModelDetails.assetModelHierarchies())
            .build()

    private fun addMissingChannelProperties(
        assetModelId: String,
        sourceOutputData: SourceOutputData?,
        targetOutputData: TargetData,
        source: String,
        assetModelProperties: MutableList<AssetModelProperty>
    ) {

        val log = logger.getCtxLoggers(className, "addMissingChannelProperties")
        val sourceMetadata = targetOutputData.metadata + (sourceOutputData?.metadata ?: emptyMap())

        sourceOutputData?.channels?.filter { it.value.value != null }?.forEach { (channelName, channelData) ->

            val propertyMetadata = (channelData.metadata ?: emptyMap()) + sourceMetadata
            val propertyNameForChannel =
                assetCreationConfiguration.renderAssetPropertyName(target, targetOutputData.schedule, source, channelName, propertyMetadata)
            if (assetModelProperties.find { it.name() == propertyNameForChannel } == null) {
                if (channelData.value == null) {
                    log.warning("Channel \"$channelName\" from source \"$source\" has no value and will be ignored")
                } else {
                    val newChannelProperty = createAssetModelMeasurementProperty(propertyNameForChannel, channelData)
                    if (newChannelProperty != null) {
                        assetModelProperties.add(newChannelProperty)
                        log.info("Adding channel measurement property \"$propertyNameForChannel\" $newChannelProperty for channel \"$channelName\" from source \"$source\" to model \"${assetModelDetailsById[assetModelId]?.assetModelName()}\" ($assetModelId)")
                    }
                }
            }
        }
    }

    private fun createAssetModelMeasurementProperty(propertyName: String, channelData: ChannelOutputData): AssetModelProperty? {
        if (channelData.value == null) return null

        val builder = AssetModelProperty.builder()
            .name(propertyName)
            .dataType(propertyDataTypeForValue(channelData.value!!))
            .type(PropertyType.builder().measurement(Measurement.builder().build()).build())

        val unit: String? = channelData.metadata?.get(assetCreationConfiguration.assetPropertyMetadataUnitName)
        if (unit != null) builder.unit(unit)

        return builder.build()
    }


    private suspend fun createAssetModelForSource(source: String, targetOutputData: TargetData): DescribeAssetModelResponse {

        val log = logger.getCtxLoggers(className, "createAssetModelForSource")

        val sourceOutputData: SourceOutputData? = targetOutputData.sources[source]

        val sourceMetadata = (targetOutputData.metadata) + (sourceOutputData?.metadata ?: emptyMap())
        val assetModelName = assetCreationConfiguration.renderAssetModelName(target, targetOutputData.schedule, source, sourceMetadata)

        val assetModelDescription =
            assetCreationConfiguration.renderAssetModelDescription(target, targetOutputData.schedule, source, sourceMetadata)

        log.info("Creating asset model \"$assetModelName\" for source \"$source\"")

        val measurementPropertiesDefinitions = sourceOutputData?.channels?.filter { it.value.value != null }?.map { (channelName, channelData) ->

            val metadata = sourceMetadata + (channelData.metadata ?: emptyMap())
            val propertyNameForChannel =
                assetCreationConfiguration.renderAssetPropertyName(target, targetOutputData.schedule, source, channelName, metadata)

            createAssetModelMeasurementPropertyDefinition(propertyNameForChannel, channelName, channelData)
        }

        val assetModelTags = assetCreationConfiguration.renderAssetModelTags(target, targetOutputData.schedule, source, sourceMetadata)

        val createAssetModelRequest = CreateAssetModelRequest.builder()
            .assetModelName(assetModelName)
            .assetModelDescription(assetModelDescription)
            .assetModelProperties((measurementPropertiesDefinitions ?: emptyList()).toMutableList())
            .tags(assetModelTags)
            .build()

        val creatAssetModelResponse = client.createAssetModel(createAssetModelRequest)

        var describeAssetModelResponse: DescribeAssetModelResponse =
            client.describeAssetModel(DescribeAssetModelRequest.builder().assetModelId(creatAssetModelResponse.assetModelId()).build())

        while (describeAssetModelResponse.isBusy) {
            delay(1000)
            describeAssetModelResponse =
                client.describeAssetModel(DescribeAssetModelRequest.builder().assetModelId(creatAssetModelResponse.assetModelId()).build())
        }

        log.info("Created asset model \"$assetModelName\" $describeAssetModelResponse")

        assetModelDetailsById[creatAssetModelResponse.assetModelId()] = describeAssetModelResponse
        assetModelDetailsByName[assetModelName] = describeAssetModelResponse
        return describeAssetModelResponse
    }

    fun getPropertyTimestamp(targetOutputData: TargetData, sourceData: SourceOutputData, channelData: ChannelOutputData): Instant {
        return when (assetCreationConfiguration.assetTimestamp) {
            AssetTimestamp.SYSTEM -> systemDateTime()
            AssetTimestamp.CHANNEL -> channelData.timestamp ?: sourceData.timestamp ?: targetOutputData.timestamp
            AssetTimestamp.SOURCE -> sourceData.timestamp ?: targetOutputData.timestamp
            AssetTimestamp.SCHEDULE -> targetOutputData.timestamp
        }
    }


    companion object {

        private val DescribeAssetModelResponse.measurements: List<AssetModelProperty>
            get() {
                return this.assetModelProperties().filter { it.type() == MEASUREMENT_TYPE }
            }

        private val DescribeAssetModelResponse.measurementsMap: Map<String, AssetModelProperty>
            get() {
                return this.measurements.associateBy {
                    it.name()
                }

            }

        val DescribeAssetModelResponse.isBusy
            get() = MODEL_BUSY_STATUS.contains(this.assetModelStatus())

        val DescribeAssetModelResponse.isAvailable
            get() = MODEL_AVAILABLE_STATUS.contains(this.assetModelStatus())

        val AssetModelSummary.isAvailable
            get() = MODEL_AVAILABLE_STATUS.contains(this.status())

        val DescribeAssetResponse.isBusy
            get() = ASSET_BUSY_STATUS.contains(this.assetStatus())

        val AssetSummary.isAvailable
            get() = ASSET_AVAILABLE_STATUS.contains(this.status())


        fun propertyDataTypeForValue(value: Any): PropertyDataType {
            val siteWiseDataType = SiteWiseDataType.fromValue(value)
            return when (siteWiseDataType) {
                SiteWiseDataType.STRING -> PropertyDataType.STRING
                SiteWiseDataType.INTEGER -> PropertyDataType.INTEGER
                SiteWiseDataType.DOUBLE -> PropertyDataType.DOUBLE
                SiteWiseDataType.BOOLEAN -> PropertyDataType.BOOLEAN
                SiteWiseDataType.UNSPECIFIED -> PropertyDataType.STRING
            }
        }


        private val MODEL_UPDATING_STATUS: AssetModelStatus = AssetModelStatus.builder().state(AssetModelState.UPDATING).build()
        private val MODEL_PROPAGATING_STATUS: AssetModelStatus = AssetModelStatus.builder().state(AssetModelState.PROPAGATING).build()
        private val MODEL_CREATING_STATUS: AssetModelStatus = AssetModelStatus.builder().state(AssetModelState.CREATING).build()
        private val MODEL_ACTIVE_STATUS: AssetModelStatus = AssetModelStatus.builder().state(AssetModelState.ACTIVE).build()


        private val MODEL_BUSY_STATUS: Set<AssetModelStatus> = setOf(MODEL_PROPAGATING_STATUS, MODEL_CREATING_STATUS, MODEL_UPDATING_STATUS)
        private val MODEL_AVAILABLE_STATUS: Set<AssetModelStatus> =
            setOf(MODEL_ACTIVE_STATUS, MODEL_CREATING_STATUS, MODEL_PROPAGATING_STATUS, MODEL_UPDATING_STATUS)

        private val ASSET_STATE_CREATING: AssetStatus = AssetStatus.builder().state(AssetState.CREATING).build()
        private val ASSET_STATE_ACTIVE: AssetStatus = AssetStatus.builder().state(AssetState.ACTIVE).build()
        private val ASSET_STATE_UPDATING: AssetStatus = AssetStatus.builder().state(AssetState.UPDATING).build()

        private val ASSET_BUSY_STATUS: Set<AssetStatus> = setOf(ASSET_STATE_CREATING, ASSET_STATE_UPDATING)
        private val ASSET_AVAILABLE_STATUS: Set<AssetStatus> = setOf(ASSET_STATE_ACTIVE, ASSET_STATE_CREATING, ASSET_STATE_UPDATING)

        private val MEASUREMENT: Measurement = Measurement.builder().build()
        private val MEASUREMENT_TYPE: PropertyType = PropertyType.builder().measurement(MEASUREMENT).build()

    }

}

private fun renderTemplate(
    template: String,
    target: String,
    schedule: String,
    source: String,
    maxLength: Int,
    metadata: Map<String, String>?,
    useDateTime: Boolean = true
): String {
    var s = template
        .replace(TEMPLATE_SCHEDULE, schedule.replace(TEMPLATE_PRE_POSTFIX, ""))
        .replace(TEMPLATE_SOURCE, source.replace(TEMPLATE_PRE_POSTFIX, ""))
        .replace(TEMPLATE_TARGET, target.replace(TEMPLATE_PRE_POSTFIX, ""))
    if (useDateTime) {
        s = s.replace(TEMPLATE_DATETIME, systemDateTime().toString())
    }
    if (metadata != null) {
        for (entry in metadata) {
            s = s.replace("${TEMPLATE_PRE_POSTFIX}entry.key$TEMPLATE_PRE_POSTFIX", entry.value.replace(TEMPLATE_PRE_POSTFIX, ""))
        }
    }
    return s.trim().substring(0, minOf(s.length, maxLength))
}

private fun renderTemplate(
    template: String,
    target: String,
    schedule: String,
    source: String,
    channel: String,
    maxLength: Int,
    metadata: Map<String, String>?,
    useDateTime: Boolean = true
): String {
    return renderTemplate(template, target, schedule, source, maxLength, metadata, useDateTime)
        .replace(TEMPLATE_CHANNEL, channel.replace(TEMPLATE_PRE_POSTFIX, ""))
}

fun AwsSiteWiseAssetCreationConfiguration.renderAssetName(target: String, schedule: String, source: String, metadata: Map<String, String>?): String =
    renderTemplate(assetName, target, schedule, source, 256, metadata)

fun AwsSiteWiseAssetCreationConfiguration.renderAssetDescription(target: String, schedule: String, source: String, metadata: Map<String, String>?): String =
    renderTemplate(assetDescription, target, schedule, source, 22048, metadata, true)

fun AwsSiteWiseAssetCreationConfiguration.renderAssetModelName(target: String, schedule: String, source: String, metadata: Map<String, String>?): String =
    renderTemplate(assetModelName, target, schedule, source, 256, metadata)

fun AwsSiteWiseAssetCreationConfiguration.renderAssetModelDescription(
    target: String,
    schedule: String,
    source: String,
    metadata: Map<String, String>?
): String =
    renderTemplate(assetModelDescription, target, schedule, source, 2048, metadata, true)

fun AwsSiteWiseAssetCreationConfiguration.renderAssetPropertyName(
    target: String,
    schedule: String,
    source: String,
    channel: String,
    metadata: Map<String, String>?
): String =
    renderTemplate(assetPropertyName, target, schedule, source, channel, 256, metadata)

private fun renderTags(
    tagsTemplate: Map<String, String>?,
    target: String,
    schedule: String,
    source: String,
    metadata: Map<String, String>?
): Map<String, String> =
    if (tagsTemplate.isNullOrEmpty())
        emptyMap()
    else {
        val tags = sequence {
            tagsTemplate.forEach { (key, value) ->

                var tagValue = value
                    .replace(TEMPLATE_SCHEDULE, schedule.replace(TEMPLATE_PRE_POSTFIX, ""))
                    .replace(TEMPLATE_SOURCE, source.replace(TEMPLATE_PRE_POSTFIX, ""))
                    .replace(TEMPLATE_TARGET, target.replace(TEMPLATE_PRE_POSTFIX, ""))
                    .replace(TEMPLATE_DATETIME, systemDateTime().toString())

                for (entry in metadata ?: emptyMap()) {
                    tagValue = tagValue.replace("${TEMPLATE_PRE_POSTFIX}entry.key$TEMPLATE_PRE_POSTFIX", entry.value.replace(TEMPLATE_PRE_POSTFIX, ""))
                }

                tagValue = tagValue.trim().substring(0, minOf(tagValue.length, 256))

                if (tagValue.isNotEmpty()) this.yield(key.substring(0, minOf(key.length, 128)) to tagValue)

            }
        }.toList()
        tags.subList(0, minOf(tags.size, 50)).toMap()

    }


private fun AwsSiteWiseAssetCreationConfiguration.renderAssetModelTags(
    target: String,
    schedule: String,
    source: String,
    metadata: Map<String, String>?
): Map<String, String> = renderTags(this.assetModelTags, target, schedule, source, metadata)

private fun AwsSiteWiseAssetCreationConfiguration.renderAssetTags(
    target: String,
    schedule: String,
    source: String,
    metadata: Map<String, String>?
): Map<String, String> = renderTags(this.assetTags, target, schedule, source, metadata)

