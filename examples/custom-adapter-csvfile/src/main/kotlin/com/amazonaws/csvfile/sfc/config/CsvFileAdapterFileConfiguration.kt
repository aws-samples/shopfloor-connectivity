// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.csvfile.sfc.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName
import java.io.File




@ConfigurationClass
class CsvFileAdapterFileConfiguration : Validate {

    @SerializedName(CONFIG_FILE_PATH)
    private var _path: String = ""
    val path: String
        get() = _path

    @SerializedName(CONFIG_DELIMITER)
    private var _delimiter: String = ""
    val delimiter: String
        get() = _delimiter

    @SerializedName(CONFIG_LINES_TO_SKIP)
    private var _linesToSkip: Int = 0
    val linesToSkip: Int
        get() = _linesToSkip

    private fun validateLinesToSkip() {
        ConfigurationException.check(
            (linesToSkip in 0..DEFAULT_SKIP_LIMIT),
            "${CONFIG_LINES_TO_SKIP} " +
                    "must be be set to 0=<..<=${DEFAULT_SKIP_LIMIT};" +
                    "FYI: A skip limit of 1 skips the first row of the CSV file (useful for header skipping);" +
                    "A skip limit of 9 would skip the first 9 rows of the CSV",
            CONFIG_LINES_TO_SKIP,
            this
        )
    }

    @SerializedName(CONFIG_MAX_ROWS_PER_READ)
    private var _maxRowsPerRead: Int = 1
    val maxRowsPerRead: Int
        get() = _maxRowsPerRead

    private fun validateMaxRows() {
        ConfigurationException.check(
            (maxRowsPerRead in 1..DEFAULT_FETCH_LIMIT),
            "${CONFIG_MAX_ROWS_PER_READ} " +
                    "must be be set to 1=<..<=${DEFAULT_FETCH_LIMIT}; " +
                    "FYI: A value of 1 gets the column value of the last row of the CSV; " +
                    "A value of 100 will return an Array with the column values of the last 100 rows...",
            CONFIG_MAX_ROWS_PER_READ,
            this
        )
    }



    private fun validateFileExists() {
        ConfigurationException.check(
            File(path).exists(),
            "${CONFIG_FILE_PATH} not found",
            CONFIG_FILE_PATH,
            this
        )
    }


    private fun validatePath() {
        ConfigurationException.check(
            !path.isNullOrEmpty(),
            "${CONFIG_FILE_PATH} can not be empty",
            CONFIG_FILE_PATH,
            this
        )

    }


    private fun validateDelimiter() {
        ConfigurationException.check(
            !delimiter.isNullOrEmpty(),
            "${CONFIG_DELIMITER} can not be empty",
            CONFIG_DELIMITER,
            this
        )
    }

    private var _validated = false
    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    @Throws(ConfigurationException::class)
    override fun validate() {
        if (validated) return
        validatePath()
        validateFileExists()
        validateDelimiter()
        validateMaxRows()
        validateLinesToSkip()
        validated = true
    }

    companion object {
        private const val DEFAULT_FETCH_LIMIT = 10000
        private const val DEFAULT_SKIP_LIMIT = 10
        private const val CONFIG_FILE_PATH = "FilePath"
        private const val CONFIG_DELIMITER = "Delimiter"
        private const val CONFIG_LINES_TO_SKIP = "LinesToSkip"
        private const val CONFIG_MAX_ROWS_PER_READ = "MaxRowsPerRead"

        private val default = CsvFileAdapterFileConfiguration()

        fun create(
            path: String = default._path,
            delimiter: String = default._delimiter,
            linesToSkip: Int = default._linesToSkip,
            maxRowsPerRead: Int = default._maxRowsPerRead):CsvFileAdapterFileConfiguration {

            val instance = CsvFileAdapterFileConfiguration()
            with(instance) {
                _path = path
                _delimiter = delimiter
                _linesToSkip = linesToSkip
                _maxRowsPerRead = maxRowsPerRead
            }
            return instance
        }

    }
}