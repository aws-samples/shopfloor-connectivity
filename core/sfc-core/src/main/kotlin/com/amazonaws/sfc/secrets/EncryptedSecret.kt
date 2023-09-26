/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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

