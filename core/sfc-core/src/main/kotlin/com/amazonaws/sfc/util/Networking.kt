/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.util

import com.amazonaws.sfc.awsiot.AWSIotException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.UnknownHostException
import java.util.*

fun canNotReachAwsService(e: Throwable) = (
        ((e.cause?.cause != null) && (e.cause?.cause!! is UnknownHostException)) ||
        ((((e as? AWSIotException)?.message) ?: "").indexOf("java.net.UnknownHostException") != -1) ||
        (e.message == "Connection pool shut down"))

fun getIp4NetworkAddress(interfaceName: String?): String? {

    if (interfaceName == null) {
        val hostAddress = InetAddress.getLocalHost()
        return hostAddress.hostAddress
    }

    val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
    return en.toList().firstOrNull {
        it.name == interfaceName || it.displayName == interfaceName
    }?.let { n -> return n.inetAddresses.toList().firstOrNull { it is Inet4Address }?.hostAddress }
}

fun getIp4Address(address: String): String? {
    return if (IP_ADDRESS.matches(address))
        address
    else try {
        if (address.lowercase() == "localhost")
            InetAddress.getLocalHost().hostAddress
        else
            InetAddress.getByName(address)?.hostAddress
    } catch (_: UnknownHostException) {
        null
    }
}

fun getHostName(): String =
    InetAddress.getLocalHost().hostName

fun getAddressesAndHostNames(): Pair<List<String>, List<String>> {
    val allNamesAndAddresses = try {
        NetworkInterface.getNetworkInterfaces().toList().flatMap { en ->
            en.inetAddresses.toList().filterIsInstance<Inet4Address>().flatMap { a ->
                listOf(a.hostName, a.canonicalHostName, a.hostAddress)
            }
        }.toSet()

    } catch (e: Exception) {
        emptySet()
    }
    return allNamesAndAddresses.partition { IP_ADDRESS.matches(it) }
}

val IP_ADDRESS = """^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)${'$'}""".toRegex()
