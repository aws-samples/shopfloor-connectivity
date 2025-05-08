// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

plugins {
    id("java")
}

buildscript {
    val version = (gradle as ExtensionAware).extensions.extraProperties.get("versionFromGit") as String
    extra.apply {
        // SET GLOBAL SFC RELEASE VERSION HERE
        // --> applied to sfc-core, sfc-ipc, sfc-main & sfc-metrics <--
        set("sfc_release", version)
    }
}