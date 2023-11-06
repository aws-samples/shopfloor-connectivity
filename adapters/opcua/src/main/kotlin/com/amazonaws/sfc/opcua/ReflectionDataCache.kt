
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.opcua

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties

internal class ReflectionDataCache {
    private val cachedProperties: MutableMap<String, List<KProperty1<Any, *>>> = mutableMapOf()
    private val cachedGetters: MutableMap<String, Map<String, KFunction<*>>?> = mutableMapOf()

    fun propertiesForClass(clazz: KClass<Any>): Collection<KProperty1<Any, *>> {

        val qualifiedName = clazz.qualifiedName ?: ""
        val cachedPropsForClass = cachedProperties[qualifiedName]
        if (cachedPropsForClass != null) {
            return cachedPropsForClass
        }

        val newPropsForClass = try {
            clazz.memberProperties
        } catch (_: Throwable) {
            emptyList()
        }

        cachedProperties[qualifiedName] = newPropsForClass.toList()
        return newPropsForClass
    }

    private fun propertyGettersForClass(clazz: KClass<Any>): Map<String, KFunction<*>> {
        val qualifiedName = clazz.qualifiedName ?: ""
        val cachedGettersForClass = cachedGetters[qualifiedName]
        if (cachedGettersForClass != null) {
            return cachedGettersForClass
        }

        val newGettersForClass = clazz.memberFunctions
            .filter { m -> m.name.startsWith("get") && m.visibility == KVisibility.PUBLIC }
            .map {
                it.name[3].lowercase() + it.name.substring(4) to it
            }.filter { g -> g.first in propertiesForClass(clazz).map { p -> p.name } }
            .toMap()
        cachedGetters[qualifiedName] = newGettersForClass
        return newGettersForClass
    }

    fun getterForProperty(clazz: KClass<Any>, property: KProperty1<Any, *>) =
        propertyGettersForClass(clazz)[property.name]
}