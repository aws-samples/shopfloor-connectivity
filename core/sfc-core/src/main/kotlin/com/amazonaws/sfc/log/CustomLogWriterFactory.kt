
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.log

import com.amazonaws.sfc.config.InProcessConfiguration
import com.amazonaws.sfc.util.InstanceFactory

class CustomLogWriterFactory(config: InProcessConfiguration, logger: Logger) {
    private val factory = InstanceFactory<LogWriter>(config, logger)
    fun newLogWriterInstance(c: String) =
        factory.createInstance(c)
}


