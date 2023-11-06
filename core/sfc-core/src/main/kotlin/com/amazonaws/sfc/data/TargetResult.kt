
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0



package com.amazonaws.sfc.data

data class TargetResult(
    val targetID: String,
    val ackSerialList: List<String>?,
    val ackMessageList: List<TargetData>?,
    val nackSerialList: List<String>?,
    val nackMessageList: List<TargetData>?,
    val errorSerialList: List<String>?,
    val errorMessageList: List<TargetData>?,
) {

    val containsAcks = (!ackSerialList.isNullOrEmpty() || !ackMessageList.isNullOrEmpty())
    val containsNacks = (!nackSerialList.isNullOrEmpty() || !nackMessageList.isNullOrEmpty())
    val containsErrors = (!errorSerialList.isNullOrEmpty() || !errorMessageList.isNullOrEmpty())

    val ackSerials = if (!ackSerialList.isNullOrEmpty()) ackSerialList else ackMessageList?.map { it.serial } ?: emptyList()
    val nackSerials = if (!nackSerialList.isNullOrEmpty()) ackSerialList else nackMessageList?.map { it.serial } ?: emptyList()
    val errorSerials = if (!errorSerialList.isNullOrEmpty()) ackSerialList else errorMessageList?.map { it.serial } ?: emptyList()

}