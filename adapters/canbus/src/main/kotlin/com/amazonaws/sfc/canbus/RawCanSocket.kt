// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//
// Partial port to Kotlin port of https://github.com/jazdw/jnaCan
//

package com.amazonaws.sfc.canbus

import com.amazonaws.sfc.canbus.jna.*
import com.amazonaws.sfc.canbus.jna.CLibrary.*
import com.amazonaws.sfc.canbus.jna.cmsghdr.cmsgtimeval
import com.ochafik.lang.jnaerator.runtime.NativeSize
import com.sun.jna.ptr.IntByReference
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException


class RawCanSocket : CanSocket<can_frame, CanFrame>(true, false) {

    private var filters: Array<CanFilter>? = null


    override fun open() {
        super.open(CanProtocol.RAW)
    }

    override fun receive(): CanFrame {
        val frame = can_frame()
        super.receive(frame)
        return CanFrame(frame)
    }


    override fun receiveFrom(canInterface: CanInterface): CanFrame {
        val frame = can_frame()
        super.receiveFrom(frame, canInterface)
        return CanFrame(frame)
    }


    fun receiveTimestamped(): TimestampedCanFrame {
        return receiveTimestampedFrom(canInterface!!)
    }


    fun receiveTimestampedFrom(canInf: CanInterface): TimestampedCanFrame {
        if (canInf != CanInterface.ALL_INTERFACES && canInterface != canInf) throw CanException("Can not receive from unbound interface ${canInterface?.name}")
        val addr = sockaddr_can()
        addr.can_family = CLibrary.AF_CAN.toShort()
        addr.can_ifindex = canInf.index
        val sockAddr = addr.toSockAddr()
        sockAddr.write()

        val iov = iovec.ByReference()
        val frame = can_frame()
        iov.iov_base = frame.getPointer()
        iov.iov_len = NativeSize(frame.size().toLong())
        iov.write()

        val msg = msghdr()
        msg.msg_name = sockAddr.getPointer()
        msg.msg_namelen = sockAddr.size()
        msg.msg_iov = iov
        msg.msg_iovlen = NativeSize(1)

        val cmsg = cmsgtimeval()
        msg.msg_control = cmsg.getPointer()
        msg.msg_controllen = NativeSize(cmsg.size().toLong())

        val bytesRead = jna.recvmsg(fd, msg, 0).toLong()
        if (bytesRead < 0) {
            if (timeoutEnabled) throw SocketTimeoutException()
            else throw IOException("Native function recvMsg() returned error $bytesRead")
        }
        if (bytesRead < frame.size()) throw IOException("Native function recvMsg() returned only $bytesRead of ${frame.size()} bytes message message")

        frame.read()
        cmsg.read()

        return TimestampedCanFrame(frame, cmsg.time)
    }


    @JvmName("setFilters")
    fun setFilters(vararg filters: CanFilter) {

        if (filters.isEmpty()) {
            clearFilters()
            return
        }

        val filterRef = can_filter.ByReference()

        @Suppress("UNCHECKED_CAST")
        val filterArray = filterRef.toArray(filters.size) as Array<can_filter>

        for (i in filters.indices) {

            filters[i].copyTo(filterArray[i])
            filterArray[i].write()
        }

        val length = filters.size * filterArray[0].size()
        if (jna.setsockopt(fd, SOL_CAN_RAW, CLibrary.CAN_RAW_FILTER, filterRef.getPointer(), length) < 0) {
            throw SocketException("Could not set CAN_RAW_FILTER socket option")
        }

        this.filters = arrayOf(*filters)
    }


    fun clearFilters() {
        setFilters(CanFilter(0x000, 0x000))
    }


    fun setErrorFilter(errorFilter: Int) {
        val filter = IntByReference(errorFilter)
        if (jna.setsockopt(fd, SOL_CAN_RAW, CLibrary.CAN_RAW_ERR_FILTER, filter.getPointer(), 4) < 0) {
            throw SocketException("Could not set CAN_RAW_ERR_FILTER socket option")
        }
    }

    var loopback: Boolean = false
        set(value) {

            val loopbackInt = if (value) 1 else 0
            val loopbackRef = IntByReference(loopbackInt)
            field = value
            if (jna.setsockopt(fd, SOL_CAN_RAW, CAN_RAW_LOOPBACK, loopbackRef.getPointer(), 4) < 0) {
                throw SocketException("Could not set CAN_RAW_LOOPBACK socket option")
            }

        }


    var receiveOwnMessages: Boolean = true
        set(value) {

            val recvOwnMsgsInt = if (value) 1 else 0
            val recvOwnMsgsRef = IntByReference(recvOwnMsgsInt)
            if (jna.setsockopt(fd, SOL_CAN_RAW, CAN_RAW_RECV_OWN_MSGS, recvOwnMsgsRef.getPointer(), 4) < 0) {
                throw SocketException("Could not set CAN_RAW_RECV_OWN_MSGS socket option")
            }
            field = value

        }

    var timestampEnabled: Boolean = false
        set(value) {
            val timestampInt = if (value) 1 else 0
            val timestampRef = IntByReference(timestampInt)
            if (jna.setsockopt(fd, SOL_SOCKET, SO_TIMESTAMP, timestampRef.getPointer(), 4) < 0) {
                throw SocketException("Could not set SO_TIMESTAMP socket option")
            }
            field = value
        }

}