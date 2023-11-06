
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.secrets

/**
 * Data class to store encrypted Secret Manager secrets locally
 */
data class EncryptedSecret(
    val arn: String,
    val name: String,
    val alias: String?,
    val versionId: String? = null,
    val encryptedSecretString: String? = null,
    val encryptedSecretBinary: String? = null,
    val versionStages: List<String>? = null,
    val createdDate: Long = 0)

