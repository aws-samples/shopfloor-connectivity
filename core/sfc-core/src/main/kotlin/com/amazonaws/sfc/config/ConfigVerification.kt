package com.amazonaws.sfc.config

import com.amazonaws.sfc.crypto.KeyHelpers
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.*
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature

object ConfigVerification {

    private fun getConfigSignature(config: Map<*, *>, privateKey: PrivateKey): ByteArray {
        val signature = Signature.getInstance(("SHA256withRSA"))
        signature.initSign((privateKey))
        val bytes = Gson().toJson(config).toByteArray()
        signature.update(bytes)
        return signature.sign()
    }

    private val ByteArray.asHexString: String
        get() = this.joinToString("") { "%02x".format(it) }

    private fun String?.asByteArray(): ByteArray {
        if (this == null) return ByteArray(0)
        val bytes = ByteArray(this.length / 2)
        for (i in bytes.indices) {
            val byte = this.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            bytes[i] = byte
        }
        return bytes
    }

    fun sign(configFile: File, privateKeyFile: File, signedConfig: OutputStream) {
        sign(configFile.inputStream(), privateKeyFile, signedConfig)
    }

    fun sign(configFile: File, privateKey: PrivateKey, signedConfig: OutputStream) {
        sign(configFile.inputStream(), privateKey, signedConfig)
    }

    fun sign(configFile: File, privateKeyFile: File, signedConfigFile: File) {
        sign(configFile.inputStream(), privateKeyFile, signedConfigFile.outputStream())
    }

    fun sign(configFile: File, privateKey: PrivateKey, signedConfigFile: File) {
        sign(configFile.inputStream(), privateKey, signedConfigFile.outputStream())
    }

    fun sign(config: InputStream, privateKeyFile: File, signedConfigFile: File) {
        sign(config, privateKeyFile, signedConfigFile.outputStream())
    }

    fun sign(config: InputStream, privateKey: PrivateKey, signedConfigFile: File) {
        sign(config, privateKey, signedConfigFile.outputStream())
    }


    fun sign(configJson: String, privateKey: PrivateKey): String {
        val buffer = ByteArrayOutputStream()
        sign(configJson.byteInputStream(), privateKey, buffer)
        return buffer.toString()
    }

    fun sign(configJson: String, privateKey: PrivateKey, signedConfig: OutputStream) {
        return sign(configJson.byteInputStream(), privateKey, signedConfig)
    }

    fun sign(configJson: String, privateKeyFile: File, signed: OutputStream) {
        sign(configJson.byteInputStream(), privateKeyFile, signed)

    }

    fun sign(configJson: String, privateKeyFile: File): String {
        val buffer = ByteArrayOutputStream()
        sign(configJson.byteInputStream(), privateKeyFile, buffer)
        return buffer.toString()
    }


    fun sign(configStream: InputStream, privateKeyFile: File, signedConfig: OutputStream) {
        val privateKey = KeyHelpers.loadPrivateKey(privateKeyFile)
        return sign(configStream, privateKey, signedConfig)
    }


    @Suppress("UNCHECKED_CAST")
    fun sign(configStream: InputStream, privateKey: PrivateKey, signedConfig: OutputStream) {

        val configAsMap =
            Gson().fromJson(configStream.bufferedReader().readText(), MutableMap::class.java) as MutableMap<String, Any>
        configAsMap.remove(CONFIG_SIGNATURE)
        val signature = getConfigSignature(configAsMap, privateKey)
        configAsMap[CONFIG_SIGNATURE] = signature.asHexString
        val gsonBuilder = GsonBuilder().disableHtmlEscaping().setPrettyPrinting()
        val json = gsonBuilder.create().toJson(configAsMap)
        val o = OutputStreamWriter(signedConfig, "UTF-8")
        o.write(json)
        o.flush()
    }


    fun verify(configJson: String, publicKeyFile: File): Boolean {
        return verify(configJson.byteInputStream(), publicKeyFile)
    }

    fun verify(configJson: String, publicKey: PublicKey): Boolean {
        return verify(configJson.byteInputStream(), publicKey)
    }

    fun verify(configFile: File, publicKeyFile: File): Boolean {
        return verify(configFile.inputStream(), publicKeyFile)
    }

    fun verify(configFile: File, publicKey: PublicKey): Boolean {
        return verify(configFile.inputStream(), publicKey)
    }


    fun verify(configStream: InputStream, publicKeyFile: File): Boolean {
        val publicKey = KeyHelpers.loadPublicKey(publicKeyFile)
        return verify(configStream, publicKey)
    }

    @Suppress("UNCHECKED_CAST")
    fun verify(configStream: InputStream, publicKey: PublicKey): Boolean {

        val configAsMap =
            Gson().fromJson(configStream.bufferedReader().readText(), Map::class.java) as MutableMap<String, Any>

        val signatureBytes = (configAsMap.remove("ConfigSignature") as String?).asByteArray()
        if (signatureBytes.isEmpty()) return false

        val signature = Signature.getInstance(("SHA256withRSA"))
        signature.initVerify(publicKey)
        val bytes = Gson().toJson(configAsMap).toByteArray()
        signature.update(bytes)

        return signature.verify(signatureBytes)

    }

    private const val CONFIG_SIGNATURE = "ConfigSignature"

}