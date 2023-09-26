/*
 Copyright (c) 2021. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.service

/**
 * Interface for service running SFC core, protocol adapter or target IPC service process
 */
interface Service {
    /**
     * Start the service
     */
    suspend fun start()

    suspend fun stop()

    /**
     * Blocks until service is shut down
     */
    suspend fun blockUntilShutdown()

}