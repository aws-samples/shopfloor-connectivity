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

import com.google.gson.JsonObject

class TransformationOperatorNumericOperand : TransformationOperatorWithOperand() {

    companion object {
        inline fun <reified R : TransformationOperator> fromJson(o: JsonObject): TransformationOperator = fromJson<R, Number>(o) { op ->
            // use string to allow parsing of hex etc.
            val s = op.asString.toString()
            if (s.isBlank()) throw TransformationException("Numeric operator can not be empty", "TransformationOperatorNumericOperand")
            try {
                Integer.decode(s)
            } catch (e: NumberFormatException) {
                throw TransformationException("\"$s\" is not a valid number", "TransformationOperatorNumericOperand")
            }
        }
    }
}