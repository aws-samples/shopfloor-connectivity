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