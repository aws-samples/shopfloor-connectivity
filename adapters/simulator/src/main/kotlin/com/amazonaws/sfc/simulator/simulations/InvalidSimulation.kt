// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.simulator.simulations

class InvalidSimulation(val simulationName : String, val reason : String) : Simulation {
    override fun value(): Any? {
        return null
    }
}