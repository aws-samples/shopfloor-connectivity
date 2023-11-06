
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.data

import com.amazonaws.sfc.config.ElementNamesConfiguration
import com.amazonaws.sfc.data.JsonHelper.Companion.fromJsonExtended
import com.amazonaws.sfc.log.Logger
import org.apache.velocity.Template
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.runtime.resource.loader.StringResourceLoader
import org.apache.velocity.runtime.resource.util.StringResourceRepository
import java.io.File
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Applies transformation using a velocity transformation specification
 * @property templateFile File The file containing the velocity transformation template
 * @property logger Logger Logger for output
 */
class OutputTransformation(private var templateFile: File, private val logger: Logger) {

    private val className = this::class.java.simpleName

    // Velocity engine instance
    private val engine: VelocityEngine by lazy {
        val e = VelocityEngine()
        e.setProperty(Velocity.RESOURCE_LOADERS, "string")
        e.addProperty("resource.loader.string.class", StringResourceLoader::class.java.name)
        e.addProperty("resource.loader.string.repository.static", "false")
        e.init()
        e
    }

    // Velocity template
    private val template: Template? by lazy {
        try {
            val templateLines = Files.readAllLines(Paths.get(templateFile.absolutePath)).joinToString(separator = "\n")
            val repo = engine.getApplicationAttribute(StringResourceLoader.REPOSITORY_NAME_DEFAULT) as StringResourceRepository
            repo.putStringResource(templateFile.name, templateLines)
            engine.getTemplate(templateFile.name)
        } catch (e: Throwable) {
            val errorLogger = logger.getCtxErrorLog(className, "template")
            errorLogger("Error creating velocity template from file template file ${templateFile.absolutePath}, ${e.message}")
            null
        }
    }


    /**
     * Applies transformation using template
     * @param targetData TargetData Data to transform
     * @param elementNames ElementNamesConfiguration Configurable element names used for context parameters
     * @return String?
     */
    fun transform(targetData: TargetData, elementNames: ElementNamesConfiguration): String? {

        if (template == null) {
            return null
        }

        val data = fromJsonExtended(targetData.toJson(elementNames), Map::class.java)

        // writer for template output
        val sw = StringWriter()

        // create context to pass to template
        val context = VelocityContext()

        return try {
            // Add schedule name
            context.put(elementNames.schedule, data[elementNames.schedule])
            // Add data
            context.put(elementNames.sources, data[elementNames.sources])
            // Add metadata
            context.put(elementNames.metadata, data[elementNames.metadata])
            template?.merge(context, sw)
            sw.toString()
        } catch (ex: Throwable) {
            val logError = logger.getCtxErrorLog(className, "transform")
            logError("Error transforming data with template ${templateFile.name}, $ex")
            null
        }
    }

}