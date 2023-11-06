
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


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
