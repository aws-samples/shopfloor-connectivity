// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.opcuatarget.config

open class NodeConfigurationMap<T:BaseNodeConfiguration> : Map<String, T> {

    protected open var nodes : Map<String, T>? = null

    override val entries: Set<Map.Entry<String, T>>
        get() = nodes?.entries?: emptySet()
    override val keys: Set<String>
        get() = nodes?.keys?: emptySet()
    override val size: Int
        get() = nodes?.size?:0
    override val values: Collection<T>
        get() = nodes?.values?: emptySet()

    override fun isEmpty(): Boolean {
        return nodes?.isEmpty()?:true
    }

    override fun get(key: String): T? {
        return nodes?.get(key)
    }

    override fun containsValue(value: T): Boolean {
        return nodes?.containsValue(value)==true
    }

    override fun containsKey(key: String): Boolean {
        return nodes?.containsKey(key) == true
    }


}