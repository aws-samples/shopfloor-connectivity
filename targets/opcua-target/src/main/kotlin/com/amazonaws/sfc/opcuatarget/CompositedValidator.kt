// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.opcuatarget

import org.eclipse.milo.opcua.sdk.server.Session
import org.eclipse.milo.opcua.sdk.server.identity.IdentityValidator
import org.eclipse.milo.opcua.stack.core.UaException
import org.eclipse.milo.opcua.stack.core.types.structured.SignatureData
import org.eclipse.milo.opcua.stack.core.types.structured.UserIdentityToken
import org.eclipse.milo.opcua.stack.core.types.structured.UserTokenPolicy

class CompositedValidator<T>(vararg validators: IdentityValidator<*>) : IdentityValidator<T> {


    private val validators: List<IdentityValidator<*>> = validators.map { it }

    @Suppress("UNCHECKED_CAST")
    override fun validateIdentityToken(session: Session, token: UserIdentityToken, tokenPolicy: UserTokenPolicy, tokenSignature: SignatureData): T {

        validators.forEach {
            try {
                return it.validateIdentityToken(session, token, tokenPolicy, tokenSignature)  as T
            } catch (e: Exception) {
                if (validators.last() == it) {
                    throw e
                }
            }
        }

        throw BAD_IDENTITY_TOKEN_EXCEPTION
    }

    companion object {
        private const val BAD_IDENTITY_TOKEN_INVALID = 2149580800L
        private val BAD_IDENTITY_TOKEN_EXCEPTION = UaException(BAD_IDENTITY_TOKEN_INVALID)
    }
}

