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

enum class AddressNamedSubElement : AddressSubElement {


    ACC {
        override val bitOffset = null
        override val wordOffset = 4
    },

    BASE {
        override val bitOffset: Int? = null
        override val mask: Short = 0x0F
        override val shr: Short = 8
    },

    CD {
        override val bitOffset = 14
    },

    CU {
        override val bitOffset = 15
    },

    DN {
        override val wordOffset = 0
        override val bitOffset = 13
    },

    EM {
        override val bitOffset = 12
    },

    EN {
        override val wordOffset = 0
        override val bitOffset = 15
    },

    ER {
        override val bitOffset = 11
    },

    EU {
        override val bitOffset = 14
    },

    FD {
        override val bitOffset = 8
    },

    IN {
        override val bitOffset = 9
    },

    LEN {
        override val bitOffset = null
        override val wordOffset = 2
    },

    OV {
        override val bitOffset = 12
    },

    POS {
        override val bitOffset = null
        override val wordOffset = 4
    },

    PRE {
        override val bitOffset = null
        override val wordOffset = 2
    },

    TT {
        override val wordOffset = 0
        override val bitOffset = 14
    },

    UA {
        override val bitOffset = 10
    },

    UL {
        override val bitOffset = 10
    },

    UN {
        override val bitOffset = 11
    };

    companion object {
        fun parse(s: String): AddressSubElement {
            val field: String = s.uppercase()
            return entries.firstOrNull { field == it.name }
                ?: throw AddressException("\"$s\" field name is unknown")
        }
    }

}