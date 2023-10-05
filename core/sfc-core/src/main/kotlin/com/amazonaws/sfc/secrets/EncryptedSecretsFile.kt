/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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