
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.router.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName


@ConfigurationClass
class RoutesConfiguration : Validate {

    @SerializedName(CONFIG_SUCCESS_TARGET)
    private var _successTargetID: String? = null
    val successTargetID: String?
        get() = _successTargetID

    @SerializedName(CONFIG_ALTERNATE_TARGET)
    private var _alternateTargetID: String? = null
    val alternateTargetID: String?
        get() = _alternateTargetID

    val routeTargets
        get() = listOfNotNull(_successTargetID, _alternateTargetID)

    private var _validated = false


    override var validated: Boolean
        get() = _validated
        set(value) {
            _validated = value
        }

    override fun validate() {
        if (validated) return

        validated = true
    }


    companion object {
        const val CONFIG_SUCCESS_TARGET = "Success"
        const val CONFIG_ALTERNATE_TARGET = "Alternate"

        private val default = RoutesConfiguration()

        fun create(
            successTargetID: String? = default._successTargetID,
            alternateTargetID: String? = default._alternateTargetID,
        ): RoutesConfiguration {

            val instance = RoutesConfiguration()

            with(instance) {
                _successTargetID = successTargetID
                _alternateTargetID = alternateTargetID
            }
            return instance
        }

    }
}