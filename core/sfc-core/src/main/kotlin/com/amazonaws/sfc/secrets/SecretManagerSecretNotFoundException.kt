
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.secrets

class SecretManagerSecretNotFoundException(val secretID: String, message: String) : SecretManagerException(message)
