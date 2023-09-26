/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject
import software.amazon.awssdk.utils.StringUtils.upperCase

@ConfigurationClass
@TransformerOperator(["UpperCase"])
class UpperCase : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: String?): String? = if (target == null) null else upperCase(target)

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<UpperCase>(o)

        fun create() = UpperCase()
    }
}