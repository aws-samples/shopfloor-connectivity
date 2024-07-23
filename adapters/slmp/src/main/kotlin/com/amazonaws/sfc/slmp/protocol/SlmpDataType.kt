// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.slmp.protocol

enum class SlmpDataType {
    WORD,
    BIT,
    DOUBLEWORD,
    STRING,
    STRUCT;

    companion object {

        fun fromString(s: String): SlmpDataType {
            return when (s.uppercase()) {
                "WORD" -> WORD
                "BIT" -> BIT
                "DOUBLEWORD" -> DOUBLEWORD
                "STRING" -> STRING
                else ->
                    if (SlmpStructureType.knowStructureTypes.containsKey(s)) STRUCT else
                        throw SlmpException("Invalid data type $s")
            }
        }
    }

}




