/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.s7.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@ConfigurationClass
class S7ControllerConfiguration : Validate {
    @SerializedName(CONFIG_ADDRESS)
    private var _address: String = ""

    val address: String
        get() = if (_address.startsWith(S7_PROTOCOL)) _address.substring(S7_PROTOCOL.length) else _address

    @SerializedName(CONFIG_LOCAL_RACK)
    private var _localRack: Int = DEFAULT_LOCAL_RACK

    val localRack: Int
        get() = _localRack

    @SerializedName(CONFIG_LOCAL_SLOT)
    private var _localSlot: Int = DEFAULT_LOCAL_SLOT

    val localSlot: Int
        get() = _localSlot

    @SerializedName(CONFIG_REMOTE_RACK)
    private var _remoteRack: Int = DEFAULT_REMOTE_RACK

    val remoteRack: Int
        get() = _remoteRack

    @SerializedName(CONFIG_REMOTE_SLOT)
    private var _remoteSlot: Int = DEFAULT_REMOTE_SLOT

    val remoteSlot: Int
        get() = _remoteSlot

    @SerializedName(CONFIG_PDU_SIZE)
    private var _pduSize: Int = DEFAULT_PDU_SIZE

    val pduSize: Int
        get() = _pduSize


    @SerializedName(CONFIG_MAX_AMQ_CALLER)
    private var _maxAmqCaller: Int = DEFAULT_MAX_AMQ_CALLER

    val maxAmqCaller: Int
        get() = _maxAmqCaller


    @SerializedName(CONFIG_MAX_AMQ_CALLEE)
    private var _maxAmqCallee: Int = DEFAULT_MAX_AMQ_CALLEE

    val maxAmqCallee: Int
        get() = _maxAmqCallee


    @SerializedName(CONFIG_CONTROLLER_TYPE)
    private var _controllerType: S7PlcControllerType = S7PlcControllerType.UNKNOWN_CONTROLLER_TYPE

    val controllerType: S7PlcControllerType
        get() = _controllerType

    @SerializedName(CONFIG_READ_TIMEOUT)
    private var _readTimeout: Int = DEFAULT_READ_TIMEOUT

    val readTimeout: Duration
        get() = _readTimeout.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_READ_ONE_FIELD_AT_A_TIME)
    private var _readSingleField: Boolean = false

    val readSingleField: Boolean
        get() = _readSingleField

    @SerializedName(CONFIG_CONNECT_TIMEOUT)
    private var _connectTimeout: Int = DEFAULT_CONNECT_TIMEOUT

    val connectTimeout: Duration
        get() = _connectTimeout.toDuration(DurationUnit.MILLISECONDS)

    private var _validated = false
    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    override fun validate() {
        if (validated) return
        validateAddress()
    }

    private fun validateAddress() =
        ConfigurationException.check(
            (address.isNotEmpty()),
            "$CONFIG_ADDRESS of S7 controller can not be empty",
            CONFIG_ADDRESS,
            this
        )


    companion object {

        private const val S7_PROTOCOL = "s7://"
        private const val CONFIG_ADDRESS = "Address"

        private const val CONFIG_LOCAL_RACK = "LocalRack"
        private const val DEFAULT_LOCAL_RACK = 1
        private const val CONFIG_LOCAL_SLOT = "LocalSlot"
        private const val DEFAULT_LOCAL_SLOT = 1

        private const val CONFIG_REMOTE_RACK = "RemoteRack"
        private const val DEFAULT_REMOTE_RACK = 0

        private const val CONFIG_REMOTE_SLOT = "RemoteSlot"
        private const val DEFAULT_REMOTE_SLOT = 0

        private const val CONFIG_PDU_SIZE = "PduSize"
        private const val DEFAULT_PDU_SIZE = 1024

        private const val CONFIG_MAX_AMQ_CALLER = "MaxAmqCaller"
        private const val DEFAULT_MAX_AMQ_CALLER = 8

        private const val CONFIG_MAX_AMQ_CALLEE = "MaxAmqCallee"
        private const val DEFAULT_MAX_AMQ_CALLEE = 8

        private const val CONFIG_CONTROLLER_TYPE = "ControllerType"

        private const val CONFIG_READ_TIMEOUT = "ReadTimeout"
        private const val DEFAULT_READ_TIMEOUT = 1000

        private const val CONFIG_CONNECT_TIMEOUT = "ConnectTimeout"
        private const val DEFAULT_CONNECT_TIMEOUT = 10000

        private const val CONFIG_READ_ONE_FIELD_AT_A_TIME = "ReadPerSingleField"

        private val default = S7ControllerConfiguration()

        fun create(address: String = default._address,
                   localRack: Int = default._localRack,
                   localSlot: Int = default._localSlot,
                   remoteRack: Int = default._remoteRack,
                   remoteSlot: Int = default._remoteSlot,
                   pduSize: Int = default._pduSize,
                   maxAmqCaller: Int = default._maxAmqCaller,
                   maxAmqCallee: Int = default._maxAmqCallee,
                   controllerType: S7PlcControllerType = default._controllerType,
                   readTimeout: Int = default._readTimeout,
                   readSingleField: Boolean = default._readSingleField,
                   connectTimeout: Int = default._connectTimeout): S7ControllerConfiguration {

            val instance = S7ControllerConfiguration()
            with(instance) {
                _address = address
                _localRack = localRack
                _localSlot = localSlot
                _remoteRack = remoteRack
                _remoteSlot = remoteSlot
                _pduSize = pduSize
                _maxAmqCaller = maxAmqCaller
                _maxAmqCallee = maxAmqCallee
                _controllerType = controllerType
                _readTimeout = readTimeout
                _connectTimeout = connectTimeout
                _readSingleField = readSingleField
            }
            return instance
        }


    }

}
