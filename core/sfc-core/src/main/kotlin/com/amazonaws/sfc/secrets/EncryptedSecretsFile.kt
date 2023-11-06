
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.secrets

import com.amazonaws.sfc.data.JsonHelper.Companion.fromJsonExtended
import com.amazonaws.sfc.data.JsonHelper.Companion.gsonExtended
import com.amazonaws.sfc.util.constrainFilePermissions

import java.io.File
import java.io.IOException

/**
 * File with data for locally stores Secrets Manager secrets
 * @property fileName String Name of the file
 */
class EncryptedSecretsFile(val fileName: String) : SecretsStore<EncryptedSecretsDocument, EncryptedSecret> {

    private var _secretsDocument: EncryptedSecretsDocument? = null

    private val secretsDocument: EncryptedSecretsDocument
        get() {
            if (_secretsDocument == null) {

                _secretsDocument = try {
                    if (File(fileName).exists())
                        fromJsonExtended(File(fileName).inputStream(), EncryptedSecretsDocument::class.java)
                    else null
                } catch (_: IOException) {
                    null
                }
            }
            return _secretsDocument ?: EncryptedSecretsDocument()
        }


    override fun getAll(): EncryptedSecretsDocument {
        return secretsDocument
    }

    override fun get(secretName: String, label: String?): EncryptedSecret? =
        secretsDocument.secrets?.firstOrNull {
            ((it.name == secretName || it.arn == secretName) && ((label == null) || (it.versionStages ?: emptyList()).contains(label)))
        }

    override fun saveAll(list: EncryptedSecretsDocument) {
        val file = File(fileName)
        file.writeText(gsonExtended().toJson(list))
        constrainFilePermissions(file)
        _secretsDocument = null
    }

}