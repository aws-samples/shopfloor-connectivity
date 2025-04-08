// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.util

import com.amazonaws.sfc.system.DateTime.systemCalendarUTC
import java.util.*

object TemplateRenderer {

    private const val TEMPLATE_PRE_POSTFIX = "%"
    private const val TEMPLATE_SCHEDULE = "${TEMPLATE_PRE_POSTFIX}schedule${TEMPLATE_PRE_POSTFIX}"
    private const val TEMPLATE_SOURCE = "${TEMPLATE_PRE_POSTFIX}source${TEMPLATE_PRE_POSTFIX}"
    private const val TEMPLATE_TARGET = "${TEMPLATE_PRE_POSTFIX}target${TEMPLATE_PRE_POSTFIX}"
    private const val TEMPLATE_CHANNEL = "${TEMPLATE_PRE_POSTFIX}channel${TEMPLATE_PRE_POSTFIX}"
    private const val TEMPLATE_YEAR = "${TEMPLATE_PRE_POSTFIX}year${TEMPLATE_PRE_POSTFIX}"
    private const val TEMPLATE_MONTH = "${TEMPLATE_PRE_POSTFIX}month${TEMPLATE_PRE_POSTFIX}"
    private const val TEMPLATE_DAY = "${TEMPLATE_PRE_POSTFIX}day${TEMPLATE_PRE_POSTFIX}"
    private const val TEMPLATE_HOUR = "${TEMPLATE_PRE_POSTFIX}hour${TEMPLATE_PRE_POSTFIX}"
    private const val TEMPLATE_MINUTE = "${TEMPLATE_PRE_POSTFIX}minute${TEMPLATE_PRE_POSTFIX}"
    private const val TEMPLATE_SECOND = "${TEMPLATE_PRE_POSTFIX}second${TEMPLATE_PRE_POSTFIX}"
    private const val TEMPLATE_MILLI_SECOND = "${TEMPLATE_PRE_POSTFIX}millisecond${TEMPLATE_PRE_POSTFIX}"
    private const val TEMPLATE_UUID = "${TEMPLATE_PRE_POSTFIX}uuid${TEMPLATE_PRE_POSTFIX}"


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

    fun render(template : String): String {
        val calendar = systemCalendarUTC()

        val year = calendar.get(Calendar.YEAR).toString()
        val month = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val day = calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val hour = calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val minute = calendar.get(Calendar.MINUTE).toString().padStart(2, '0')
        val second = calendar.get(Calendar.SECOND).toString().padStart(2, '0')
        val millisecond = calendar.get(Calendar.MILLISECOND).toString().padStart(3, '0')


        var s = template
            .replace(TEMPLATE_YEAR, year.replace(TEMPLATE_PRE_POSTFIX, ""))
            .replace(TEMPLATE_MONTH, month.replace(TEMPLATE_PRE_POSTFIX, ""))
            .replace(TEMPLATE_DAY, day.replace(TEMPLATE_PRE_POSTFIX, ""))
            .replace(TEMPLATE_HOUR, hour.replace(TEMPLATE_PRE_POSTFIX, ""))
            .replace(TEMPLATE_MINUTE,  minute.replace(TEMPLATE_PRE_POSTFIX, ""))
            .replace(TEMPLATE_SECOND, second.replace(TEMPLATE_PRE_POSTFIX, ""))
            .replace(TEMPLATE_MILLI_SECOND, millisecond.replace(TEMPLATE_PRE_POSTFIX, ""))
            .replace(TEMPLATE_UUID, UUID.randomUUID().toString().replace(TEMPLATE_PRE_POSTFIX, ""))

        return s
    }

    fun containsPlaceHolders(s : String): Boolean = getPlaceHolders(s).isNotEmpty()

    fun getPlaceHolders(s : String):List<String> = PLACE_HOLDER_REGEX.findAll(s).map{it.value}.toList()

    private val PLACE_HOLDER_REGEX = ("$TEMPLATE_PRE_POSTFIX.+$TEMPLATE_PRE_POSTFIX").toRegex()

}

