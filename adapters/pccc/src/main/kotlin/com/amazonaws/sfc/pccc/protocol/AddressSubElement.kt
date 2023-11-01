/*
 *
 *
 *     Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *      Licensed under the Amazon Software License (the "License"). You may not use this file except in
 *      compliance with the License. A copy of the License is located at :
 *
 *      http://aws.amazon.com/asl/
 *
 *      or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 *      language governing permissions and limitations under the License.
 *
 *
 *
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
    val mask : Short?  get() = null
    val shr : Short?  get() = null
    val shl : Short?  get() = null
}

