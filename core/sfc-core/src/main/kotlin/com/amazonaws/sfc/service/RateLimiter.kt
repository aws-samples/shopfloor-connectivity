/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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