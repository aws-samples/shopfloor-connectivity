// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.awssitewise

import com.amazonaws.sfc.awssitewise.config.AssetTimestamp
import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseAssetCreationConfiguration
import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseAssetCreationConfiguration.Companion.TEMPLATE_CHANNEL
import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseAssetCreationConfiguration.Companion.TEMPLATE_DATETIME
import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseAssetCreationConfiguration.Companion.TEMPLATE_PRE_POSTFIX
import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseAssetCreationConfiguration.Companion.TEMPLATE_SCHEDULE
import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseAssetCreationConfiguration.Companion.TEMPLATE_SOURCE
import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseAssetCreationConfiguration.Companion.TEMPLATE_TARGET
import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseAssetCreationConfiguration.Companion.TEMPLATE_UUID
import com.amazonaws.sfc.data.ChannelOutputData
import com.amazonaws.sfc.data.SourceOutputData
import com.amazonaws.sfc.data.TargetData
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.system.DateTime.systemDateTime
import kotlinx.coroutines.delay
import software.amazon.awssdk.services.iotsitewise.model.*
import java.time.Instant
import java.util.*

class SiteWiseAssetHelper(private val client: AwsSiteWiseClient,
                          private val target: String,
                          private val assetCreationConfiguration: AwsSiteWiseAssetCreationConfiguration,
                          val createAssets: Boolean,
                          private val logger: Logger) {

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

    val assetDetailsById: MutableMap<String, DescribeAssetResponse> by lazy {
        val assetDetails = assetSummaries.associate {
            val describeAssetRequest: DescribeAssetRequest = DescribeAssetRequest.builder().assetId(it.id()).build()
            val describeAssetResponse: DescribeAssetResponse = client.describeAsset(describeAssetRequest)
            describeAssetResponse.assetId() to describeAssetResponse
        }.toMutableMap()
        logger.getCtxTraceLog(className, "assetModelDetailsById")("${assetDetails.size} assets loaded")
        assetDetails
    }

    val assetDetailsByName: MutableMap<String, DescribeAssetResponse> by lazy {
        assetDetailsById.map { it.value.assetName() to it.value }.toMap().toMutableMap()
    }

    val assetDetailsByExternalID: MutableMap<String, DescribeAssetResponse> by lazy {
        assetDetailsById.map { it.value.assetExternalId() to it.value }.toMap().toMutableMap()
    }

    private fun createAssetModelMeasurementPropertyDefinition(propertyName: String, externalID: String?, channelName: String, channelData: ChannelOutputData): AssetModelPropertyDefinition {

        val log = logger.getCtxLoggers(className, "createAssetModelMeasurementPropertyDefinition")

        val builder = AssetModelPropertyDefinition.builder()
            .name(propertyName)
            .dataType(propertyDataTypeForValue(channelData.value!!))
            .type(MEASUREMENT_TYPE)

        val unit: String? = getPropertyUnit(channelData.metadata)
        if (unit != null) builder.unit(unit)

        if (externalID != null) builder.externalId(externalID)

        val assetPropertyDefinition: AssetModelPropertyDefinition = builder.build()
        log.info("Created asset property definition with name \"${propertyName}\" for channel \"${channelName}\", $assetPropertyDefinition")

        return assetPropertyDefinition
    }


    private suspend fun createAsset(name: String,
                                    description: String,
                                    assetExternalID: String?,
                                    assetModelId: String,
                                    source: String,
                                    targetData: TargetData,
                                    tags: Map<String, String>? = emptyMap()): DescribeAssetResponse {

        val log = logger.getCtxLoggers(className, "createAsset")

        val assetModelDetail: DescribeAssetModelResponse = assetModelDetailsById[assetModelId] ?: throw Exception("Asset model $assetModelId not found")

        log.info("Creating asset \"$name\" using model \"${assetModelDetail.assetModelName()}\" ($assetModelId)")

        val createAssetRequest = buildCreateAssetRequest(
            name = name, description = description, assetModelId = assetModelId, externalID = assetExternalID, tags = tags)

        val createAssetResponse: CreateAssetResponse = client.createAsset(createAssetRequest)

        if (assetCreationConfiguration.assetPropertyAlias != null) {
            setAssetPropertiesAlias(source, createAssetResponse.assetId(), targetData)
        }

        val describeAssetResponse: DescribeAssetResponse = describeAssetWhenReady(createAssetResponse.assetId())

        log.info("Created asset \"$name\" $describeAssetResponse")

        assetDetailsById[createAssetResponse.assetId()] = describeAssetResponse
        assetDetailsByName[name] = describeAssetResponse
        return describeAssetResponse
    }

    private suspend fun describeAssetWhenReady(assetID: String): DescribeAssetResponse {
        val describeAssetRequest: DescribeAssetRequest = DescribeAssetRequest.builder().assetId(assetID).build()
        var describeAssetResponse: DescribeAssetResponse = client.describeAsset(describeAssetRequest)

        while (describeAssetResponse.isBusy) {
            delay(1000)
            describeAssetResponse = client.describeAsset(describeAssetRequest)
        }
        return describeAssetResponse
    }

    private suspend fun setAssetPropertiesAlias(source: String, assetID: String, targetData: TargetData) {

        val log = logger.getCtxLoggers(className, "setAssetPropertiesAlias")

        targetData.sources[source]?.channels?.keys?.forEach { channelName ->

            val alias = assetCreationConfiguration.renderAssetPropertyAlias(
                target = target, source = source, channel = channelName, assetID = assetID, targetData = targetData)

            if (alias != null) {
                val propertyName = assetCreationConfiguration.renderAssetPropertyName(
                    target = target, source = source, channel = channelName, targetData = targetData)

                // reload latest copy of stable asset as otherwise setting the alias may fail
                val asset =describeAssetWhenReady(assetID)
                assetDetailsById[assetID] = asset
                val property = asset.assetProperties().find { it.name() == propertyName }
                if (property != null) {
                    log.info("Setting alias \"$alias\" for property $propertyName) \"$alias\" of asset ${asset.assetName()}(${asset.assetId()} for channel \"$channelName\"")
                    try {
                        setAssetPropertyAlias(asset, property, alias)
                    } catch (e: Exception) {
                        log.error("Error setting alias \"$alias\" for property \"${propertyName}\"  of asset ${asset.assetName()} (${asset.assetId()}) for channel \"$channelName\", ${e.message}")
                    }
                } else{
                    log.error("Property \"$propertyName\" not found for asset ${asset.assetName()} (${asset.assetId()}) for channel \"$channelName\"")
                }
            }
        }
    }

    private fun setAssetPropertyAlias(asset: DescribeAssetResponse, property: AssetProperty, alias: String) {

        val updateAssetPropertyRequestBuilder = UpdateAssetPropertyRequest.builder().assetId(asset.assetId()).propertyId(property.id()).propertyAlias(alias)

        val request = updateAssetPropertyRequestBuilder.build()
        client.updateAssetProperty(request)

    }

    private fun buildCreateAssetRequest(name: String, description: String, assetModelId: String, externalID: String?, tags: Map<String, String>?): CreateAssetRequest {

        val builder = CreateAssetRequest.builder()
        builder.assetName(name).assetDescription(description).assetModelId(assetModelId)

        if (!tags.isNullOrEmpty()) {
            builder.tags(tags)
        }

        if (!externalID.isNullOrEmpty()) {
            builder.assetExternalId(externalID)
        }

        val createAssetRequest = builder.build()
        return createAssetRequest
    }


    private suspend fun getOrBuildAssetForSource(source: String, targetData: TargetData): DescribeAssetResponse {

        val log = logger.getCtxLoggers(className, "getOrBuildAssetForSource")

        val sourceOutputData: SourceOutputData? = targetData.sources[source]

        val assetName = assetCreationConfiguration.renderAssetName(target, source, targetData)

        var asset: DescribeAssetResponse? = assetDetailsByName[assetName]

        if (asset != null) {
            val assetModel = assetModelDetailsById[asset.assetModelId()] ?: throw Exception("Asset model ${asset.assetModelId()}  for asset ${asset.assetId()} not found")

            val measurements = assetModel.measurementsMap

            val allValuesHaveMeasurementProperty = sourceOutputData?.channels?.keys?.all { channelName ->

                val channelPropertyName = assetCreationConfiguration.renderAssetPropertyName(target, source, channelName, targetData)
                measurements.containsKey(channelPropertyName)

            } ?: true

            if (allValuesHaveMeasurementProperty) return asset

            val addedChannelProperties = updateSourceAssetModelById(assetModel.assetModelId(), source, targetData)

            // get updated asset with properties added
            asset = describeAssetWhenReady(asset.assetId())
            val assetID = asset.assetId()

            addedChannelProperties.forEach { (channelName, assetModelProperty) ->

                val alias = assetCreationConfiguration.renderAssetPropertyAlias(
                    target = target, source = source, channel = channelName, assetID = assetID, targetData = targetData)

                if (alias != null) {

                    val assetProperty = asset!!.assetProperties().find { it.name() == assetModelProperty.name() }
                    if (assetProperty != null) {
                        log.info("Setting alias \"$alias\" for assetModelProperty ${assetModelProperty.name()} (${assetModelProperty.id()}) \"$alias\" of asset ${asset?.assetName()} (${asset?.assetId()} for channel \"$channelName\"")
                        try {
                            setAssetPropertyAlias(asset!!, assetProperty, alias)
                        } catch (e: Exception) {
                            log.error("Error setting alias \"$alias\" for assetModelProperty \${assetModelProperty.name()} (${assetModelProperty.id()}) of asset ${asset?.assetName()} (${asset?.assetId()}) for channel \"$channelName\", ${e.message}")
                        }
                    }
                }
            }

            // read asset with new properties
            asset = describeAssetWhenReady(asset.assetId()) //Store updated asset
            assetDetailsById[asset.assetId()] = asset
            assetDetailsByName[asset.assetName()] = asset

            return asset

        }

        val assetModelName = assetCreationConfiguration.renderAssetModelName(target, source, targetData)
        var assetModel: DescribeAssetModelResponse? = assetModelDetailsByName[assetModelName]
        if (assetModel == null) {
            assetModel = createAssetModelForSource(source, targetData)
        }
        val assetDescription = assetCreationConfiguration.renderAssetDescription(target, source, targetData)

        var assetExternalID = assetCreationConfiguration.renderAssetExternalID(target, source, targetData)

        if (assetExternalID != null) {
            val assetForExtId = assetSummaries.find { it.externalId().toString().lowercase() == assetExternalID.toString().lowercase() }
            if (assetForExtId != null) {
                log.error("Asset external ID \"$assetExternalID\" for source \"$source\" in target \"$target\" is already in use by asset  \"${assetForExtId.name()}\"")
                assetExternalID = null
            }
        }

        val assetTags = assetCreationConfiguration.renderAssetTags(target, source, targetData)
        return createAsset(
            name = assetName,
            description = assetDescription,
            assetExternalID = assetExternalID,
            assetModelId = assetModel.assetModelId(),
            source = source,
            targetData = targetData,
            tags = assetTags)
    }


    fun measurementsMapByNameForAsset(asset: DescribeAssetResponse): Map<String, AssetProperty>? {
        val measurements: Map<String, AssetModelProperty> = assetModelDetailsById[asset.assetModelId()]?.measurementsMap ?: return null
        return asset.assetProperties().filter { measurements.containsKey(it.name()) }.associateBy { it.name() }
    }

    suspend fun assetAndPropertiesForSource(source: String, targetData: TargetData): Pair<String, Map<String, AssetProperty>> {

        val assetForSource = getOrBuildAssetForSource(source, targetData)
        val measurementPropertiesForAsset = measurementsMapByNameForAsset(assetForSource) ?: emptyMap()
        val sourceData: SourceOutputData = targetData.sources[source] ?: return assetForSource.assetId() to emptyMap()

        return assetForSource.assetId() to (sequence {
            sourceData.channels.keys.forEach { channelName ->
                val channelPropertyName = assetCreationConfiguration.renderAssetPropertyName(target, source, channelName, targetData)
                val propertyForChannel = measurementPropertiesForAsset[channelPropertyName]
                if (propertyForChannel != null) yield(channelName to propertyForChannel)
            }
        }.toMap())
    }

    private suspend fun updateSourceAssetModelById(assetModelId: String, source: String, targetData: TargetData): List<Pair<String, AssetModelProperty>> {

        val log = logger.getCtxLoggers(className, "updateSourceAssetModelById")

        val assetModelDetails: DescribeAssetModelResponse = assetModelDetailsById[assetModelId] ?: throw Exception("Asset model $assetModelId not found")

        log.info("Updating asset model \"${assetModelDetailsById[assetModelId]?.assetModelName()}\" ($assetModelId) for source \"$source\"")

        val assetModelProperties: MutableList<AssetModelProperty> = assetModelDetails.assetModelProperties().toMutableList()

        val addedChannels = addMissingChannelProperties(assetModelId, targetData, source, assetModelProperties) ?: emptyList()

        val externalID = assetCreationConfiguration.renderAssetModelExternalID(target, source, targetData)

        val assetModelUpdateRequest = buildUpdateAssetModelRequest(assetModelDetails, assetModelProperties, externalID)

        client.updateAssetModel(assetModelUpdateRequest)

        val describeAssetModelResponse: DescribeAssetModelResponse = describeAssetModelWhenReady(assetModelId)

        assetModelDetailsById[assetModelId] = describeAssetModelResponse
        assetModelDetailsByName[describeAssetModelResponse.assetModelName()] = describeAssetModelResponse

        return addedChannels
    }

    private suspend fun describeAssetModelWhenReady(assetModelId: String): DescribeAssetModelResponse {
        var describeAssetModelResponse: DescribeAssetModelResponse = client.describeAssetModel(DescribeAssetModelRequest.builder().assetModelId(assetModelId).build())

        while (describeAssetModelResponse.isBusy) {
            delay(1000)
            describeAssetModelResponse = client.describeAssetModel(DescribeAssetModelRequest.builder().assetModelId(assetModelId).build())
        }
        return describeAssetModelResponse
    }

    private fun buildUpdateAssetModelRequest(assetModelDetails: DescribeAssetModelResponse, assetModelProperties: MutableList<AssetModelProperty>, externalID: String?): UpdateAssetModelRequest {

        val builder = UpdateAssetModelRequest.builder().assetModelId(assetModelDetails.assetModelId()).assetModelName(assetModelDetails.assetModelName()).assetModelProperties(assetModelProperties)
            .assetModelDescription(assetModelDetails.assetModelDescription()).assetModelCompositeModels(assetModelDetails.assetModelCompositeModels())
            .assetModelHierarchies(assetModelDetails.assetModelHierarchies())

        if (!externalID.isNullOrEmpty()) {
            builder.assetModelExternalId(externalID)
        }


        return builder.build()
    }

    private fun addMissingChannelProperties(assetModelId: String,
                                            targetData: TargetData,
                                            source: String,
                                            assetModelProperties: MutableList<AssetModelProperty>): List<Pair<String, AssetModelProperty>>? {

        val log = logger.getCtxLoggers(className, "addMissingChannelProperties")
        val sourceOutputData: SourceOutputData = targetData.sources[source] ?: return null

        return sequence {
            sourceOutputData.channels.filter { it.value.value != null }.forEach { (channelName, channelData) ->

                val propertyNameForChannel = assetCreationConfiguration.renderAssetPropertyName(target, source, channelName, targetData)

                if (assetModelProperties.find { it.name() == propertyNameForChannel } == null) {
                    if (channelData.value == null) {
                        log.warning("Channel \"$channelName\" from source \"$source\" has no value and will be ignored")
                    } else {
                        val propertyExternalID = assetCreationConfiguration.renderAssetModelPropertyExternalID(
                            target, source, channelName, targetData)

                        val newChannelProperty = createAssetModelMeasurementProperty(propertyNameForChannel, propertyExternalID, channelData)
                        if (newChannelProperty != null) {
                            assetModelProperties.add(newChannelProperty)
                            log.info("Adding channel measurement property \"$propertyNameForChannel\" $newChannelProperty for channel \"$channelName\" from source \"$source\" to model \"${assetModelDetailsById[assetModelId]?.assetModelName()}\" ($assetModelId)")
                            yield(channelName to newChannelProperty)
                        }
                    }
                }
            }
        }.toList()
    }


    private fun createAssetModelMeasurementProperty(propertyName: String, propertyExternalID: String?, channelData: ChannelOutputData): AssetModelProperty? {
        if (channelData.value == null) return null

        val builder =
            AssetModelProperty.builder().name(propertyName).dataType(propertyDataTypeForValue(channelData.value!!)).type(PropertyType.builder().measurement(Measurement.builder().build()).build())

        if (propertyExternalID != null) builder.externalId(propertyExternalID)

        val unit: String? = getPropertyUnit(channelData.metadata)
        if (unit != null) builder.unit(unit)

        return builder.build()
    }

    private fun getPropertyUnit(metadata: Map<String, String>?) = (metadata ?: emptyMap())[assetCreationConfiguration.assetPropertyMetadataUnitName]


    private suspend fun createAssetModelForSource(source: String, targetData: TargetData): DescribeAssetModelResponse {

        val log = logger.getCtxLoggers(className, "createAssetModelForSource")

        val sourceOutputData: SourceOutputData? = targetData.sources[source]

        val assetModelName = assetCreationConfiguration.renderAssetModelName(target, source, targetData)

        var assetModelExternalID = assetCreationConfiguration.renderAssetModelExternalID(target, source, targetData)

        if (assetModelExternalID != null) {
            val assetModelExtId = assetModelSummaries.find { it.externalId().toString().lowercase() == assetModelExternalID.toString().lowercase() }
            if (assetModelExtId != null) {
                log.error("Asset model external ID \"$assetModelExternalID\" for source \"$source\" in target \"$target\" is already in use by asset  \"${assetModelExtId.name()}\"")
                assetModelExternalID = null
            } else{
                if (!"[a-zA-Z0-9_][a-zA-Z_\\-0-9.:]*[a-zA-Z0-9_]+".toRegex().matches(assetModelExternalID.toString())){
                    log.error("External ID \"$assetModelExternalID\" for asset model \"$assetModelName\" is not valid")
                    assetModelExternalID = null
                }
            }
        }

        val assetModelDescription = assetCreationConfiguration.renderAssetModelDescription(target, source, targetData)

        log.info("Creating asset model \"$assetModelName\" for source \"$source\"")

        val measurementPropertiesDefinitions = sourceOutputData?.channels?.filter { it.value.value != null }?.map { (channelName, channelData) ->

            val propertyNameForChannel = assetCreationConfiguration.renderAssetPropertyName(target, source, channelName, targetData)

            var externalIdForChannel = assetCreationConfiguration.renderAssetModelPropertyExternalID(target, source, channelName, targetData)
            if  ((externalIdForChannel != null ) && (!"[a-zA-Z0-9_][a-zA-Z_\\-0-9.:]*[a-zA-Z0-9_]+".toRegex().matches(externalIdForChannel.toString()))){
                log.error("External ID \"$externalIdForChannel\" for value $channelName is not valid")
                externalIdForChannel = null
            }

            createAssetModelMeasurementPropertyDefinition(
                propertyNameForChannel, externalIdForChannel, channelName, channelData)
        }

        val assetModelTags = assetCreationConfiguration.renderAssetModelTags(target, source, targetData)

        val createAssetModelRequestBuilder = CreateAssetModelRequest.builder().assetModelName(assetModelName).assetModelDescription(assetModelDescription)
            .assetModelProperties((measurementPropertiesDefinitions ?: emptyList()).toMutableList())

        if (!assetModelExternalID.isNullOrEmpty()) {
            createAssetModelRequestBuilder.assetModelExternalId(assetModelExternalID)
        }

        if (assetModelTags.isNotEmpty()) createAssetModelRequestBuilder.tags(assetModelTags)

        val createAssetModelRequest = createAssetModelRequestBuilder.build()

        val creatAssetModelResponse = client.createAssetModel(createAssetModelRequest)

        val describeAssetModelResponse = describeAssetModelWhenReady(creatAssetModelResponse.assetModelId())

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
        private val MODEL_AVAILABLE_STATUS: Set<AssetModelStatus> = setOf(MODEL_ACTIVE_STATUS, MODEL_CREATING_STATUS, MODEL_PROPAGATING_STATUS, MODEL_UPDATING_STATUS)

        private val ASSET_STATE_CREATING: AssetStatus = AssetStatus.builder().state(AssetState.CREATING).build()
        private val ASSET_STATE_ACTIVE: AssetStatus = AssetStatus.builder().state(AssetState.ACTIVE).build()
        private val ASSET_STATE_UPDATING: AssetStatus = AssetStatus.builder().state(AssetState.UPDATING).build()

        private val ASSET_BUSY_STATUS: Set<AssetStatus> = setOf(ASSET_STATE_CREATING, ASSET_STATE_UPDATING)
        private val ASSET_AVAILABLE_STATUS: Set<AssetStatus> = setOf(ASSET_STATE_ACTIVE, ASSET_STATE_CREATING, ASSET_STATE_UPDATING)

        private val MEASUREMENT: Measurement = Measurement.builder().build()
        private val MEASUREMENT_TYPE: PropertyType = PropertyType.builder().measurement(MEASUREMENT).build()

    }

}

fun AwsSiteWiseAssetCreationConfiguration.renderAssetName(target: String, source: String, targetData: TargetData): String =
    renderTemplate(assetName, targetData.schedule, source, target, 128, targetData.metaDataAtSourceLevel(source))

fun AwsSiteWiseAssetCreationConfiguration.renderAssetDescription(target: String, source: String, targetData: TargetData): String =
    renderTemplate(assetDescription, targetData.schedule, target, target, 2048, targetData.metaDataAtSourceLevel(source), true)

fun AwsSiteWiseAssetCreationConfiguration.renderAssetModelName(target: String, source: String, targetData: TargetData): String =
    renderTemplate(assetModelName, targetData.schedule, source, target, 256, targetData.metaDataAtSourceLevel(source))

fun AwsSiteWiseAssetCreationConfiguration.renderAssetModelExternalID(target: String, source: String, targetData: TargetData): String? =
    if (assetModelExternalID.isNullOrEmpty()) null else renderTemplate(assetModelExternalID!!, targetData.schedule, source, target, 128, targetData.metaDataAtSourceLevel(source)).replace("/", "_")

fun AwsSiteWiseAssetCreationConfiguration.renderAssetExternalID(target: String, source: String, targetData: TargetData): String? =
    if (assetExternalID.isNullOrEmpty()) null else renderTemplate(assetExternalID!!, targetData.schedule, source, target, 128, targetData.metaDataAtSourceLevel(source)).replace("/", "_")

fun AwsSiteWiseAssetCreationConfiguration.renderAssetModelDescription(target: String, source: String, targetData: TargetData): String =
    renderTemplate(assetModelDescription, targetData.schedule, source, target, 2048, targetData.metaDataAtSourceLevel(source), true)

fun AwsSiteWiseAssetCreationConfiguration.renderAssetPropertyName(target: String, source: String, channel: String, targetData: TargetData): String =
    renderTemplate(assetPropertyName, targetData.schedule, source, channel, target, 256, targetData.metaDataAtChannelLevel(source, channel))

fun AwsSiteWiseAssetCreationConfiguration.renderAssetModelPropertyExternalID(target: String, source: String, channel: String, targetData: TargetData): String? =
    if (assetModelPropertyExternalID.isNullOrEmpty()) null else renderTemplate(
        assetModelPropertyExternalID!!,
        targetData.schedule,
        source,
        channel,
        target,
        1000,
        targetData.metaDataAtChannelLevel(source, channel)).replace("/", "_")

fun AwsSiteWiseAssetCreationConfiguration.renderAssetPropertyAlias(target: String, source: String, channel: String, assetID: String, targetData: TargetData): String? =
    if (assetPropertyAlias.isNullOrEmpty()) null
    else (renderTemplate(assetPropertyAlias!!, targetData.schedule, source, channel, target, 128, targetData.metaDataAtChannelLevel(source, channel))
        .replace(TEMPLATE_UUID, UUID.randomUUID().toString().replace(TEMPLATE_PRE_POSTFIX, ""))
        .replace(AwsSiteWiseAssetCreationConfiguration.TEMPLATE_ASSET_ID, assetID.replace(TEMPLATE_PRE_POSTFIX, "")))

private fun AwsSiteWiseAssetCreationConfiguration.renderAssetModelTags(target: String, source: String, targetData: TargetData): Map<String, String> = renderTags(
    this.assetModelTags, targetData.schedule, source, target, targetData.metaDataAtSourceLevel(source))

private fun AwsSiteWiseAssetCreationConfiguration.renderAssetTags(target: String, source: String, targetData: TargetData): Map<String, String> = renderTags(
    this.assetTags, targetData.schedule, source, target, targetData.metaDataAtSourceLevel(source))


private fun renderTemplate(template: String, schedule: String, source: String, channel: String, target: String, maxLength: Int, metadata: Map<String, String>?, useDateTime: Boolean = true): String {
    return renderTemplate(template, schedule, source, target, maxLength, metadata, useDateTime)
        .replace(TEMPLATE_CHANNEL, channel.replace(TEMPLATE_PRE_POSTFIX, ""))
}

private fun renderTags(tagsTemplate: Map<String, String>?, schedule: String, source: String, target: String, metadata: Map<String, String>?): Map<String, String> =
    if (tagsTemplate.isNullOrEmpty()) emptyMap()
    else {
        val tags = sequence {
            tagsTemplate.forEach { (key, value) ->

                var tagValue = value.replace(TEMPLATE_SCHEDULE, schedule.replace(TEMPLATE_PRE_POSTFIX, "")).replace(TEMPLATE_SOURCE, source.replace(TEMPLATE_PRE_POSTFIX, ""))
                    .replace(TEMPLATE_TARGET, target.replace(TEMPLATE_PRE_POSTFIX, "")).replace(TEMPLATE_DATETIME, systemDateTime().toString())

                for (entry in metadata ?: emptyMap()) {
                    tagValue = tagValue.replace(
                        "${TEMPLATE_PRE_POSTFIX}entry.key$TEMPLATE_PRE_POSTFIX", entry.value.replace(TEMPLATE_PRE_POSTFIX, ""))
                }

                tagValue = tagValue.trim().substring(0, minOf(tagValue.length, 256))

                if (tagValue.isNotEmpty()) this.yield(key.substring(0, minOf(key.length, 128)) to tagValue)

            }
        }.toList()
        tags.subList(0, minOf(tags.size, 50)).toMap()

    }

private fun renderTemplate(template: String, schedule: String, source: String, target: String, maxLength: Int, metadata: Map<String, String>?, useDateTime: Boolean = true): String {
    var s = template.replace(TEMPLATE_SCHEDULE, schedule.replace(TEMPLATE_PRE_POSTFIX, "")).replace(TEMPLATE_SOURCE, source.replace(TEMPLATE_PRE_POSTFIX, ""))
        .replace(TEMPLATE_TARGET, target.replace(TEMPLATE_PRE_POSTFIX, ""))
    if (useDateTime) {
        s = s.replace(TEMPLATE_DATETIME, systemDateTime().toString())
    }
    if (metadata != null) {
        for (entry in metadata) {
            s = s.replace(
                "${TEMPLATE_PRE_POSTFIX}${entry.key}$TEMPLATE_PRE_POSTFIX", entry.value.replace(TEMPLATE_PRE_POSTFIX, ""))
        }
    }
    return s.trim().substring(0, minOf(s.length, maxLength))
}

