/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.awslambda

import com.amazonaws.sfc.services.AwsServicePermissions
import software.amazon.awssdk.services.lambda.model.InvokeRequest
import software.amazon.awssdk.services.lambda.model.InvokeResponse


// Abstraction for testing with mock client
@AwsServicePermissions("lambda", ["InvokeFunction"])
interface AwsLambdaClient {
    fun invoke(invokeRequest: InvokeRequest): InvokeResponse
    fun close()
}