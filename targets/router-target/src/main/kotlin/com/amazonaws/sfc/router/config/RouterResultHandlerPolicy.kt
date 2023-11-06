
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.router.config

import com.google.gson.annotations.SerializedName

enum class RouterResultHandlerPolicy {
    @SerializedName("AllTargets")
    ALL_TARGETS,

    @SerializedName("AnyTarget")
    ANY_TARGET,
}