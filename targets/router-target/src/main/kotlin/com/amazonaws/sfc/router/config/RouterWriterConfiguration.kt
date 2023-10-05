/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.router.config

import com.amazonaws.sfc.awsiot.AwsIotCredentialProviderClientConfiguration
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.router.config.RouterTargetConfiguration.Companion.CONFIG_ROUTES
import com.amazonaws.sfc.router.config.RoutesConfiguration.Companion.CONFIG_ALTERNATE_TARGET
import com.amazonaws.sfc.router.config.RoutesConfiguration.Companion.CONFIG_SUCCESS_TARGET
import com.google.gson.annotations.SerializedName


@ConfigurationClass
class RouterWriterConfiguration : BaseConfigurationWithMetrics() {

    @SerializedName(CONFIG_TARGETS)
    private var _targets: Map<String, RouterTargetConfiguration> = emptyMap()
    val targets
        get() = _targets

    val routerTargets
        get() = _targets.filter { it.value.targetType == ROUTER }


    private val primaryTargetKeys by lazy { routerTargets.values.flatMap { it.routes.keys }.toSet() }

    private val secondaryTargetKeys by lazy {
        routerTargets.values.flatMap { t ->
            t.routes.values.flatMap { r ->
                listOfNotNull(r.successTargetID, r.alternateTargetID)
            }
        }.toSet()
    }

    private val validPrimaryRoutingTargets by lazy {
        targets.keys
            .filter { !routerTargets.keys.contains(it) }
            .filter { !secondaryTargetKeys.contains(it) }
            .filter { targets[it] != null }
    }

    private val validPrimaryRoutingTargetsStr by lazy { validPrimaryRoutingTargets.joinToString(separator = ",") { "\"it\"" } }

    private val validSecondaryTargetKeys by lazy {
        targets.keys
            .filter { !routerTargets.keys.contains(it) }
            .filter { !primaryTargetKeys.contains(it) }
            .filter { targets[it] != null }
    }

    private val validSecondaryTargetsStr by lazy { validSecondaryTargetKeys.joinToString(separator = ",") { "\"it\"" } }

    override fun validate() {

        if (validated) return
        super.validate()

        validateRoutes()

        validated = true
    }

    private fun validateRoutes() {
        routerTargets.forEach { (routerTargetID, routerTargetConfiguration) ->
            routerTargetConfiguration.validate()
            routerTargetConfiguration.routes.forEach { (primaryTarget, routesConfiguration) ->
                validatePrimaryTargetID(routerTargetID, primaryTarget, routerTargetConfiguration)
                validateSecondaryTargets(routerTargetID, routerTargetConfiguration, primaryTarget, routesConfiguration)
            }
        }
    }

    private fun validateSecondaryTargets(routerTargetID: String,
                                         routerTargetConfiguration: RouterTargetConfiguration,
                                         primaryTarget: String,
                                         routesConfiguration: RoutesConfiguration) {
        listOf(
            routesConfiguration.successTargetID to CONFIG_SUCCESS_TARGET,
            routesConfiguration.alternateTargetID to CONFIG_ALTERNATE_TARGET).forEach { (secondaryTargetID, targetType) ->

            validateSecondaryTarget(secondaryTargetID, routerTargetID, primaryTarget, targetType, routerTargetConfiguration)
        }
    }

    private fun validateSecondaryTarget(secondaryTargetID: String?,
                                        routerTargetID: String,
                                        primaryTarget: String,
                                        targetType: String,
                                        routerTargetConfiguration: RouterTargetConfiguration) {
        ConfigurationException.check(
            secondaryTargetID.isNullOrEmpty() || targets[secondaryTargetID] != null,
            "Target \"$routerTargetID\", primary target \"$primaryTarget\", $targetType route target \"$secondaryTargetID\" " +
            "does not exist, valid targets are $validSecondaryTargetsStr",
            CONFIG_ROUTES,
            routerTargetConfiguration
        )

        ConfigurationException.check(
            validSecondaryTargetKeys.contains(secondaryTargetID),
            "Target \"$routerTargetID\",target \"$primaryTarget\", $targetType route target \"$secondaryTargetID\" " +
            "can not be used as it is already used as a router target ID or a primary target which can cause routing loops, " +
            "valid targets are $validSecondaryTargetsStr",
            CONFIG_ROUTES,
            routerTargetConfiguration
        )
    }

    private fun validatePrimaryTargetID(routerTargetID: String,
                                        primaryTarget: String,
                                        routerTargetConfiguration: RouterTargetConfiguration) {

        ConfigurationException.check(
            validPrimaryRoutingTargets.contains(primaryTarget),
            "Target \"$routerTargetID\" primary target \"$primaryTarget\" does not exist, valid targets are $validPrimaryRoutingTargetsStr",
            CONFIG_ROUTES,
            routerTargetConfiguration
        )

        ConfigurationException.check(
            routerTargets.contains(primaryTarget),
            "Target \"$routerTargetID\", primary target \"$primaryTarget\" is already used as a target ID for a router target which can cause routing loops, " +
            "valid primary targets are $validPrimaryRoutingTargetsStr",
            CONFIG_ROUTES,
            routerTargetConfiguration
        )

    }


    companion object {
        const val ROUTER = "ROUTER"

        private val default = RouterWriterConfiguration()

        fun create(targets: Map<String, RouterTargetConfiguration> = default._targets,
                   name: String = default._name,
                   version: String = default._version,
                   awsVersion: String? = default._awsVersion,
                   description: String = default._description,
                   schedules: List<ScheduleConfiguration> = default._schedules,
                   logLevel: LogLevel? = default._logLevel,
                   metadata: Map<String, String> = default._metadata,
                   elementNames: ElementNamesConfiguration = default._elementNames,
                   targetServers: Map<String, ServerConfiguration> = default._targetServers,
                   targetTypes: Map<String, InProcessConfiguration> = default._targetTypes,
                   adapterServers: Map<String, ServerConfiguration> = default._protocolAdapterServers,
                   adapterTypes: Map<String, InProcessConfiguration> = default._protocolTypes,
                   awsIotCredentialProviderClients: Map<String, AwsIotCredentialProviderClientConfiguration> = default._awsIoTCredentialProviderClients,
                   secretsManagerConfiguration: SecretsManagerConfiguration? = default._secretsManagerConfiguration): RouterWriterConfiguration {

            val instance = createBaseConfiguration<RouterWriterConfiguration>(
                name = name,
                version = version,
                awsVersion = awsVersion,
                description = description,
                schedules = schedules,
                logLevel = logLevel,
                metadata = metadata,
                elementNames = elementNames,
                targetServers = targetServers,
                targetTypes = targetTypes,
                adapterServers = adapterServers,
                adapterTypes = adapterTypes,
                awsIotCredentialProviderClients = awsIotCredentialProviderClients,
                secretsManagerConfiguration = secretsManagerConfiguration)

            instance._targets = targets
            return instance
        }

    }
}