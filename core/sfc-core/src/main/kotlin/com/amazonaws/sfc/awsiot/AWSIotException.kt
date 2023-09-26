/*
  * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
  * SPDX-License-Identifier: Apache-2.0
  */

package com.amazonaws.sfc.awsiot

class AWSIotException : Exception {
    constructor(message: String?, e: Throwable?) : super(message, e)
    constructor(e: Throwable?) : super(e)
    constructor(message: String?) : super(message)
}