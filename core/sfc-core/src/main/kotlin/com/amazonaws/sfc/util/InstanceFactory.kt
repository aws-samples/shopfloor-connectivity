
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.util

import com.amazonaws.sfc.config.InProcessConfiguration
import com.amazonaws.sfc.data.ProtocolAdapterException
import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.log.Logger
import java.io.File
import java.net.URLClassLoader

open class InstanceFactory<T>(private val config: InProcessConfiguration, private val logger: Logger) {

    private val className = this::class.java.simpleName

    // gets the class for the factory for the instances to create from the jar file(s)
    private val classToLoad by lazy {

        val log = logger.getCtxLoggers(className, "classToLoad")

        if (config.jarFiles.isNullOrEmpty()) {
            if (logger.level == LogLevel.TRACE) {
                val classloader = ClassLoader.getSystemClassLoader()
                val urls =(classloader as URLClassLoader).urLs.map{url->url.file}
                log.trace("No jar files specified for '${config.factoryClassName}, using factory class from classpath:  ${urls.joinToString()}")
            }
        } else {
            log.trace("Loading factory class name class ${config.factoryClassName} from ${config.jarFiles!!.joinToString()}")
        }

        val expandedJars = expandedJarList(config.jarFiles ?: emptyList())
        if (expandedJars.isEmpty()) {
            Class.forName(config.factoryClassName)
        } else {
            if (config.jarFiles != expandedJars) {
                log.trace("configured jars expanded to ${expandedJars.joinToString()}")
            }
            val classLoader = URLClassLoader(expandedJars.map { it.toURI().toURL() }.toTypedArray(), this::class.java.classLoader)

            Class.forName(config.factoryClassName, true, classLoader)
        }
    }

    // gets the method for the factory class that is calls to create new instances
    private val creatorMethod by lazy {
        val trace = logger.getCtxTraceLog(className, "creatorMethod")
        trace("Getting factory class method $CREATOR_METHOD_NAME from ${config.factoryClassName}")
        classToLoad?.getDeclaredMethod(CREATOR_METHOD_NAME, Array::class.java)
    }


    // creates a new instance by calling the static new instance method of the factory class
    fun createInstance(vararg createParameters: Any?): T? {
        val logs = logger.getCtxLoggers(className, "createInstance")
        try {
            @Suppress("UNCHECKED_CAST")
            return creatorMethod?.invoke(null, createParameters) as T?
        } catch (e: java.lang.reflect.InvocationTargetException) {
            if (e.cause is ProtocolAdapterException){
                logs.error("Error creating instance of \"${config.factoryClassName}\" because a required class was not fond at ${config.jarFiles?.joinToString()}, ${(e.cause as ProtocolAdapterException).message}")
            } else {
                logs.error(
                    "Error creating instance of \"${config.factoryClassName}\" from  ${config.jarFiles?.joinToString()}, cause is ${e.targetException}, $e")
            }

        }
        return null
    }


    companion object {
        const val CREATOR_METHOD_NAME = "newInstance"

        fun expandedJarList(jars: List<File>): List<File> {
            val expandedJars: List<File> = jars.flatMap { j ->

                if (j.isFile) {
                    listOf(j)
                } else {
                    if (j.isDirectory) {
                        (j.listFiles()?.filter { f -> f.isFile && f.extension.lowercase() == "jar" }) ?: emptyList()
                    } else
                        emptyList()
                }
            }
            return expandedJars
        }
    }
}