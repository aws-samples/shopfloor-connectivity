
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.data

/**
 * Result data from reading from multiple sources
 * @property map Mapping<String, SourceReadResult> Mapping of values, each entry contains a map of channel values for a source
 * @constructor
 */
class ReadResult(private val map: Map<String, SourceReadResult>) :
        Map<String, SourceReadResult> by map {

    /**
     * Makes a copy of the instance
     * @return ReadResul
     */
    fun copy(): ReadResult {
        return ReadResult(map + mapOf())
    }

    /**
     * Makes of a copy of all values, which are SourceReadSuccess instances.
     * @return Mapping<String, SourceReadSuccess> Mapping containing all successfully read values indexed by source
     */
    fun copyValues(): Map<String, SourceReadSuccess> {
        return map.filter { it.value is SourceReadSuccess && (it.value as SourceReadSuccess).values.isNotEmpty() }.map {
            it.key to it.value as SourceReadSuccess
        }.toMap() + mapOf()
    }
}