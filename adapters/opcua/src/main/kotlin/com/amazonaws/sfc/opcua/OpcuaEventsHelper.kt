/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.opcua

import com.amazonaws.sfc.opcua.OpcuaAdapter.Companion.OPC_UA_NAMESPACE
import org.eclipse.milo.opcua.sdk.client.model.types.objects.BaseEventType
import org.eclipse.milo.opcua.sdk.core.QualifiedProperty
import org.eclipse.milo.opcua.stack.core.Identifiers
import org.eclipse.milo.opcua.stack.core.serialization.SerializationContext
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant
import kotlin.reflect.KCallable
import kotlin.reflect.KVisibility

class OpcuaEventsHelper : EventsHelper {


    override val allEventClassNames by lazy { allOpcuaEvents.entries.map { it.key } }
    override val allEventClassIdentifiers by lazy { allOpcuaEvents.entries.map { it.value.identifier } }
    override val allEventClasses by lazy { allEventClassNames.zip(allEventClassIdentifiers) }

    override fun findEvent(name: String?): Event? {
        if (name == null) return null
        val event = allOpcuaEvents[name]
        if (event != null) return event

        val nodeId = NodeId.parseOrNull(name)
        return if (nodeId != null) findEvent(nodeId) else null
    }


    override fun findEvent(identifier: NodeId?): Event? = allOpcuaEvents.values.firstOrNull { it.identifier == identifier }

    override fun isKnownEvent(name: String): Boolean =
        findEvent(name) != null


    override fun variantPropertiesToMap(eventVariantValues: Array<Variant>,
                                        eventProperties: List<Pair<NodeId, QualifiedName>>,
                                        context: SerializationContext): Map<String, Any> {
        val opcuaDataTypes = OpcuaDataTypesConverter(context)
        return sequence {

            eventProperties.map { it.second.name.toString() }.zip(eventVariantValues.map {
                opcuaDataTypes.asNativeValue(it)
            }).forEach {
                if (it.second != null) {
                    yield(it.first to it.second as Any)
                }
            }
        }.toMap()
    }


    // Helper method to build a list of nodeIDs and browse names for all qualified properties of an event/alarm class
    fun buildNodeBrowseNameList(nodeID: NodeId, eventClass: Class<*>) =
        getQualifiedProperties(eventClass).map { nodeID to it.browseName }


    // Get all qualified properties for a BaseEvent or inherited class
    private fun getQualifiedProperties(eventClass: Class<*>): List<QualifiedProperty<*>> {
        return eventClass.declaredFields
            .filter { it.type.isAssignableFrom(QualifiedProperty::class.java) }
            .map { it.get(null) as QualifiedProperty<*> }
    }

    fun isValidEventType(nodeEventType: String): Boolean {
        if (findEvent(nodeEventType) != null) return true

        val nodeID = NodeId.parseOrNull(nodeEventType)
        return (nodeID != null) && (findEvent(nodeID) != null)

    }

    private val allOpcuaEvents by lazy {

        val allEventAndAlarmIdentifiers = allEventAndAlarmClasses()

        val ns = BaseEventType::class.qualifiedName?.substringBeforeLast(".")
        sequence {
            allEventAndAlarmIdentifiers.forEach { identifierField ->

                try {
                    val node = identifierField.call() as? NodeId?
                    if (node != null) {
                        val cls = Class.forName("$ns.${identifierField.name}")
                        this.yield(identifierField.name to OpcuaEventData(identifierField.name, node, cls))
                    }
                } catch (_: Exception) {
                    // No further action needed as fields, which are initially filtered by name only, are not NodeID's can and should be ignored
                }
            }
        }.toMap()
    }

    private fun allEventAndAlarmClasses(): Sequence<KCallable<*>> {
        // regex to select event and alarm names by name
        val r = """^[a-zA-Z]*(Event|Alarm|Condition)Type${'$'}""".toRegex()
        val allEventAndAlarmIdentifiers = sequence {
            Identifiers::class.members.filter { member ->
                r.matches(member.name) && member.visibility == KVisibility.PUBLIC
            }.forEach {
                this.yield(it)
            }
        }
        return allEventAndAlarmIdentifiers
    }


    inner class OpcuaEventData(override val name: String, override val identifier: NodeId, private val eventClass: Class<*>) : Event {

        override val eventProperties: List<QualifiedName> by lazy {
            eventClass.declaredFields
                .filter { it.type.isAssignableFrom(QualifiedProperty::class.java) }
                .map {
                    val qp = (it.get(null) as QualifiedProperty<*>)
                    QualifiedName(OPC_UA_NAMESPACE, qp.browseName)
                }
        }

        override val properties by lazy { getEventClassProperties(eventClass) }


        private fun getEventClassProperties(cls: Class<*>): List<Pair<NodeId, QualifiedName>> {

            val event: OpcuaEventData = allOpcuaEvents.values.firstOrNull { it.eventClass == cls } ?: return emptyList()

            val properties = event.eventProperties.map {
                event.identifier to it
            }.toMutableList()

            if (cls.superclass != null) {
                properties.addAll(getEventClassProperties(cls.superclass))
            }

            for (i in cls.interfaces) {
                properties.addAll(getEventClassProperties(i))
            }
            return properties
        }

    }

}


