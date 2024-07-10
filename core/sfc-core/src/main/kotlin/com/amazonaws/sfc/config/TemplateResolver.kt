// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.config

import com.amazonaws.sfc.data.JsonHelper.Companion.forEachStringNode
import com.amazonaws.sfc.data.JsonHelper.Companion.gsonPretty


class TemplateResolver( private val templateSection : String) {

    @Suppress("UNCHECKED_CAST")
    fun resolve(config : Map<String, Any>): Any {

        val templates = config[templateSection] as Map<String, Any>? ?: return config


        val configMap = forEachStringNode(config) { node, trail ->
            val templateMatch = templateRegex.find(node) ?: return@forEachStringNode ("" to node)
            val template = parseTemplate(templateMatch.value)

            val resolved = resolve(templates, template, trail)

            if (templateMatch.value == node) {
                return@forEachStringNode template.name to resolved
            }
            if (resolved is Map<*, *> || resolved is List<*>) throw TemplateException(
                "Partial replacement values can not be lists or structures, ${template.name} -> ${gsonPretty().toJson(resolved)}"
            )
            return@forEachStringNode template.name to node.replace(templateMatch.value, resolved.toString())
        } as MutableMap<String, Any>

        configMap.remove(templateSection)
        return configMap

    }


    private fun parseTemplate(templateStr: String): Template {
        val templateMatchResult =
            templateRegex.matchEntire(templateStr.trim()) ?: throw TemplateException("Template \"$templateStr\" is not a valid template")
        val templateName = templateMatchResult.groups["name"]?.value?.trim()
            ?: throw TemplateException("Template is not valid as it does not contain the name of the template")
        val placeholdersStr = templateMatchResult.groups["placeholders"]?.value ?: ""
        val placeholders = templatePlaceHoldersRegex.findAll(placeholdersStr.trim()).map {
            val placeholder = it.value.split("=")
            if (placeholder.size != 2) throw TemplateException("Placeholder\"$it\" in template \"$templateStr\" is not valid, it must have the format name=value")
            placeholder[0].trim(' ', ',') to placeholder[1].trim(' ', ',')
        }.toMap()

        return Template(templateName, placeholders)
    }




    private fun resolve(templates: Map<String, Any>, templateValue: Template, trail: List<String>): Any? {

        // check for recursive templates
        if (trail.contains(templateValue.name)) {
            throw TemplateException("\"${templateValue.name}\" in template \"${trail.last()}\" is recursive, $trail")
        }

        // get template from map of available templates
        val template: Any = templates[templateValue.name]
            ?: throw TemplateException(
                "Template \"${templateValue.name}\" does not exist" +
                        "${if (trail.isNotEmpty()) ", $trail" else ""}, " +
                        "available templates are ${templates.keys.joinToString { "\"$it\"" }}"
            )


        // no values in the template to replace, return template value
        if (templateValue.values.isEmpty()) {
            return template
        }

        return replacePlaceHolders(template, trail, templateValue)

    }

    private fun replacePlaceHolders(template: Any, trail: List<String>, templateValue: Template): Any? {

        val replaced = forEachStringNode(template, trail) { node, t ->

            if (t.contains(templateValue.name))
                throw TemplateException("Recursion for template ${templateValue.name}")

            var replacedStr = node

            templateValue.values.forEach { (t, u) ->
                replacedStr = replacedStr.replace("%$t%", u)
            }

            "" to replacedStr
        }
        return replaced
    }


    companion object {
        private val templateRegex = "\\$\\((?<name>[^,^\\s)]+)\\s?(,\\s?(?<placeholders>.*))?\\)".toRegex()
        private val templatePlaceHoldersRegex = ("(?:([^=]+=[^,\\]]+))").toRegex()
        private val numericValueRegex = "-?\\d+(\\.\\d+)?$".toRegex()
        private fun isNumericValue(v: String) = numericValueRegex.matches(v)

    }

}

