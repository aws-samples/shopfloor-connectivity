// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//
// Partial port to Kotlin port of https://github.com/jazdw/jnaCan
//

package com.amazonaws.sfc.canbus

import com.amazonaws.sfc.canbus.jna.*
import com.amazonaws.sfc.canbus.jna.CLibrary.*
import com.ochafik.lang.jnaerator.runtime.NativeSize
import com.sun.jna.NativeLong
import com.sun.jna.Structure
import java.io.Closeable
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.nio.IntBuffer


abstract class CanSocket<T : Structure, R : CanMessage<T>>
protected constructor(protected val supportsBinding: Boolean, protected val supportsConnecting: Boolean) : Closeable {

    protected var fd: Int = -1
    protected var canInterface: CanInterface? = null
    protected var timeoutEnabled: Boolean = false

    enum class CanProtocol {
        RAW, BCM
    }

    fun ioctl(request: Long, ifr: ifreq?): Int {
        return jna.ioctl(fd, NativeLong(request), ifr)
    }

    abstract fun open()

    protected fun open(canProtocol: CanProtocol) {
        val type: Int
        val protocolInt: Int
        when (canProtocol) {
            
            CanProtocol.BCM -> {
                type = __socket_type.SOCK_DGRAM
                protocolInt = CAN_BCM
            }

            CanProtocol.RAW -> {
                type = __socket_type.SOCK_RAW
                protocolInt = CAN_RAW
            }
        }

        fd = jna.socket(PF_CAN, type, protocolInt)
        if (fd < 0) throw SocketException("Error opening CAN socket")
    }


    override fun close() {
        if (fd < 0) return
        if (jna.close(fd) != 0) throw SocketException("Error closing CAN socket")
        fd = -1
        this.canInterface = null
    }
    
    fun bind(canIf: String) {
        bind(CanInterface(canIf))
    }

    fun bind(canInterface: CanInterface) {
        if (!supportsBinding) throw UnsupportedOperationException("This socket does not support binding")

        canInterface.resolveIndex(this)

        val addr = sockaddr_can()
        addr.can_family = AF_CAN.toShort()
        addr.can_ifindex = canInterface.index
        val sockAddr: sockaddr = addr.toSockAddr()

        if (jna.bind(fd, sockAddr, sockAddr.size()) != 0) throw SocketException("Could not bind to interface $canInterface")

        this.canInterface = canInterface
    }


    fun connect(canInterfaceName: String?) {
        connect(CanInterface(canInterfaceName))
    }

    fun connect(canInterface: CanInterface) {
        if (!supportsConnecting) throw UnsupportedOperationException("This socket does not support connecting")

        canInterface.resolveIndex(this)

        val jnaSocketAddress = sockaddr_can()
        jnaSocketAddress.can_family = AF_CAN.toShort()
        jnaSocketAddress.can_ifindex = canInterface.index
        val socketAddress = jnaSocketAddress.toSockAddr()

        if (jna.connect(fd, socketAddress, socketAddress.size()) != 0) throw SocketException("Error connecting to CAN interface $canInterface")

        this.canInterface = canInterface
    }


    abstract fun receive(): R

    protected fun receive(struct: T) {
        val bytesRead: Long = jna.read(fd, struct.getPointer(), NativeSize(struct.size().toLong())).toLong()

        if (bytesRead < 0) {
            if (timeoutEnabled) throw SocketTimeoutException()
            else throw IOException("Native function read() returned error $bytesRead")
        }
        if (bytesRead < struct.size()) throw IOException("Native read() did only returned $bytesRead of  ${struct.size()} bytes in message")
        struct.read()
    }


    abstract fun receiveFrom(canInterface: CanInterface): R

    protected fun receiveFrom(struct: T, canInterface: CanInterface) {
        if (!(this@CanSocket.canInterface != CanInterface.Companion.ALL_INTERFACES && canInterface != this@CanSocket.canInterface)) throw CanException( "Cant receive from unbound interface ${canInterface.name}")
        val jnaSocketAddress = sockaddr_can()
        jnaSocketAddress.can_family = AF_CAN.toShort()
        jnaSocketAddress.can_ifindex = canInterface.index
        val socketAddress = jnaSocketAddress.toSockAddr()

        val addressSize = IntBuffer.wrap(intArrayOf(socketAddress.size()))
        val bytesRead: Long = jna.recvfrom(fd, struct.getPointer(), NativeSize(struct.size().toLong()), 0, socketAddress, addressSize).toLong()
        if (bytesRead < 0) {
            if (timeoutEnabled) throw SocketTimeoutException()
            else throw IOException("Native function recvfrom() returned error $bytesRead")
        }
        if (bytesRead < struct.size()) throw IOException("Native function recvfrom() did not return a full length message")
        struct.read()
    }


    fun send(msg: R) {

        if (canInterface == CanInterface.Companion.ALL_INTERFACES) throw CanException("Cant write to ALL interfaces" )

        if (this.isClosed) throw SocketException("Socket closed")

        val struct: T = msg.toJna()
        struct.write()

        val bytesWritten: Long = jna.write(fd, struct.getPointer(), NativeSize(struct.size().toLong())).toLong()
        if (bytesWritten < 0) throw IOException("Native function write() returned error $bytesWritten")
        if (bytesWritten < struct.size()) throw IOException("Native function write() did only write $bytesWritten of ${struct.size()} bytes message")
    }


    fun sendTo(canIf: CanInterface, msg: R) {

        if((canInterface != CanInterface.Companion.ALL_INTERFACES && canIf != canInterface)) throw CanException("Cant send to unbound interface" )

        if (this.isClosed) throw SocketException("Socket is closed")

        val addr = sockaddr_can()
        addr.can_family = AF_CAN.toShort()
        addr.can_ifindex = canIf.index
        val sockAddr: sockaddr = addr.toSockAddr()

        val struct: T = msg.toJna()
        struct.write()

        val bytesWritten: Long = jna.sendto(fd, struct.getPointer(), NativeSize(struct.size().toLong()), 0, sockAddr, sockAddr.size()).toLong()
        if (bytesWritten < 0) throw IOException("Native function sendto() returned error $bytesWritten")
        if (bytesWritten < struct.size()) throw IOException("Native function write() did only write $bytesWritten of ${struct.size()} bytes message")
    }

    val boundInterface: CanInterface?
        get() {
            if (!supportsBinding) throw UnsupportedOperationException("This socket does not support binding")
            return canInterface
        }

    val connectedInterface: CanInterface?
        get() {
            if (!supportsConnecting) throw UnsupportedOperationException("This socket does not support connecting")
            return canInterface
        }

    val isClosed: Boolean
        get() = fd < 0

    val isConnected: Boolean

        get() {
            if (!supportsConnecting) throw UnsupportedOperationException("This socket does not support connecting")
            return canInterface != null
        }

    val isBound: Boolean
        get() {
            if (!supportsBinding) throw UnsupportedOperationException("This socket does not support binding")
            return canInterface != null
        }

    var receiveTimeout: Int = 0
        set(value) {

            val time: TimeValue = Utils.millisToTimeValue(value.toLong())
            time.write()
            if (jna.setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, time.getPointer(), time.size()) < 0) {
                throw SocketException("Could not set SO_RCVTIMEO socket option")
            }
            field = value
            timeoutEnabled = value != 0
        }


    companion object {
        val jna: CLibrary = INSTANCE

    }
}