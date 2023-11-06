
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations


import com.amazonaws.sfc.config.Validate
import com.amazonaws.sfc.log.Logger


interface TransformationOperator : Validate {
    //val returnType : Class<*>
    fun invoke(target: Any, valueName: String = "", checkType: Boolean = true, throwsException: Boolean = true, logger: Logger? = null): Any?
    val inputType: Class<*>
    val resultType: Class<*>

    override fun validate() {
    }

}





