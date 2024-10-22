// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.opcuatarget

import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator

class UserNameValidator(allowAnonymousAccess: Boolean) : UsernameIdentityValidator(allowAnonymousAccess, { false })