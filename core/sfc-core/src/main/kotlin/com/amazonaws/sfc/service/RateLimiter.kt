
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.service

class RateLimiter(val ratePerSecond: Int) {
    private var lastTokenRefillTime = System.nanoTime()
    private var availableTokens = ratePerSecond

    fun tryAcquire(): Boolean {
        synchronized(this) {
            refillTokens()
            if (availableTokens > 0) {
                availableTokens--
                return true
            }
            return false
        }
    }

    private fun refillTokens() {
        val now = System.nanoTime()
        val timeElapsed = now - lastTokenRefillTime
        lastTokenRefillTime = now
        availableTokens += (timeElapsed * ratePerSecond / 1e9).toInt()
        if (availableTokens > ratePerSecond) {
            availableTokens = ratePerSecond
        }
    }
}