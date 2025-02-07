// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//
// Partial port to Kotlin port of https://github.com/jazdw/jnaCan
//

package com.amazonaws.sfc.canbus
import com.amazonaws.sfc.canbus.jna.CLibrary
import com.amazonaws.sfc.canbus.jna.ifreq
import com.amazonaws.sfc.canbus.jna.ifreq.ifr_ifrn_union
import com.amazonaws.sfc.canbus.jna.ifreq.ifr_ifru_union
import com.sun.jna.Native
import java.net.SocketAddress
import java.net.SocketException

class CanInterface(var index: Int = 0,
                   var name: String? = null,
                   var mtu: Int? = null) : SocketAddress() {


    constructor(name: String?) : this() {
        this.name = name
    }

    constructor(index: Int, name: String?) : this() {
        this.index = index
        this.name = name
    }

    fun resolveName(socket: CanSocket<*, *>) {
        if (name != null) return

        if (index == ALL_INTERFACES.index) {
            name = ALL_INTERFACES.name
            return
        }

        val ifRequest = ifreq()
        ifRequest.ifr_ifru = ifr_ifru_union(index)
        if (socket.ioctl(CLibrary.SIOCGIFNAME.toLong(), ifRequest) != 0) {
            throw SocketException("Could not find name of interface $index ")
        }
        name = Native.toString(ifRequest.ifr_ifrn.ifrn_name)
    }


    fun resolveIndex(socket: CanSocket<*, *>) {

        if (name == null) resolveName(socket)

        val ifRequest = ifreq()
        ifRequest.ifr_ifrn = ifr_ifrn_union(Utils.stringToFixedLengthByteArray(name.toString(), 16))
        if (socket.ioctl(CLibrary.SIOCGIFINDEX.toLong(), ifRequest) != 0) {
            throw SocketException("Could not find interface with name $name")
        }
        index = ifRequest.ifr_ifru.ifru_ivalue
    }


    fun resolveMTU(socket: CanSocket<*, *>) {
        if (mtu != null) return

        if (name == null) resolveName(socket)

        val ifRequest: ifreq = ifreq()
        ifRequest.ifr_ifrn = ifr_ifrn_union(Utils.stringToFixedLengthByteArray(name.toString(), 16))
        if (socket.ioctl(CLibrary.SIOCGIFMTU.toLong(), ifRequest) != 0)
            throw SocketException("Could not get MTU of interface $name")
        mtu = ifRequest.ifr_ifru.ifru_mtu
    }


    companion object {
        val ALL_INTERFACES: CanInterface = CanInterface(0, "All")

    }
}