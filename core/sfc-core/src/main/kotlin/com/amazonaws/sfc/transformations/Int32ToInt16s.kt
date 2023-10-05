/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

@ConfigurationClass
@TransformerOperator(["Int32ToInt16s"])
class Int32ToInt16s : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: Number?): List<Short>? =

        if (target == null) null else listOf((target.toInt() shr 16).toShort(), (target.toInt() and 0xFFFF).toShort())

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<Int32ToInt16s>(o)
        fun create() = Int32ToInt16s()
    }
}