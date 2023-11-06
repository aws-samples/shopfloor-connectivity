
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


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