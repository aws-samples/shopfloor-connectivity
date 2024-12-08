// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.nats

import com.amazonaws.sfc.log.Logger
import io.nats.client.AuthHandler
import io.nats.client.NKey

import java.io.File


class NKeyAuthHandler(private val path: String, private val logger: Logger) : AuthHandler {

    private val className = this.javaClass.name

    val nKey: NKey? by lazy {
        val log = logger.getCtxLoggers(className, "nKey")
        try {
            val buffer = charArrayOf(*(File(path).readText().toCharArray()))
            NKey.fromSeed(buffer)
        } catch (e: Exception) {
            log.error("Unable to load NKey from file \"$path\", $e")
            throw e
        }
    }

    override fun sign(nonce: ByteArray?): ByteArray? {
        val log = logger.getCtxLoggers(className, "sign")
        return try {
            this.nKey?.sign(nonce)
        } catch (e: Exception) {
            log.error("Unable to sign with NKey, $e")
            null
        }
    }

    override fun getID(): CharArray? {
        val log = logger.getCtxLoggers(className, "getID")
        return try {
            nKey?.getPublicKey()
        } catch (e: Exception) {
            log.error("Unable to get public key from NKey, $e")
            null
        }
    }

    override fun getJWT(): CharArray? = null

}