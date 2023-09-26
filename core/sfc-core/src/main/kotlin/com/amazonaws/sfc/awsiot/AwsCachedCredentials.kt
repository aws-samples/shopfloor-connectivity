package com.amazonaws.sfc.awsiot

import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import java.time.Instant

class AwsCachedCredentials {
    var credentials: AwsSessionCredentials? = null
    var responseCode = 0
    var expiry: Instant? = null
}