//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.simulator.simulations

import com.amazonaws.sfc.system.DateTime.systemDateTime
import com.amazonaws.sfc.system.DateTime.systemDateTimeUTC
import com.google.gson.JsonObject
import java.time.ZoneId
import java.util.*

class DateTime(val utc: Boolean = false) : Simulation {

    override fun value(): Any =
        if (utc)
            systemDateTimeUTC().atZone(ZoneId.of("UTC"))
        else
            systemDateTime().atZone(ZoneId.of(TimeZone.getDefault().id))


    companion object {

        private const val CONFIG_UTC_TIMEZONE = "UTC"

        fun fromJson(o: JsonObject): Simulation {
            val utc = try{o.get(CONFIG_UTC_TIMEZONE).asBoolean}catch (_ : Exception){false}
            return DateTime(utc)
        }
    }

}