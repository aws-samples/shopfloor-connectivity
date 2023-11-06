
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.data.JmesPathExtended
import com.amazonaws.sfc.util.LookupCacheHandler
import com.google.gson.JsonObject
import io.burt.jmespath.Expression
import kotlinx.coroutines.runBlocking

@ConfigurationClass
@TransformerOperator(["Query"])
class Query(operand: String) : TransformationImpl<String>(operand) {


    @TransformerMethod
    fun apply(target: Any?): Any? =

        if (operand == null) null
        else try {
            runBlocking {
                val query = cachedQueries.getItemAsync(operand).await()
                query?.search(target)
            }
        } catch (_: Throwable) {
            null
        }

    override fun validate() {
        ConfigurationException.check(
            (operand != null),
            "Operand for ${this::class.simpleName} operator must be set to a valid jmespath query",
            "${this::class.simpleName}.${TransformationsDeserializer.CONFIG_TRANSFORMATION_OPERATOR}",
            this)

        try {
            runBlocking {
                if (cachedQueries.getItemAsync(operand.toString()).await() == null) {
                    throw TransformationException("Could not create query", "Query")
                }

            }
        } catch (e: Exception) {
            throw ConfigurationException("\"$operand.toString()\" is not a valid jmespath query, $e",
                "${this::class.simpleName}.${TransformationsDeserializer.CONFIG_TRANSFORMATION_OPERATOR}",
                this)
        }

    }


    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorWithOperand.fromJson<Query, String>(o) { op ->
            val queryString = op.asString.toString()
            runBlocking {
                cachedQueries.getItemAsync(queryString).await()
                ?: throw TransformationException("Invalid operand \"$queryString\" specified for query operator", "Query")
            }
            queryString
        }

        fun create(operand: String) = Query(operand)

        private val cachedQueries = LookupCacheHandler<String, Expression<Any>?, String>(
            supplier = { null },
            initializer = { qs, _, _ -> jmes.compile(qs) },
            onInitializationError = { _, _, _ -> null }
        )

        private val jmes = JmesPathExtended.create()
    }

}