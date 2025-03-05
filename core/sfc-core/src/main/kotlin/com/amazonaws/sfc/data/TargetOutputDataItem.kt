// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.data

import com.amazonaws.sfc.system.DateTime
import java.time.Instant

class TargetOutputDataItem(val schedule: String,
                           val sources: Map<String, SourceOutputData>,
                           val metadata: Map<String, String>,
                           val serial: String,
                           val timestamp: Instant?)

