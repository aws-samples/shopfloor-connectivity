/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */


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