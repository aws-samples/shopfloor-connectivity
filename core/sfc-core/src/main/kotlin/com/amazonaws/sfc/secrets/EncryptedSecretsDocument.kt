
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.secrets

/**
 * Document to  locally store encrypted secret manager secrets
 * @property secrets List<EncryptedSecret>?
 */
class EncryptedSecretsDocument {
    /**
     * List of store secrets
     */
    var secrets: List<EncryptedSecret>? = null
}
