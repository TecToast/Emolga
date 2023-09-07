@file:Suppress("unused")

package de.tectoast.emolga.utils

import de.tectoast.emolga.commands.httpClient
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val TURN_ON_TIME = 500
private const val TURN_OFF_TIME = 500
private const val POWER_OFF = 5000

abstract class GPIOManager(val boardType: BoardType) {
    abstract suspend fun startServer(pc: PC)
    abstract suspend fun stopServer(pc: PC)
    abstract suspend fun powerOff(pc: PC)
    abstract suspend fun isOn(pc: PC): Boolean


    companion object {
        private var instance: GPIOManager = RemoteGPIOManager(httpClient, "http://rpicontrol:8080")
        operator fun invoke(): GPIOManager = instance

        fun setGlobalManager(manager: GPIOManager) {
            instance = manager
        }
    }

}

class RemoteGPIOManager
/**
 *
 */
    (val client: HttpClient, val url: String) : GPIOManager(BoardType.BCM) {
    override suspend fun startServer(pc: PC) = push(pc, TURN_ON_TIME)

    override suspend fun stopServer(pc: PC) = push(pc, TURN_OFF_TIME)

    override suspend fun powerOff(pc: PC) = push(pc, POWER_OFF)

    private suspend fun push(pc: PC, delay: Int) {
        withContext(Dispatchers.IO) {
            client.post("$url/push/${pc.getWritePin(boardType)}") {
                setBody("$delay")
            }
        }
    }

    override suspend fun isOn(pc: PC) = withContext(Dispatchers.IO) {
        client.get("$url/status/${pc.getReadPin(boardType)}").bodyAsText().trim() != "1"
    }
}

object DirectGPIOManager : GPIOManager(BoardType.WPi) {
    private val service = Executors.newScheduledThreadPool(1)
    private fun toggle(pc: PC, duration: Int) {
        val writePin = pc.getWritePin(boardType)
        exec(arrayOf("/usr/bin/gpio", "write", writePin, "0"))
        service.schedule(
            { exec(arrayOf("/usr/bin/gpio", "write", writePin, "1")) },
            duration.toLong(),
            TimeUnit.MILLISECONDS
        )
    }

    override suspend fun startServer(pc: PC) {
        toggle(pc, TURN_ON_TIME)
    }

    override suspend fun stopServer(pc: PC) {
        toggle(pc, TURN_OFF_TIME)
    }

    override suspend fun powerOff(pc: PC) {
        toggle(pc, POWER_OFF)
    }

    private fun exec(cmd: Array<String>) {
        Runtime.getRuntime().exec(cmd)
    }

    override suspend fun isOn(pc: PC): Boolean {
        return withContext(Dispatchers.IO) {
            BufferedReader(
                InputStreamReader(
                    Runtime.getRuntime()
                        .exec(arrayOf("/usr/bin/gpio", "read", pc.getReadPin(boardType))).inputStream
                )
            ).readLine()
        }.trim() != "1"
    }

}

enum class PC(
    private val writePinWPi: Int,
    private val readPinWPi: Int,
    private val writePinBCM: Int,
    private val readPinBCM: Int,
    val messageId: Long
) {
    FLORIX_2(
        2,
        24,
        27,
        19,
        964571226964115496
    ),
    FLORIX_3(
        3,
        25,
        22,
        26,
        975076826588282962
    );

    fun getWritePin(type: BoardType): String {
        return when (type) {
            BoardType.WPi -> writePinWPi
            BoardType.BCM -> writePinBCM
        }.toString()
    }

    fun getReadPin(type: BoardType): String {
        return when (type) {
            BoardType.WPi -> readPinWPi
            BoardType.BCM -> readPinBCM
        }.toString()
    }

    companion object {
        fun byMessage(messageId: Long): PC {
            return entries.first { it.messageId == messageId }
        }
    }
}

enum class BoardType {
    WPi, BCM
}
