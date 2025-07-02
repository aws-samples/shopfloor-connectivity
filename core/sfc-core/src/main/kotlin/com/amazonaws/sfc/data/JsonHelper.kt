
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.data


import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.apache.commons.lang3.NotImplementedException
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.lang.reflect.Type
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * JSON serializer based on GSon with support for additional types
 */
class JsonHelper {


    /**
     * Extends GSON with Instant type
     */
    class InstantAdapter : TypeAdapter<Instant>() {
        override fun write(writer: JsonWriter?, value: Instant?) {
            if ((writer != null) && (value != null)) {
                writer.value(value.toString())
            }
        }

        override fun read(reader: JsonReader?): Instant {

            return Instant.parse(reader?.nextString().toString())
        }
    }


    /**
     * Extends GSON with Duration type
     */
    class DurationAdapter : TypeAdapter<Duration>() {
        override fun write(writer: JsonWriter?, value: Duration?) {
            if ((writer != null) && (value != null)) {
                writer.value(value.inWholeMilliseconds)
            }
        }

        override fun read(reader: JsonReader?): Duration? {
            if (reader != null) {
                return try {
                    val ms = reader.nextLong()
                    ms.toDuration(DurationUnit.MILLISECONDS)
                } catch (_: Exception) {
                    null
                }
            }
            return null
        }
    }

    /**
     * Extends GSON with Unsigned long type
     */
    class ULongAdapter : TypeAdapter<ULong>() {
        override fun write(writer: JsonWriter?, value: ULong?) {
            if ((writer != null) && (value != null)) {
                // GSON serialization serializes ULong as string, convert to double to get a number
                writer.value(value.toDouble())
            }
        }

        override fun read(reader: JsonReader?): ULong {
            throw NotImplementedException("Not implemented")
        }
    }


    /**
     * Extends GSON with UInt type
     */
    class UIntAdapter : TypeAdapter<UInt>() {
        override fun write(writer: JsonWriter?, value: UInt?) {
            if ((writer != null) && (value != null)) {
                writer.value(value.toString())
            }
        }

        override fun read(reader: JsonReader?): UInt {
            throw NotImplementedException("Not implemented")
        }
    }


    /**
     * Extends GSON with UShort type
     */
    class UShortAdapter : TypeAdapter<UShort>() {
        override fun write(writer: JsonWriter?, value: UShort?) {
            if ((writer != null) && (value != null)) {
                writer.value(value.toString())
            }
        }

        override fun read(reader: JsonReader?): UShort {
            throw NotImplementedException("Not implemented")
        }
    }


    /**
     * Extends GSON with File type
     */
    class FileAdapter : TypeAdapter<File>() {
        override fun write(writer: JsonWriter?, value: File?) {
            if ((writer != null) && (value != null)) {
                writer.value(value.absolutePath)
            }
        }

        override fun read(reader: JsonReader?): File {
            throw NotImplementedException("Not implemented")
        }
    }


    /**
     * Extends GSON with UByte type
     */
    class UByteAdapter : TypeAdapter<UByte>() {
        override fun write(writer: JsonWriter?, value: UByte?) {
            if ((writer != null) && (value != null)) {
                writer.value(value.toString())
            }
        }

        override fun read(reader: JsonReader?): UByte {
            throw NotImplementedException("Not implemented")
        }
    }


    companion object {

        /**
         * Creates instance of GSON with additional types
         * @return Gson
         */
        fun gsonExtended(): Gson =
            gsonBuilder().create()


        /**
         * Creates instance of GSON with additional types and pretty printing output
         * @return Gson
         */
        fun gsonPretty(): Gson =
            gsonBuilder().setPrettyPrinting().create()


        // Extends GSON with additional types used in SFC
        fun gsonBuilder(): GsonBuilder {
            return GsonBuilder()
                .serializeNulls()
                // GSON causes warning due to illegal reflection on Instant class
                .registerTypeAdapter(Instant::class.java, InstantAdapter())
                // GSON uses data member of unsigned types as value, use custom adapters to output just the value for these types
                .registerTypeAdapter(UByte::class.java, UByteAdapter())
                .registerTypeAdapter(UShort::class.java, UShortAdapter())
                .registerTypeAdapter(UInt::class.java, UIntAdapter())
                .registerTypeAdapter(java.io.File::class.java, FileAdapter())
                .registerTypeAdapter(Duration::class.java, DurationAdapter())
                .registerTypeAdapter(ULong::class.java, ULongAdapter())
        }

        fun <T> fromJsonExtended(g : Gson, json: String, classOfT: Class<T>): T {
            return try {
                g.fromJson(json, classOfT as Type?)
            } catch (e: JsonSyntaxException) {
                throw enrichedJsonSyntaxErrorException(e, BufferedReader(json.reader()).readText())
            } catch (e: Exception) {
                throw e
            }
        }

        fun <T> fromJsonExtended(json: String, classOfT: Class<T>): T {
            return fromJsonExtended(gsonExtended(), json, classOfT)
        }

        fun <T> fromJsonExtended(jsonInputStream: InputStream, classOfT: Class<T>): T {
            val json = jsonInputStream.bufferedReader().use(BufferedReader::readText)
            return fromJsonExtended(json, classOfT)

        }




        private fun enrichedJsonSyntaxErrorException(e: JsonSyntaxException, json: String): JsonSyntaxException {
            val r = """.*line\s(\d+)\s""".toRegex()
            val matches = r.findAll(e.message.toString()).toList()
            if (matches.isNotEmpty() && matches.first().groups.size == 2) {
                val line = matches.first().groups[1]?.value?.toIntOrNull()
                return if (line != null) {
                    val start = maxOf(0, line - 5)
                    val end = minOf(json.lines().size-1, line + 5)
                    var lines = json.lines().slice(IntRange(start, end))
                    lines = lines.mapIndexed { i, l ->
                        val index = start + i + 1
                        String.format("%08d", index) + (if (index == line) " ==> " else "     ") + l + "\n"
                    }
                    JsonSyntaxException(e.toString() + "\n" + lines.joinToString(separator = ""))
                } else {
                    e
                }
            }
            return e
        }


        fun forEachNode(key : String = "", node: Any?, trail: List<String> = emptyList(), fnFilter: (String, Any?, List<String>)->Boolean = { _: String, _: Any?, _: List<String> -> true }, fnAction: (String, Any?, List<String>) -> Unit) {
            when (node) {

                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    val mm = node as Map<String, Any>
                    mm.mapNotNull { (key, item) ->
                        val trail1 = trail + key
                        key to forEachNode(key, item, trail1, fnFilter, fnAction)
                    }.toMap()
                }

                is List<*>  -> {
                    @Suppress("UNCHECKED_CAST")
                    val list = if (node.isNotEmpty()) (node as MutableList<Any>) else mutableListOf()
                    list.mapNotNull { item ->
                        forEachNode(key, item, trail, fnFilter, fnAction)
                    }
                }
                else ->  {
                     if (fnFilter(key, node, trail) )fnAction(key, node, trail)
                }
            }
        }

        fun forEachStringNode(node: Any?, trail: List<String> = emptyList(), fnFilter: (String?)->Boolean = {true}, fnAction: (String, List<String>) -> Pair<String, Any?>): Any? {
            return when (node) {

                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    val mm = node as Map<String, Any>
                    mm.mapNotNull { (key, item) ->
                        key to forEachStringNode(item, trail, fnFilter, fnAction)
                    }.toMap()
                }

                is List<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    val list = if (node.isNotEmpty()) (node as MutableList<Any>) else mutableListOf()
                    list.mapNotNull { item ->
                        forEachStringNode(item, trail, fnFilter, fnAction)
                    }
                }

                is String -> {
                    val item = if (fnFilter(node) )fnAction(node, trail) else null to node
                    return if (item.second != node) forEachStringNode(item.second, if (item.first == null) trail else trail + (item.first?:""), fnFilter, fnAction) else item.second
                }
                else -> node
            }
        }

        private val integerJsonValueRegex = """(".+":\s)(\d+.0+([\n,}]+?))""".toRegex()
        fun isStructuredValue(v: String) = (v.startsWith("{") && v.endsWith("}"))

        fun reformatJsonString(s: String): String {
            if (!isStructuredValue(s)) return s
            // build formated JSON string
            var structured = gsonExtended().toJson(fromJsonExtended(s, Map::class.java))
            //  strip trailing ".0" from integers
            integerJsonValueRegex.findAll(structured, 0).forEach {
                val oldValue = it.groups[0]?.value
                val newValue = it.groups[1]?.value + it.groups[2]?.value?.split('.')?.first() + it.groups[3]?.value
                structured = structured.replace(oldValue ?: "", newValue)
            }
            return structured
        }

        fun JsonSyntaxException.extendedJsonException(json : String) = enrichedJsonSyntaxErrorException(this, json)


    }

}