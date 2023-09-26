/*
 *
 *     Copyright (c) 2021. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *     Licensed under the Amazon Software License (the "License"). You may not use this  file except in  compliance with the License. A copy of the License is located at :
 *
 *       http://aws.amazon.com/asl/
 *
 *     or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific language governing permissions and limitations under the License.
 *
 */

@file:Suppress("unused")

package com.amazonaws.sfc.modbus.protocol

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Address represents an address in a remote device (0x0000 to 0xFFFF)
 */

typealias Address = UShort

/**
 * Extension method to convert int to address type
 * @receiver Int
 * @return Address
 */
fun Int.toAddress(): Address = this.toUShort()

/**
 * Extension method to convert uint to address type
 * @receiver UInt
 * @return Address
 */
fun UInt.toAddress(): Address = this.toUShort()

/**
 * Extension method to convert short to address type
 * @receiver Short
 * @return Address
 */
fun Short.toAddress(): Address = this.toUShort()

/**
 * DiscreteValue represents a discrete value in a modbus device
 **/
typealias DiscreteValue = Boolean

/**
 * FunctionCode represents one of the Modbus functions
 **/
typealias FunctionCode = UByte

/**
 * RegisterValue represents a Modbus register value
 **/
typealias RegisterValue = UShort

/**
 * Extension method to convert Int to register value type
 * @receiver Int
 * @return RegisterValue
 */
fun Int.toRegisterValue(): RegisterValue = this.toUShort()

/**
 * TransactionID represents the ID of a transaction in a modbus device request/response
 */
typealias TransactionID = UShort

/**
 * Extension method to convert Unsigned Byte value to transaction ID
 * @receiver UByte
 * @return TransactionID
 */
fun UByte.toTransactionID(): TransactionID = this.toUShort()

/**
 * Extension method to convert Int value to transaction ID
 * @receiver Int
 * @return TransactionID
 */
fun Int.toTransactionID(): TransactionID = this.toUShort()

/**
 * ErrorCode represents the code of an error thrown by a Modbus function
 */
typealias ErrorCode = UByte

/**
 * Modbus request payload
 */
typealias Payload = UByteArray

/**
 * ExceptionCode represents the exception code in function errors
 */
typealias ExceptionCode = UByte

/**
 * Extension method to send a request to modbus device
 * @receiver ModbusHandler The modbus handler for the device
 * @param request Request Request to send
 */
suspend fun ModbusHandler.send(request: Request) = this.requests.send(request)

/**
 * Extension method to receive a response from a modbus device
 * @receiver ModbusHandler ModbusHandler The modbus handler for the device
 * @return Response Received response
 */
suspend fun ModbusHandler.receive(): Response = this.responses.receive()

/**
 * DeviceID represents the ID of a remote device
 */
typealias DeviceID = UByte

/**
 * Modbus protocol helpers and constants
 */
object Modbus {

    /**
     * DiscreteHigh represents an ON value
     */
    const val DISCRETE_ON: DiscreteValue = true

    /**
     * DiscreteLow represents an OFF value
     */
    const val DISCRETE_OFF: DiscreteValue = false

    /**
     *  FunctionReadCoils is used to read from  1 to 2000 contiguous status of coils in a modbus device.
     *  See also Modbus Application Protocol V1.1b3, Section 6.1
     */
    const val FUNCTION_CODE_READ_COILS: FunctionCode = 0x01u

    /**
     * FunctionReadDiscreteInputs is used to read from  1 to 2000 contiguous status of discreet inputs in a modbus device.
     * See also Modbus Application Protocol V1.1b3, Section 6.2
     */
    const val FUNCTION_CODE_READ_DISCRETE_INPUTS: FunctionCode = 0x02u

    /**
     * FunctionReadHoldingRegisters is used to read the contents of a contiguous block of holding registers in a modbus device.
     * See also Modbus Application Protocol V1.1b3, Section 6.3
     */
    const val FUNCTION_CODE_READ_HOLDING_REGISTERS: FunctionCode = 0x03u

    /**
     * FunctionReadInputRegisters is used to read from 1 to 125 contiguous input registers in a modbus device.
     * See also Modbus Application Protocol V1.1b3, Section 6.4
     */
    const val FUNCTION_CODE_READ_INPUT_REGISTERS: FunctionCode = 0x04u

    /**
     * FunctionWriteSingleCoil is used to write a single output to either ON or OFF in a modbus device.
     * See also Modbus Application Protocol V1.1b3, Section 6.5
     */
    const val FUNCTION_CODE_WRITE_SINGLE_COIL: FunctionCode = 0x05u

    /**
     * FunctionWriteSingleRegister is used to write a single holding register in a modbus device.
     * See also Modbus Application Protocol V1.1b3, Section 6.6
     */
    const val FUNCTION_CODE_WRITE_SINGLE_REGISTER: FunctionCode = 0x06u

    /**
     * FunctionWriteMultipleCoils is used to force each coil in a sequence of coils to either ON or OFF in a modbus device.
     * See also Modbus Application Protocol V1.1b3, Section 6.11
     */
    const val FUNCTION_CODE_WRITE_MULTIPLE_COILS: FunctionCode = 0x0Fu

    /**
     * FunctionWriteMultipleRegisters is used to write a block of contiguous registers (1 to 123 registers) in a modbus device.
     * See also Modbus Application Protocol V1.1b3, Section 6.12
     */
    const val FUNCTION_CODE_WRITE_MULTIPLE_REGISTERS: FunctionCode = 0x10u

    /**
     * FunctionMaskWriteRegister is used to modify the contents of a specified holding register using a combination of
     * an AND mask, an OR mask, and the register's current contents. The function can be used to set or clear
     * individual bits in the register.
     * See also Modbus Application Protocol V1.1b3, Section 6.16
     */
    const val FUNCTION_CODE_MASK_WRITE_REGISTER: FunctionCode = 0x16u

    /**
     * ErrorValueOffset is added to a function code to represent an error
     */
    private const val ERROR_VALUE_OFFSET: UByte = 0x80u

    /**
     * FunctionReadCoilsError represents and error while reading a coil of a remote device.
     */
    val FunctionReadCoilsError: ErrorCode = FUNCTION_CODE_READ_COILS or ERROR_VALUE_OFFSET

    /**
     * MaxReadCoils is the greatest number of coils that can be read in a remote device as define by the
     * Modbus Application Protocol V1.1b3
     */
    const val MAX_READ_COILS_INPUTS = 2000

    /**
     * MaxReadRegisters is the greatest number of registers that can be read in a remote device as define by the
     * Modbus Application Protocol V1.1b3
     */
    const val MAX_READ_REGISTERS = 125

    /**
     * MaxOutputRegisters is the greatest number of registers that can be written in a remote device as define by the
     * Modbus Application Protocol V1.1b3
     */
    const val MAX_OUTPUT_REGISTERS = 123

    /**
     * MaxOutputCoils is the greatest number of coils that can be written in a remote device as define by the
     * Modbus Application Protocol V1.1b3
     */
    const val MAX_OUTPUT_COILS = 125

    // ExceptionIllegalFunction is the exception thrown if the requested function code is not supported
    private const val EXCEPTION_ILLEGAL_FUNCTION: ExceptionCode = 0x01u

    // ExceptionIllegalAddress is thrown if the requested address is out of the valid range
    private const val EXCEPTION_ILLEGAL_ADDRESS: ExceptionCode = 0x02u

    // ExceptionIllegalDataValue is thrown if the requested data value is not supported
    private const val EXCEPTION_ILLEGAL_DATA_VALUE: ExceptionCode = 0x03u

    // ExceptionIllegalSlaveDeviceFailure is thrown if the requested data can't process the request
    private const val EXCEPTION_ILLEGAL_SLAVE_DEVICE_FAILURE: ExceptionCode = 0x04u

    private val functionNames = mapOf(
        FUNCTION_CODE_READ_COILS to "Read Coils",
        FUNCTION_CODE_READ_DISCRETE_INPUTS to "Read Discrete Inputs",
        FUNCTION_CODE_READ_HOLDING_REGISTERS to "Read Holding Registers",
        FUNCTION_CODE_READ_INPUT_REGISTERS to "Read Input Registers",
        FUNCTION_CODE_WRITE_SINGLE_COIL to "Write Single Coil",
        FUNCTION_CODE_WRITE_SINGLE_REGISTER to "Write Single Register",
        FUNCTION_CODE_WRITE_MULTIPLE_COILS to "Write Multiple Coils",
        FUNCTION_CODE_WRITE_MULTIPLE_REGISTERS to "Write Multiple Registers",
        FUNCTION_CODE_MASK_WRITE_REGISTER to "Mask Write Register"
    )

    private val validExceptionCodes = setOf(
        EXCEPTION_ILLEGAL_FUNCTION,
        EXCEPTION_ILLEGAL_ADDRESS,
        EXCEPTION_ILLEGAL_DATA_VALUE,
        EXCEPTION_ILLEGAL_SLAVE_DEVICE_FAILURE
    )

    /**
     * ErrorInvalidCoilInputQuantity is returned if the quantity of coils is out of range
     */
    const val ERROR_INVALID_OR_COIL_INPUT_QUANTITY = "quantity of read coils or inputs must be in range 1-$MAX_READ_COILS_INPUTS"

    /**
     * ErrorInvalidDeviceID is returned if a given device ID is invalid
     */
    const val ERROR_INVALID_DEVICE_ID = "invalid device id"

    /**
     * ErrorInvalidExceptionCode is returned if an invalid exception code is returned
     */
    const val ERROR_INVALID_EXCEPTION_CODE = "invalid exception code"

    /**
     * ErrorInvalidNumberOfOutputCoils is returned if the quantity of output coils is out of range
     */
    const val ERROR_INVALID_NUMBER_OF_OUTPUT_COILS = "quantity of output coil values must be in range 1-$MAX_OUTPUT_COILS"

    /**
     * ErrorInvalidRegisterQuantity is returned if the quantity of registers is out of range
     */
    const val ERROR_INVALID_REGISTER_QUANTITY = "quantity of registers must be in range 1-$MAX_READ_REGISTERS"

    /**
     * Default read timeout
     */
    val READ_TIMEOUT: Duration = 10000.toDuration(DurationUnit.MILLISECONDS)

    /**
     * Reads and creates creates a response for the specified function code
     * @param functionCode UByte The function code
     * @param deviceID UByte The device ID
     * @param transactionID UShort? Transaction ID
     * @return ResponseBase? Read and decoded response
     */
    fun readResponseForFunctionCode(functionCode: FunctionCode, deviceID: DeviceID, transactionID: TransactionID? = null) =
        when (functionCode) {
            FUNCTION_CODE_READ_COILS -> ReadCoilsResponse(
                deviceID = deviceID,
                transactionID = transactionID
            )

            FUNCTION_CODE_READ_DISCRETE_INPUTS -> ReadDiscreteInputsResponse(
                deviceID = deviceID,
                transactionID = transactionID
            )

            FUNCTION_CODE_READ_HOLDING_REGISTERS -> ReadHoldingRegistersResponse(
                deviceID = deviceID,
                transactionID = transactionID
            )

            FUNCTION_CODE_READ_INPUT_REGISTERS -> ReadInputRegistersResponse(
                deviceID = deviceID,
                transactionID = transactionID
            )

            FUNCTION_CODE_WRITE_SINGLE_COIL -> WriteSingleCoilResponse(
                deviceID = deviceID,
                transactionID = transactionID
            )

            FUNCTION_CODE_WRITE_SINGLE_REGISTER -> WriteSingleRegisterResponse(
                deviceID = deviceID,
                transactionID = transactionID
            )

            FUNCTION_CODE_WRITE_MULTIPLE_COILS -> WriteMultipleCoilsResponse(
                deviceID = deviceID,
                transactionID = transactionID
            )

            FUNCTION_CODE_WRITE_MULTIPLE_REGISTERS -> WriteMultipleRegistersResponse(
                deviceID = deviceID,
                transactionID = transactionID
            )

            FUNCTION_CODE_MASK_WRITE_REGISTER -> MaskWriteRegisterResponse(
                deviceID = deviceID,
                transactionID = transactionID
            )

            else -> null
        }

    /**
     * Tests if byte contains an error code
     * @param b UByte Byte to test
     * @return Boolean True if the byte contains an error code
     */
    fun isErrorCode(b: UByte) = b >= ERROR_VALUE_OFFSET

    /**
     * Tests if an exception code is valid
     * @param b UByte Tested exception code
     * @return Boolean Trie if code is valid
     */
    fun isValidExceptionCode(b: UByte) = b in validExceptionCodes

    private const val DiscreteOnStr = "ON"
    private const val DiscreteOffStr = "OFF"

    /**
     * String representation of a discrete value
     * @param v Boolean Discrete value
     * @return String
     */
    fun discreteStr(v: DiscreteValue) = if (v == DISCRETE_ON) DiscreteOnStr else DiscreteOffStr

    /**
     * String representation of request/response elements
     * @param transactionID UShort? Transaction ID
     * @param deviceID UByte Device ID
     * @param function UByte Function code
     * @return String
     */
    internal fun asString(transactionID: TransactionID?, deviceID: DeviceID, function: FunctionCode): String {
        val s = mutableListOf<String>()

        if (transactionID != null) {
            s.add("Transaction=$transactionID")
        }
        s.add("DeviceID=${deviceID}")
        s.add("Function=${asHex(function)}" + if (functionNames[function] != null) " (${functionNames[function]})" else "")
        return s.joinToString()
    }

    /**
     * Modbus handling exception
     */
    class ModbusException(message: String?) : Exception(message)

    /**
     * Hex display value of Unsigned byte
     * @param b UByte
     * @return String
     */
    fun asHex(b: UByte): String {
        return "0x%02X".format(b.toInt())
    }

    /**
     * Hex display value of a short
     * @param s Short
     * @return String
     */
    fun asHex(s: Short): String {
        return "0x%04X".format(s)
    }

    /**
     * Hex display value of an unsigned short
     * @param u UShort
     * @return String
     */
    fun asHex(u: UShort): String {
        return "0x%04X".format(u.toInt())
    }

    /**
     * Hex display value of an integer
     * @param i Int
     * @return String
     */
    fun asHex(i: Int): String {
        return "0x%08X".format(i.toUInt())
    }
}







