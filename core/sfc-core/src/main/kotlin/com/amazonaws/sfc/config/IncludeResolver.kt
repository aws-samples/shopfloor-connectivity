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

object  IncludeResolver {

    class IncludeResolverException(message: String) : Exception(message)

    var cacheResults : Boolean = false
    var cacheDirectory: String? = null


    private val urlCache: UrlReaderCache by lazy {
        UrlReaderCache(
            cachePeriod = 60.toDuration(DurationUnit.SECONDS),
            maxRetries = 5,
            waitBetweenReties = 1.toDuration(DurationUnit.SECONDS),
            cacheDirectory = cacheDirectory,
            cacheResults = cacheResults
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

    fun resolve(node: Any, fnResolved: (List<String>) -> Unit = {}): Any? {
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
    ): Pair<String, Any> {
        val node = ConfigReader.setEnvironmentValues(stringNode)

        return try {

            if ((!node.startsWith("@")) || (node.length < 2)) node to node else {

                val i = node.indexOf('@', 1)
                val base: String = if (i == -1) node else node.substring(0, i)
                val selector = if (i != -1) node.substring(i + 1) else null

                val urlString = base.substring(1)

                val includedData: Pair<String, Any> = when {
                    // from file
                    (base.startsWith("@file:", true)) -> loadFromFile(base, trail, fnResolved)
                    // from url
                    (base.startsWith("@") && isUrl(urlString)) -> loadFromUrl(urlString, trail, fnResolved)
                    // use string as is
                    else -> base to node
                }
                if (selector == null) includedData
                else {
                    val jmesPath = JmesPathExtended.create().compile(selector)
                    val search = jmesPath.search(includedData.second)
                    if (search == null) {
                        val hint = if ("^[a-zA-B0-9_]".toRegex()
                                .containsMatchIn(selector.toString())
                        ) ", selector may contain restricted characters, see https://jmespath.org/specification.html for more info" else ""
                        throw IncludeResolverException("Selector \"$selector\" in \"$node\" is invalid or returns no selected data $hint")
                    }
                    base to search
                }
            }

        } catch (e: JsonSyntaxException) {
            throw IncludeResolverException("Invalid JSON loaded from $stringNode, $e")
        } catch (e: Exception) {
            throw IncludeResolverException("Error loading configuration from, $stringNode,  $e")
        }
    }

    private fun loadFromFile(node: String, trail: List<String>, fnResolved: (String) -> Unit): Pair<String, Any> {
        val includeFile = node.substring(6)
        val includedFile = File(includeFile)

        if (trail.contains(includedFile.absolutePath))
            throw IncludeResolverException("Recursion processing included file $includedFile, $ ${trailString(trail + includedFile.absolutePath)}")

        var includedText = fileCache[includedFile] ?: "{}"
        includedText = includedText.replace("\\u003d", "=")
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