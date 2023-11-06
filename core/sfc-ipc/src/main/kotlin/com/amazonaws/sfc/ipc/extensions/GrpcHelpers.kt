
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.ipc.extensions

import com.google.protobuf.Timestamp
import java.time.Instant


fun newTimestamp(ts: Instant): Timestamp = Timestamp.newBuilder().setSeconds(ts.epochSecond).setNanos(ts.nano).build()

