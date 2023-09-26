/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.util

import org.apache.commons.io.FileUtils

fun byteCountToString(i: Int): String = FileUtils.byteCountToDisplaySize(i.toLong())

val Int.byteCountString: String
    get() = byteCountToString(this)

fun byteCountToString(l: Long): String = FileUtils.byteCountToDisplaySize(l)
val Long.byteCountString: String
    get() = byteCountToString(this)

fun byteCountToString(n: Number): String = FileUtils.byteCountToDisplaySize(n)
val Number.byteCountString: String
    get() = byteCountToString(this)

