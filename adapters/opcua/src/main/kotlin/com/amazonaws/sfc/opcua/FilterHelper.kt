
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.opcua

import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.opcua.config.OpcuaConfiguration
import com.amazonaws.sfc.util.LookupCacheHandler
import kotlinx.coroutines.runBlocking
import org.eclipse.milo.opcua.sdk.client.OpcUaClient
import org.eclipse.milo.opcua.sdk.client.model.types.objects.BaseEventType
import org.eclipse.milo.opcua.sdk.client.subscriptions.EventFilterBuilder
import org.eclipse.milo.opcua.stack.core.AttributeId
import org.eclipse.milo.opcua.stack.core.Identifiers
import org.eclipse.milo.opcua.stack.core.types.builtin.ExtensionObject
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant
import org.eclipse.milo.opcua.stack.core.types.enumerated.FilterOperator
import org.eclipse.milo.opcua.stack.core.types.structured.ContentFilter
import org.eclipse.milo.opcua.stack.core.types.structured.ContentFilterElement
import org.eclipse.milo.opcua.stack.core.types.structured.LiteralOperand
import org.eclipse.milo.opcua.stack.core.types.structured.SimpleAttributeOperand


class FilterHelper(private val client: OpcUaClient,
                   private val sourceID: String,
                   private val opcuaConfiguration: OpcuaConfiguration,
                   private val logger: Logger) {

    private val className = this::class.simpleName.toString()

    private val filters = LookupCacheHandler<String, ExtensionObject?, Nothing>(


        supplier = { name ->
            val log = logger.getCtxLoggers(className, "filterSupplier")

            // Lookup event, if specified event was unknown then replace it by the default type
            val event = serverEvents.findEvent(if (name == UNKNOWN_EVENT_TYPE) DEFAULT_EVENT_TYPE else name)
            if (event == null) {
                log.error("Event type \"$name\" for source \"$sourceID\" does not contain any valid event names, known types are ${serverEvents.allEventClasses}")
                null
            } else {
                // add properties of all selected event types to select clause
                val filterBuilder = EventFilterBuilder()

                // select  properties of the event type
                event.properties.forEach { (propertyNodeId, propertyName) ->
                    filterBuilder.select(propertyNodeId, propertyName)
                }

                // where clause of filter for specific event type except when event type was unknown and events of any type are collected
                if (name != UNKNOWN_EVENT_TYPE) {
                    filterBuilder.where(contentFilterForEventType(event))
                }


                val filter = filterBuilder.build()
                log.trace("Created filter for event \"name for source \"$sourceID\", $filter")
                ExtensionObject.encode(client.serializationContext, filter)
            }

        })

    private fun contentFilterForEventType(event: Event) =
        ContentFilter(
            arrayOf(
                ContentFilterElement(
                    FilterOperator.Equals, arrayOf<ExtensionObject>(
                        eventTypeValueProperty(),
                        ExtensionObject.encode(
                            client.serializationContext,
                            LiteralOperand(Variant(event.identifier))
                        )
                    )
                )
            )
        )


    private fun eventTypeValueProperty(): ExtensionObject = ExtensionObject.encode(
        client.serializationContext,
        SimpleAttributeOperand(
            Identifiers.BaseEventType, arrayOf(QualifiedName(0, BaseEventType.EVENT_TYPE.browseName)),
            AttributeId.Value.uid(),
            null
        )
    )


    operator fun get(eventName: String): ExtensionObject? {

        return runBlocking {
            filters.getItemAsync(eventName).await()
        }
    }


    private val serverEvents: EventsHelper by lazy {
        OpcuaProfileEventsHelper.createHelper(serverProfile)
    }

    private val serverProfile by lazy {
        val source = opcuaConfiguration.sources[sourceID]
        val adapter = opcuaConfiguration.protocolAdapters[source?.protocolAdapterID]
        val server = adapter?.opcuaServers?.get(source?.sourceAdapterOpcuaServerID)
        adapter?.serverProfiles?.get(server?.serverProfile)
    }

    companion object {
        const val DEFAULT_EVENT_TYPE = "BaseEventType"
        const val UNKNOWN_EVENT_TYPE = "*"
    }


}