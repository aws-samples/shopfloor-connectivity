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





