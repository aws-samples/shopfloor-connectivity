package com.amazonaws.sfc.awsiot

import java.nio.charset.StandardCharsets

//@NoArgsConstructor
//@AllArgsConstructor
//@Data
class IotCloudResponse(private var responseBody: ByteArray, var statusCode: Int) {
    override fun toString(): String {
        return String(responseBody, StandardCharsets.UTF_8)
    }
}