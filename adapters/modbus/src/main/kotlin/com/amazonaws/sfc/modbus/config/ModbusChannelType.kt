
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.modbus.config

import com.google.gson.annotations.SerializedName

/**
 * Modbus device source channel types
 */
enum class ModbusChannelType {
    @SerializedName("Coil")
    COIL,

    @SerializedName("DiscreteInput")
    DISCRETE_INPUT,

    @SerializedName("HoldingRegister")
    HOLDING_REGISTER,

    @SerializedName("InputRegister")
    INPUT_REGISTER
}