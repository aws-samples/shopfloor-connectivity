/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */

package com.amazonaws.sfc.pccc.protocol

interface AddressSubElement {
    // the element in B3:0/1 this would be 0
    val element: Int get() = 0

    // the bit index, for B3:0/1 this would be 1
    val bitOffset: Int?

    // the word offset of a sub element in an item, for C5:0.ACC this is mapped to 4 as the ACC field is in the 4th word of the counter
    val wordOffset: Int get() = 0

    // mask and shifts to apply to get a sub element from a word value
    val mask: Short? get() = null
    val shr: Short? get() = null
    val shl: Short? get() = null
}

