// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.awss3tables

import org.apache.iceberg.data.GenericRecord

class RecordBuffer() {
    private val recordData = mutableListOf<Pair<String, GenericRecord>>()
    fun addRecord(serial : String, record: GenericRecord) : Int {
        recordData.add(serial to record)
        return recordData.size
    }
    val size
        get() = recordData.size
    val serials
        get() = recordData.map { it.first }
    val records
        get() = recordData.map { it.second }
    val items
        get() = recordData.toList()

    fun clear() = recordData.clear()
}