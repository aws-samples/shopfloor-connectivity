
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.opcua

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName

interface Event {
    val name: String
    val identifier: NodeId
    val eventProperties: List<QualifiedName>
    val properties: List<Pair<NodeId, QualifiedName>>
}