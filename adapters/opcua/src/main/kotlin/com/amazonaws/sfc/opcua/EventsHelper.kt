
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.opcua

import org.eclipse.milo.opcua.stack.core.serialization.SerializationContext
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant

interface EventsHelper {
    val allEventClassNames: List<String>
    val allEventClassIdentifiers: List<NodeId?>
    val allEventClasses: List<Pair<String, NodeId?>>
    fun findEvent(name: String?): Event?
    fun findEvent(identifier: NodeId?): Event?

    fun isKnownEvent(name: String): Boolean
    fun variantPropertiesToMap(eventVariantValues: Array<Variant>,
                               eventProperties: List<Pair<NodeId, QualifiedName>>,
                               context: SerializationContext): Map<String, Any>


}