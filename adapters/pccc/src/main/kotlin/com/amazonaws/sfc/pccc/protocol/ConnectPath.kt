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


class ConnectPath(backplane: Byte = 0x01, slot: Byte = 0x00) {
    val bytes = byteArrayOf(0x01, 0x00, backplane, slot)
}