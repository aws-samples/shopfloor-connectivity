
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.crypto

import com.google.gson.annotations.SerializedName

enum class CertificateFormat(val ext: String) {
    @SerializedName("Unknown")
    Unknown(""),

    @SerializedName("Pem")
    Pem("pem"),

    @SerializedName("Pkcs12")
    Pkcs12("pfx")
}