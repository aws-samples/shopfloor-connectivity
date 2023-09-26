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

import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.amazonaws.sfc.log.Logger

class InvalidTransformationOperator(val operatorName: String? = "", val message: String, val item: String) : TransformationOperator, Validate {

    override fun validate() {
        if (operatorName.isNullOrEmpty()) {
            throw ConfigurationException(message, TransformationsDeserializer.CONFIG_TRANSFORMATION_OPERATOR, item)
        }
        throw ConfigurationException("Operator \"$operatorName\", $message", TransformationsDeserializer.CONFIG_TRANSFORMATION_OPERATOR, item)
    }

    override var validated: Boolean = false

    override fun invoke(target: Any, valueName: String, checkType: Boolean, throwsException: Boolean, logger: Logger?): Any? = null
    override fun toString(): String {
        return "InvalidTransformationOperator(operatorName=$operatorName, message='$message)"
    }

    override val inputType: Class<*> = Nothing::class.java
    override val resultType: Class<*> = Nothing::class.java

}