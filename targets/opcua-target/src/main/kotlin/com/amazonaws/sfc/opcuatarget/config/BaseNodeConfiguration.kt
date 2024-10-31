// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.opcuatarget.config

import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.google.gson.JsonObject
import org.eclipse.milo.opcua.stack.core.UaRuntimeException
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId
import java.lang.ref.WeakReference

open class BaseNodeConfiguration(namespaceIndex : Int, val id: String, browseName: String?, displayName: String?, description: String? = null) : Validate {



    protected var _nameSpaceIndex: Int = namespaceIndex
    open var nameSpaceIndex: Int
        get() = _nameSpaceIndex
        set(value) {
            _nameSpaceIndex = value
        }

    @Transient
    private var _parent: WeakReference<ParentNode?>? = null

    var parent: ParentNode?
        get() = _parent?.get()
        set(value) {
            _parent?.clear()
            _parent = WeakReference(value)
        }

    val path : String
        get() = if (parent != null) "${parent?.path}/$id" else id


    private var _displayName: String? = displayName
    val displayName: String by lazy {
        _displayName ?: id
    }

    private var _browseName: String? = browseName
    val browseName: String by lazy {
        _browseName ?: id
    }

    private var _description: String? = description
    val description: String?
        get() = _description

    private var _baseId: String? = id

    val nodeID: NodeId?
        get()=
        try {
            val nodeStr = when (_baseId) {
                is String -> {
                    val i = (_baseId as String).split(".").first().toIntOrNull()
                    when {
                        // i=
                        i != null -> "ns=$nameSpaceIndex;i=$i"
                        // g=
                        (guidRegex.matchEntire((_baseId as String).trim()) != null) -> "ns=$nameSpaceIndex;g=$_baseId"
                        // s=
                        else -> "ns=$nameSpaceIndex;s=$_baseId"
                    }
                }

                else -> "ns=$nameSpaceIndex;s=${_baseId.toString()}"
            }
            NodeId.parse(
                nodeStr)
        } catch (e: UaRuntimeException) {
            null
        }

    private var _validated = false

    /**
     * Validates configuration.
     * @throws ConfigurationException
     */
    override fun validate() {

        if (validated) return

        ConfigurationException.check(
            id .isNotEmpty(),
            "$CONFIG_NODE_ID \"$id\" must be set",
            CONFIG_NODE_ID,
            this)

        ConfigurationException.check(
            nodeID != null,
            "$CONFIG_NODE_ID \"$id\" is not a valid node id",
            CONFIG_NODE_ID,
            this
        )

        validated = true

    }

    override var validated: Boolean
        get() = _validated
        set(value) {
            _validated = value
        }


    companion object {
        @JvmStatic
        fun fromJson(key: String, json: JsonObject): BaseNodeConfiguration {

            return BaseNodeConfiguration(
                namespaceIndex = 0,
                id = json.asJsonObject?.get(CONFIG_NODE_ID)?.asString?:key,
                displayName = json.asJsonObject?.get(CONFIG_NODE_DISPLAY_NAME)?.asString,
                browseName =  json.asJsonObject?.get(CONFIG_NODE_BROWSE_NAME)?.asString,
                description =  json.asJsonObject?.get(CONFIG_NODE_DESCRIPTION)?.asString
            )
        }

        private const val CONFIG_NODE_DISPLAY_NAME = "DisplayName"
        private const val CONFIG_NODE_BROWSE_NAME = "BrowseName"
        private const val CONFIG_NODE_DESCRIPTION = "Description"
        private const val CONFIG_NODE_ID = "Id"

        private val guidRegex = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}".toRegex()
    }


}