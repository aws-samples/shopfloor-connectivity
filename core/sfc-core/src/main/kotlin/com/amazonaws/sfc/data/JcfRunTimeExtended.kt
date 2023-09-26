/*
 Copyright (c) 2021. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.data

import io.burt.jmespath.JmesPathType
import io.burt.jmespath.RuntimeConfiguration
import io.burt.jmespath.jcf.JcfRuntime
import java.time.Instant

/**
 * Extensions for data types not supported by default by JMesPath
 */

object JmesPathExtended {

    class JcfRunTimeExtended(c: RuntimeConfiguration) : JcfRuntime(c) {
        override fun typeOf(value: Any): JmesPathType {
            return when (value) {
                is Instant -> JmesPathType.STRING
                is Array<*> -> JmesPathType.ARRAY
                is ByteArray -> JmesPathType.ARRAY
                is UByte -> JmesPathType.NUMBER
                is UShort -> JmesPathType.NUMBER
                is UInt -> JmesPathType.NUMBER
                is ULong -> JmesPathType.NUMBER
                else ->
                    return super.typeOf(value)
            }
        }
    }

    // jmespath queries may not contain "/", replace these with '_'
    fun escapeJMesString(s: String) =
        s.replace("/", "_slash_").replace("-", "_dash_")


    /**
     * Creates new extended JcfRuntime
     * @return JcfRuntime
     */
    fun create(): JcfRuntime =
        JcfRunTimeExtended(RuntimeConfiguration.builder().withSilentTypeErrors(true).build())

}
