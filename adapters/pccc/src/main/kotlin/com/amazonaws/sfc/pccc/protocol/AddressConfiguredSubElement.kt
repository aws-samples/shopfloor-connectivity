/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */

package com.amazonaws.sfc.pccc.protocol

data class AddressConfiguredSubElement(
    // the element in B3:0/1 this would be 0
    override val element: Int = 0,
    // the bit index, for B3:0/1 this would be 1
    override val bitOffset: Int? = null,
    // the word offset of a sub element in an item, for C5:0.ACC this is mapped to 4 as the ACC field is in the 4th word of the counter
    override val wordOffset: Int = 0
) : AddressSubElement