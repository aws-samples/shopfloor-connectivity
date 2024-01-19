package main;

import com.amazonaws.sfc.config.OpcuaAutoDiscoveryConfigProvider
import com.amazonaws.sfc.log.Logger
import kotlinx.coroutines.delay
import java.io.File

suspend fun main(){
    println("Hello")
    val logger = Logger.createLogger()
    val configStr = File("/Users/leeuwest/Desktop/SFC-source/examples/opcua-auto-discovery/opcua-ad-config.json").readText()
    val cp = OpcuaAutoDiscoveryConfigProvider(configStr, null, logger)

    delay(1000000)

}


