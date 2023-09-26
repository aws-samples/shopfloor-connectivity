/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.log

import com.amazonaws.sfc.config.InProcessConfiguration
import com.amazonaws.sfc.util.InstanceFactory

class CustomLogWriterFactory(config: InProcessConfiguration, logger: Logger) {
    private val factory = InstanceFactory<LogWriter>(config, logger)
    fun newLogWriterInstance(c: String) =
        factory.createInstance(c)
}


