// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.util

object TemplateRenderer {

    private const val TEMPLATE_PRE_POSTFIX = "%"
    private const val TEMPLATE_SCHEDULE = "${TEMPLATE_PRE_POSTFIX}schedule${TEMPLATE_PRE_POSTFIX}"
    private const val TEMPLATE_SOURCE = "${TEMPLATE_PRE_POSTFIX}source${TEMPLATE_PRE_POSTFIX}"
    private const val TEMPLATE_TARGET = "${TEMPLATE_PRE_POSTFIX}target${TEMPLATE_PRE_POSTFIX}"
    private const val TEMPLATE_CHANNEL = "${TEMPLATE_PRE_POSTFIX}channel${TEMPLATE_PRE_POSTFIX}"


    fun render(template: String, schedule: String, source: String, channel: String, target: String, metadata: Map<String, String>?): String {

        var s = template
            .replace(TEMPLATE_SCHEDULE, schedule.replace(TEMPLATE_PRE_POSTFIX, ""))
            .replace(TEMPLATE_SOURCE, source.replace(TEMPLATE_PRE_POSTFIX, ""))
            .replace(TEMPLATE_CHANNEL, channel.replace(TEMPLATE_PRE_POSTFIX, ""))
            .replace(TEMPLATE_TARGET, target.replace(TEMPLATE_PRE_POSTFIX, ""))

        if (metadata != null) {
            for (entry in metadata) {
                s = s.replace(
                    "${TEMPLATE_PRE_POSTFIX}${entry.key}$TEMPLATE_PRE_POSTFIX", entry.value.replace(TEMPLATE_PRE_POSTFIX, ""))
            }
        }
        return s
    }

    fun containsPlaceHolders(s : String): Boolean = getPlaceHolders(s).isNotEmpty()

    fun getPlaceHolders(s : String):List<String> = PLACE_HOLDER_REGEX.findAll(s).map{it.value}.toList()

    private val PLACE_HOLDER_REGEX = ("$TEMPLATE_PRE_POSTFIX.+$TEMPLATE_PRE_POSTFIX").toRegex()

}

