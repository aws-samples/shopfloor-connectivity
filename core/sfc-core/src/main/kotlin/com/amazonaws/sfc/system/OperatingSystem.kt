
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.system

fun isWindowsSystem() = System.getProperty("os.name").startsWith("Windows")