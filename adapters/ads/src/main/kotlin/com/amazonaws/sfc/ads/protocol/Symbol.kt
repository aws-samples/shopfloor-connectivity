/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */

package com.amazonaws.sfc.ads.protocol

class Symbol(
    val symbolName: String,
    val indexGroup: Int,
    val indexOffset: Int,
    val dataType: DataType,
    val typeName: String,
    val size: Int,
    val arrayDimensions: List<Int>? = null

) {

    class AdsSymbolBuilder {

        var symbolName: String? = null
        var indexGroup: Int? = null
        var indexOffset: Int? = null
        var dataType: DataType? = null
        var typeName: String? = null
        var size: Int? = null
        var arrayDimensions: List<Int>? = null

        fun symbolName(name: String) = apply {
            this.symbolName = name
        }

        fun indexGroup(indexGroup: Int) = apply {
            this.indexGroup = indexGroup
        }

        fun indexOffset(indexOffset: Int) = apply {
            this.indexOffset = indexOffset
        }

        fun dataType(dataType: DataType) = apply {
            this.dataType = dataType
        }

        fun typeName(typeName: String) = apply {
            this.typeName = typeName
        }

        fun size(size: Int) = apply {
            this.size = size
        }

        fun arrayDimensions(arrayDimensions: List<Int>?) = apply {
            this.arrayDimensions = arrayDimensions
        }


        fun build() = Symbol(
            symbolName ?: throw Exception(),
            indexGroup ?: throw Exception(),
            indexOffset ?: throw Exception(),
            dataType ?: throw Exception(),
            typeName ?: throw Exception(),
            size ?: throw Exception(),
            arrayDimensions
        )
    }

    val arrayDimensionsStr =
        arrayDimensions?.joinToString(
            prefix = "[",
            postfix = "]",
            separator = ","
        ) { d -> d.toString() }
            ?: ""

    override fun toString(): String {
        return "AdsSymbol(name=$symbolName, " +
                "indexGroup=$indexGroup, " +
                "indexOffset=$indexOffset, " +
                "dataType=$dataType, " +
                "typeName=$typeName, " +
                "size=$size ${if (arrayDimensions != null) ", arrayDimensions=$arrayDimensionsStr" else ""}"
    }

    companion object {
        fun builder() = AdsSymbolBuilder()
    }
}