/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */

package com.amazonaws.sfc.ads.protocol

enum class DataType {
    APP_INFO,
    BIGTYPE,
    BOOL,
    BYTE,
    DATE,
    DATE_AND_TIME,
    DINT,
    DWORD,
    INT,
    LIB_VERSION,
    LINT,
    LREAL,
    LTIME,
    OTCID,
    REAL,
    SINT,
    STRING,
    TASK_SYSTEM_INFO,
    TIME,
    TIME_OF_DAY,
    UDINT,
    UINT,
    ULINT,
    USINT,
    VERSION,
    VOID,
    WORD,
    WSTRING;

    companion object {

        fun fromString(name : String): DataType  = when(name.split(' ').last().split("(")[0].trim()){
            "BIGTYPE" -> BIGTYPE
            "BOOL" -> BOOL
            "BYTE" -> BYTE
            "DATE" -> DATE
            "DATE_AND_TIME" -> DATE_AND_TIME
            "DINT" -> DINT
            "DWORD" -> DWORD
            "INT" -> INT
            "LINT" -> LINT
            "LREAL" -> LREAL
            "LTIME"-> LTIME
            "OTCID" -> OTCID
            "PLC.PlcAppSystemInfo" -> APP_INFO
            "PLC.PlcTaskSystemInfo" -> TASK_SYSTEM_INFO
            "REAL" -> REAL
            "SINT" -> SINT
            "STRING" -> STRING
            "ST_LibVersion" -> LIB_VERSION
            "TASK_SYSTEM_INFO" -> TASK_SYSTEM_INFO
            "TIME" -> TIME
            "TIME_OF_DAY" -> TIME_OF_DAY
            "UDINT" -> UDINT
            "UINT" -> UINT
            "ULINT" -> ULINT
            "USINT" -> USINT
            "VERSION" -> VERSION
            "WORD" -> WORD
            "WSTRING" -> WSTRING
            else -> VOID
        }
    }
}



