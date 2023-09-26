/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.router.config

import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.TargetConfiguration
import com.amazonaws.sfc.config.Validate
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.amazonaws.sfc.router.config.RouterWriterConfiguration.Companion.ROUTER
import com.amazonaws.sfc.router.config.RoutesConfiguration.Companion.CONFIG_ALTERNATE_TARGET
import com.amazonaws.sfc.router.config.RoutesConfiguration.Companion.CONFIG_SUCCESS_TARGET
import com.google.gson.annotations.SerializedName

class RouterTargetConfiguration : Validate, TargetConfiguration() {

    @SerializedName(CONFIG_ROUTES)
    private var _routes: Map<String, RoutesConfiguration> = emptyMap()
    val routes
        get() = _routes

    override val subTargets
        get() = (_routes.keys + _routes.flatMap { it.value.routeTargets }).toList()

    @SerializedName(CONFIG_RESULT_HANDLER_POLICY)
    private var _resultHandlerPolicy: RouterResultHandlerPolicy = RouterResultHandlerPolicy.ALL_TARGETS
    val resultHandlerPolicy
        get() = _resultHandlerPolicy

    override fun validate() {
        if (validated) return
        checkRoutes()
        validated = true
    }

    private fun checkRoutes() {

        _routes.values.forEach { it.validate() }

        val primaryTargetIDs = routes.keys
        _routes.forEach { (primary, secondaryRoutes) ->
            if (secondaryRoutes.successTargetID in primaryTargetIDs) throw ConfigurationException("$CONFIG_SUCCESS_TARGET  \"${secondaryRoutes.successTargetID}\" for primary target \"$primary\" can not be used as it is also used as a primary target ${routes[secondaryRoutes.successTargetID]}", CONFIG_SUCCESS_TARGET, this)
            if (secondaryRoutes.alternateTargetID in primaryTargetIDs) throw ConfigurationException("$CONFIG_ALTERNATE_TARGET  \"${secondaryRoutes.successTargetID}\" for primary target \"$primary\" can not be used as it is also used as a primary target ${routes[secondaryRoutes.alternateTargetID]}", CONFIG_ALTERNATE_TARGET, this)
        }

    }


    companion object {

        const val CONFIG_ROUTES = "Routes"
        const val CONFIG_RESULT_HANDLER_POLICY = "ResultHandlerPolicy"

        private val default = RouterTargetConfiguration()

        fun create(routes: Map<String, RoutesConfiguration>,
                   resultHandlerPolicy: RouterResultHandlerPolicy,
                   description: String = default._description,
                   active: Boolean = default._active,
                   targetServer: String? = default._server,
                   credentialProviderClient: String? = default._credentialProvideClient,
                   metrics: MetricsSourceConfiguration = default._metrics): RouterTargetConfiguration {

            val instance = createTargetConfiguration<RouterTargetConfiguration>(description = description,
                active = active,
                targetType = ROUTER,
                targetServer = targetServer,
                metrics = metrics,
                credentialProviderClient = credentialProviderClient) as RouterTargetConfiguration

            with(instance) {
                _routes = routes
                _resultHandlerPolicy = resultHandlerPolicy
            }

            return instance

        }
    }
}