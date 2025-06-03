// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.awss3tables.config

enum class PartitionTransform {

    IDENTITY,
    YEAR,
    MONTH,
    DAY,
    HOUR,
    BUCKET,
    TRUNCATE
    ;

    var param: Any? = null
    var source: String = ""

    companion object {

        val TRANSFORM_REGEX = Regex("(.+)\\[\\s*(\\d+)\\s*\\]")

        fun of(name: String, source: String): PartitionTransform {

            val instance: PartitionTransform = when (name) {
                TRANSFORM_IDENTITY -> IDENTITY
                TRANSFORM_YEAR -> YEAR
                TRANSFORM_MONTH -> MONTH
                TRANSFORM_DAY -> DAY
                TRANSFORM_HOUR -> HOUR

                else -> {
                    if (TRANSFORM_REGEX.matches(name)) {
                        val (transform, param) = TRANSFORM_REGEX.find(name)!!.destructured
                        when (transform) {
                            TRANSFORM_TRUNCATE -> TRUNCATE.apply { this.param = param.toInt() }
                            TRANSFORM_BUCKET -> BUCKET.apply { this.param = param.toInt() }
                            else -> IllegalArgumentException("Unknown transform $name")
                        }
                    } else {
                        throw IllegalArgumentException("Unknown transform $name")
                    }
                }
            } as PartitionTransform
            instance.source = source
            return instance
        }

        private const val TRANSFORM_IDENTITY = "identity"
        private const val TRANSFORM_YEAR = "year"
        private const val TRANSFORM_MONTH = "month"
        private const val TRANSFORM_DAY = "day"
        private const val TRANSFORM_HOUR = "hour"
        private const val TRANSFORM_BUCKET = "bucket"
        private const val TRANSFORM_TRUNCATE = "truncate"
    }

}