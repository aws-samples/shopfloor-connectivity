// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.simulator.simulations

import com.amazonaws.sfc.simulator.SimulatorException
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_SIMULATION_TYPE
import com.amazonaws.simulation.Random
import com.google.gson.*
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Type
import java.security.InvalidParameterException
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance

class SimulationDeserializer : JsonDeserializer<Simulation> {

    data class SimulationCreateInstanceData(val name: String, val companionInstance: Any, val createMethod: Method)

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Simulation? {
        return json?.asJsonObject?.let { simulationFromJsonObject(it) }
    }

    private fun simulationFromJsonObject(o: JsonObject): Simulation {

        val simulationPrimitive = o.getAsJsonPrimitive(CONFIG_SIMULATION_TYPE)
        val simulation = simulationPrimitive?.asString ?: return InvalidSimulation("Simulation","No $CONFIG_SIMULATION_TYPE specified")

        val simulationData = knownSimulations[simulation.uppercase()] ?: return InvalidSimulation("Simulation","$CONFIG_SIMULATION_TYPE \"$simulation\" is unknown, $o")
        return try {
            simulationData.createMethod.invoke(simulationData.companionInstance, o) as Simulation
        } catch (e: InvocationTargetException) {
            return InvalidSimulation(simulation,"Can not deserialize instance of simulations type \"$simulation\" from configuration, ${e.targetException.message}, $o")
        }
    }

    companion object {
        @Suppress("MemberVisibilityCanBePrivate") // keep public to allow registration of custom operators
        fun <T : Simulation> registerSimulation(t: KClass<T>) {
            val companionInstance = t.companionObjectInstance ?: return

            val name: String = t.simpleName.toString().uppercase()

            val o = knownSimulations[name]
            if (o != null) {
                if (o.name != name)
                    throw SimulatorException("Registering function $name, operator name \"$name\" is already used for simulations ${o.name}")
            } else {
                knownSimulations[name] =
                    SimulationCreateInstanceData(name, companionInstance, companionInstance::class.java.getDeclaredMethod("fromJson", JsonObject::class.java))
            }

        }


        private val knownSimulations = mutableMapOf<String, SimulationCreateInstanceData>()


        init {
            listOf(

                // Values generating simulations
                Constant::class,
                Counter::class,
                DateTime::class,
                Interval::class,
                Random::class,
                Range::class,
                Sawtooth::class,
                Sinus::class,
                Square::class,

                // Composite simulations
                Triangle::class,
                Buffered::class,
                List::class,
                Structure::class

            ).forEach {
                registerSimulation(it)
            }
        }

        fun jsonToNative(item: JsonElement): Any = when {

            item.isJsonPrimitive -> {
                when {
                    item.asJsonPrimitive.isBoolean -> item.asJsonPrimitive.asBoolean
                    item.asJsonPrimitive.asJsonPrimitive.isString -> item.asJsonPrimitive.asString
                    item.asJsonPrimitive.asJsonPrimitive.isNumber -> item.asJsonPrimitive.asDouble
                    else -> item.asJsonPrimitive.asJsonPrimitive.asString
                }
            }

            item.isJsonArray -> {
                item.asJsonArray.filter { it.isJsonPrimitive }.map { subItem ->
                    jsonToNative(subItem)
                }
            }

            else -> {
                emptyList<Any>()
            }
        }

        fun asDouble(value: Any) = when (value) {
            is JsonPrimitive -> value.asDouble
            is Byte -> value.toDouble()
            is UByte -> value.toDouble()
            is Short -> (value).toDouble()
            is UShort -> value.toDouble()
            is Int -> value.toDouble()
            is UInt -> value.toDouble()
            is Long -> value.toDouble()
            is ULong -> value.toDouble()
            is Float -> value.toDouble()
            is String -> value.toDouble()
            is Double -> value.toDouble()
            is Boolean -> if (value) 1.0 else 0.0
            else -> {
                throw InvalidParameterException("Value $value must be a number")
            }
        }

        fun JsonObject.getDouble(key: String, default: Double?): Double? {
            if (!this.has(key)) return default
            val d = this.get(key)
            if (d.isJsonNull) return default
            return try {
                this.get(key).asDouble
            } catch (_: Exception) {
                null
            }
        }

        fun JsonObject.getString(key: String, default: String?): String? {
            if (!this.has(key)) return default
            val s = this.get(key)
            if (s.isJsonNull) return default
            return try {
                this.get(key).asString
            } catch (_: Exception) {
                null
            }
        }

        fun JsonObject.getInt(key: String, default: Int?): Int? {
            if (!this.has(key)) return default
            val i = this.get(key)
            if (i.isJsonNull) return default
            return try {
                this.get(key).asInt
            } catch (_: Exception) {
                null
            }
        }

        fun JsonObject.getLong(key: String, default: Long?): Long? {
            if (!this.has(key)) return default
            val l = this.get(key)
            if (l.isJsonNull) return default
            return try {
                this.get(key).asLong
            } catch (_: Exception) {
                null
            }
        }

        fun createSimulationReader(): Gson = GsonBuilder()
            .registerTypeAdapter(Simulation::class.java, SimulationDeserializer())
            .create()
    }

}

