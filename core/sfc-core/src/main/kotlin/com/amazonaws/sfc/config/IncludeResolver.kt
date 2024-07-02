// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.config

import com.amazonaws.sfc.data.JmesPathExtended
import com.amazonaws.sfc.data.JsonHelper.Companion.forEachStringNode
import com.amazonaws.sfc.data.JsonHelper.Companion.fromJsonExtended
import com.amazonaws.sfc.util.FileReaderCache
import com.amazonaws.sfc.util.UrlReaderCache
import com.google.gson.JsonSyntaxException
import io.ktor.http.*
import java.io.File
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object IncludeResolver {

    class IncludeResolverException(message: String) : Exception(message)


    private val urlCache: UrlReaderCache by lazy {
        UrlReaderCache(
            cachePeriod = 60.toDuration(DurationUnit.SECONDS),
            maxRetries = 5,
            waitBetweenReties = 1.toDuration(DurationUnit.SECONDS)
        )
    }

    private val fileCache: FileReaderCache by lazy {
        FileReaderCache()
    }

    fun removeFromCache(file: File) {
        fileCache.remove(file)
    }

    fun removeFromCache(url: Url) {
        urlCache.remove(url)
    }

    fun resolve(node: Any, fnResolved: (List<String>) -> Unit = {}): Any {
        val resolvedItems = mutableSetOf<String>()
        val resolved = forEachStringNode(node) { n, trail ->
            processIncludeFile(n, trail) { s -> resolvedItems.add(s) }
        }
        fnResolved(resolvedItems.toList())
        return resolved
    }

    private fun processIncludeFile(
        stringNode: String,
        trail: List<String>,
        fnResolved: (String) -> Unit = {}
    ): Pair<String?, Any> {
        val node = ConfigReader.setEnvironmentValues(stringNode)

        return try {

            if ((!node.startsWith("@")) || (node.length < 2)) null to node else {

                val i = node.indexOf('@', 1)
                val base = if (i == -1) node else node.substring(0,i)
                val selector = if (i != -1) node.substring(i+1) else null

                val urlString = base.substring(1)

                val includedData = when {
                    // from file
                    (base.startsWith("@file:", true)) -> loadFromFile(base, trail, fnResolved)
                    // from url
                    (base.startsWith("@") && isUrl(urlString)) -> loadFromUrl(urlString, trail, fnResolved)
                    // use string as is
                    else -> null to node
                }
                if (includedData.first != null && selector == null) includedData else {
                    val jmesPath = JmesPathExtended.create().compile(selector)
                    base to jmesPath.search(includedData.second)
                }
            }

        } catch (e: JsonSyntaxException) {
            throw IncludeResolverException("Invalid JSON loaded from $stringNode, $e")
        } catch (e: Exception) {
            throw IncludeResolverException("Error loading configuration from ,$stringNode,  $e")
        }
    }

    private fun loadFromFile(node: String, trail: List<String>, fnResolved: (String) -> Unit): Pair<String, Any> {
        val includeFile = node.substring(6)
        val includedFile = File(includeFile)

        if (trail.contains(includedFile.absolutePath))
            throw IncludeResolverException("Recursion processing included file $includedFile, $ ${trailString(trail + includedFile.absolutePath)}")

        val includedText = fileCache[includedFile] ?: "{}"
        fnResolved("file:${includedFile.absolutePath}")
        return includedFile.absolutePath to fromJsonExtended(includedText, Any::class.java)
    }

    private fun loadFromUrl(urlString: String, trail: List<String>, fnResolved: (String) -> Unit): Pair<String, Any> {
        val url = Url(urlString)
        if (trail.contains(url.toString()))
            throw IncludeResolverException("Recursion reading from url $url, ${trailString(trail + urlString)}")
        val content = urlCache[url] ?: "{}"
        fnResolved(urlString)
        return urlString to fromJsonExtended(content, Any::class.java)
    }


    private fun trailString(trail: List<String>) = trail.joinToString(separator = " > ")
    val urlRegex =
        "https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&//=]*)".toRegex()

    private fun isUrl(s: String): Boolean = urlRegex.matches(s.lowercase())


}