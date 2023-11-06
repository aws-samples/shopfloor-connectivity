
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.secrets

interface SecretsStore<V, T> {
    fun getAll(): V
    fun saveAll(list: V)
    fun get(secretName: String, label: String? = null): T?
}