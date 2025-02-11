// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.j1939.protocol

import com.amazonaws.sfc.log.Logger
import java.io.File

class J1939Dbc(private val dbcFile : File, private val logger: Logger) {

    enum class PgnParseState {
        READ_PNG,
        READ_PNG_SIGNALS
    }

    // Lookup table from PGN tp J1939PGN
    val pgnByPgnMap = mutableMapOf<UInt, J1939PGN>()
    // Lookup table from PGN Id to its Can ID
    val pgnToCanIdIdMap = mutableMapOf<UInt, UInt>()
    // Lookup table from SPN Id to its J1939SPN
    val spnMap = mutableMapOf<UInt, MutableMap<UInt, J1939SPN>>()

    fun pgnByCanId(canId: UInt): J1939PGN? {
        val pgn = CanFrameIdentifier.extractPgn(canId)
        return pgnByPgnId(pgn)
    }

    fun pgnByPgnId(pgn: UInt?): J1939PGN? {
        return if (pgn!= null)pgnByPgnMap[pgn] else null
    }

    fun pgnByName(name: String): J1939PGN? {
        return pgnByPgnMap.values.find { it.name == name }
    }

    fun spnById(pgn: UInt, spnId: UInt): J1939SPN? {
        val messageId = pgnToCanIdIdMap[pgn] ?: return null
        return spnMap[messageId]?.get(spnId)
    }

    fun spnListForPgn(pgn: UInt): Map<UInt, J1939SPN>? {
        val messageId = pgnToCanIdIdMap[pgn] ?: return null
        return spnMap[messageId]
    }

    fun load() : J1939Dbc {

        val log = logger.getCtxLoggers(className, "loadDbcFile")

        log.info("Loading DBC file ${dbcFile.absoluteFile.name}")

        val fileReader = dbcFile.bufferedReader()

        pgnByPgnMap.clear()
        pgnToCanIdIdMap.clear()
        spnMap.clear()


        try {

            var line = fileReader.readLine()?.trim()

            var currentPgn: J1939PGN? = null
            var parsingState: PgnParseState = PgnParseState.READ_PNG

            while (line != null) {

                when (parsingState) {
                    PgnParseState.READ_PNG -> {
                        if (line.startsWith(PNG_PREFIX)) {
                            // PGN found in file
                            currentPgn = parseDbcPgn(line)
                            // If PGN could be parsed start reading signals for the PGN
                            parsingState = if (currentPgn != null) PgnParseState.READ_PNG_SIGNALS else PgnParseState.READ_PNG
                        }
                    }

                    PgnParseState.READ_PNG_SIGNALS -> {

                        if (line.startsWith(PNG_SIGNAL_PREFIX)) {
                            // Signal  line for PGN found, if it could be parsed continue reading SPNs for this PGN or else start looking for next PGN
                            parsingState = if (parseDbcPgnSignal(line, currentPgn) != null) PgnParseState.READ_PNG_SIGNALS else PgnParseState.READ_PNG
                        } else {
                            // Line was not a Signal, start looking for next PGN
                            parsingState = PgnParseState.READ_PNG
                        }

                        // Switched from READ_SPN to READ_PGN state, process PGN and its SPNs
                        if (parsingState == PgnParseState.READ_PNG) {
                            if (currentPgn != null) {
                                val pgnFromCanId = CanFrameIdentifier.extractPgn(currentPgn.canId)
                                pgnByPgnMap[pgnFromCanId] = currentPgn
                                pgnToCanIdIdMap[pgnFromCanId] = currentPgn.canId
                            }
                            currentPgn = null
                        }
                    }
                }

                // Looking for SPN Lines
                if (line.startsWith(SPN_PREFIX)) {
                    parsingState = if (parseDbcSpn(line) != null) PgnParseState.READ_PNG_SIGNALS else PgnParseState.READ_PNG
                    val spn = parseDbcSpn(line)

                    // build a map with an entry for the PGN the SPN is a part of, the entry value is a map containing all Signals for the PGN
                    if (spn != null) {
                        var spnForPgnMap = spnMap[spn.messageId]
                        if (spnForPgnMap == null) {
                            spnForPgnMap = mutableMapOf()
                            spnMap[spn.messageId] = spnForPgnMap
                        }
                        spnForPgnMap[spn.id] = spn
                    }
                }

                line = fileReader.readLine()?.trim()
            }
        } catch (e: Exception) {
            log.error("Error loading DBC file ${dbcFile.absoluteFile.name}, $e")
            throw e
        } finally {
            fileReader.close()
        }

        log.info("Loaded ${pgnByPgnMap.size} PGN and ${spnMap.values.sumOf { it.size }} SPN items from ${dbcFile.name}")

        return this
    }

    private fun parseDbcSpn(line: String): J1939SPN? {
        val match = spnPattern.find(line)
        if (match == null) return null
        return try {
            J1939SPN(
                messageId = match.groupValues[1].toUInt(),
                name = match.groupValues[2],
                id = match.groupValues[3].toUInt()
            )
        } catch (e: Exception) {
            logger.getCtxLoggers(className, "parseDbcSpn").error("Error parsing SPN from line \"line\", $e")
            null
        }
    }

    private fun parseDbcPgnSignal(line: String, png: J1939PGN?): J1939Signal? {
        val match = pgnSignalPattern.find(line)
        if (match == null) return null
        return try {
            val signal = J1939Signal(
                name = match.groupValues[1],
                startBit = match.groupValues[2].toInt(),
                length = match.groupValues[3].toInt(),
                byteOrder = if (match.groupValues[4] == "@1") ByteOrder.INTEL else ByteOrder.MOTOROLA,
                valueType = if (match.groupValues[5] == "+") ValueType.UNSIGNED else ValueType.SIGNED,
                factor = match.groupValues[6].toDouble(),
                offset = match.groupValues[7].toDouble(),
                minimum = match.groupValues[8].toDouble(),
                maximum = match.groupValues[9].toDouble(),
                // Not used for now
                // unit = match.groupValues[10],
                // receivers = match.groupValues[11].split(",").map { it.trim() }
            )
            png?.addSignal(signal)
            signal
        } catch (e: Exception) {
            logger.getCtxLoggers(className, "parseDbcPgnSignal").error("Error parsing signal from line \"line\", $e")
            null
        }

    }

    private fun parseDbcPgn(line: String): J1939PGN? {

        val log = logger.getCtxLoggers(className, "parseDbcPgn")

        val match = pgnPattern.find(line) ?: return null

        return try {

            val pgn = J1939PGN(
                name = match.groupValues[2],
                canId = match.groupValues[1].toUInt(),
                // Not used for now
                // dlc = match.groupValues[3].toInt(),
                // source = match.groupValues[4]
            )

            return pgn

        } catch (e: Exception) {
            log.error("Error parsing PGN from line \"line\", $e")
            null
        }
    }


    companion object {

        private val className = this::class.java.name

        const val PNG_PREFIX = "BO_"
        private val pgnPattern = """BO_\s+
        (\d+)\s+  # id
        (\w+)     # name
        :\s*
        (\d+)     # dlc
        \s+
        (\w+)     # souce""".trimIndent().toRegex(RegexOption.COMMENTS)


        private const val PNG_SIGNAL_PREFIX = "SG_"
        private val pgnSignalPattern = """SG_\s+
        (\S+)\s+  # Signal name
        :\s*
        (\d+)\|   # Start bit
        (\d+)     # Length
        (@[01])   # Byte order (0 Motorola, 1=Intel)
        ([+-])    # Value type (plus =unsigned, minus =signed)
        \s*
        \(
        ([-+]?[0-9]*\.?[0-9]+),  # Factor
        ([-+]?[0-9]*\.?[0-9]+)   # Offset
        \)
        \s*
        \[
        ([-+]?[0-9]*\.?[0-9]+)\| # Minimum
        ([-+]?[0-9]*\.?[0-9]+)   # Maximum
        \]
        \s*
        "([^"]*)" # Unit
        \s*
        (\S.*)    # Receivers """.trimIndent().toRegex(RegexOption.COMMENTS)

        const val SPN_PREFIX = "BA_ \"SPN\" SG_"
        private val spnPattern = """
        BA_\s"SPN"\sSG_\s
        (\d+)   # message ID
        \s
        (\w+)   # name
        \s
        (\d+)   # spn ID
        ;""".trimIndent().toRegex(RegexOption.COMMENTS)

    }
}

