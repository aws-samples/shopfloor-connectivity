/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.util

import com.amazonaws.sfc.config.InProcessConfiguration
import com.amazonaws.sfc.log.Logger
import java.io.File
import java.net.URLClassLoader

open class InstanceFactory<T>(private val config: InProcessConfiguration, private val logger: Logger) {

    private val className = this::class.java.simpleName

    // gets the class for the factory for the instances to create from the jar file(s)
    private val classToLoad by lazy {

        val log = logger.getCtxLoggers(className, "classToLoad")

        if (config.jarFiles.isNullOrEmpty()) {
            throw Exception("No jar files specified")
        } else {
            log.trace("Loading factory class name class $config.factoryClassName from ${config.jarFiles!!.joinToString()}")
        }

        val expandedJars = expandedJarList(config.jarFiles ?: emptyList())
        if (expandedJars.isEmpty()) {
            throw Exception("No jar files to load from ${config.jarFiles!!.joinToString(",")}")
        } else {
            if (config.jarFiles != expandedJars) {
                log.trace("configured jars expanded to ${expandedJars.joinToString()}")
            }
        }

        val classLoader = URLClassLoader(expandedJars.map { it.toURI().toURL() }.toTypedArray(), this::class.java.classLoader)

        Class.forName(config.factoryClassName, true, classLoader)
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
            logs.error("Error creating instance of \"${config.factoryClassName}\" from jar ${config.jarFiles?.joinToString()}, cause is ${e.targetException}")
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