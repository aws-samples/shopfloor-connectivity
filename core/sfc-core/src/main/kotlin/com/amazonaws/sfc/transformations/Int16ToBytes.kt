/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
@TransformerOperator(["Int16ToBytes"])
class Int16ToBytes : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: Int?): List<Byte>? =

        if (target == null) null else listOf((target.toInt() shr 8).toByte(), (target.toInt() and 0xFF).toByte())

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<Int16ToBytes>(o)
        fun create() = Int16ToBytes()
    }
}