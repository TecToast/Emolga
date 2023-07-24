package de.tectoast.emolga.utils

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object GPIOManager {
    private val service = Executors.newScheduledThreadPool(1)
    private const val TURN_ON_TIME = 500
    private const val TURN_OFF_TIME = 500
    private const val POWER_OFF = 5000
    private fun toggle(pc: PC, duration: Int) {
        exec(arrayOf("/usr/bin/gpio", "write", pc.getWritePin(), "0"))
        service.schedule(
            { exec(arrayOf("/usr/bin/gpio", "write", pc.getWritePin(), "1")) },
            duration.toLong(),
            TimeUnit.MILLISECONDS
        )
    }

    fun startServer(pc: PC) {
        toggle(pc, TURN_ON_TIME)
    }

    fun stopServer(pc: PC) {
        toggle(pc, TURN_OFF_TIME)
    }

    fun powerOff(pc: PC) {
        toggle(pc, POWER_OFF)
    }

    private fun exec(cmd: Array<String>) {
        Runtime.getRuntime().exec(cmd)
    }

    fun isOn(pc: PC): Boolean {
        return BufferedReader(
            InputStreamReader(
                Runtime.getRuntime()
                    .exec(arrayOf("/usr/bin/gpio", "read", pc.getReadPin())).inputStream
            )
        ).readLine().trim() != "1"
    }

    enum class PC(private val writePin: Int, private val readPin: Int, val messageId: Long) {
        FLORIX_2(2, 24, 964571226964115496),
        FLORIX_3(3, 25, 975076826588282962),
        DUMMY(-1, -1, -1);

        fun getWritePin(): String {
            return writePin.toString()
        }

        fun getReadPin(): String {
            return readPin.toString()
        }

        companion object {
            fun byMessage(messageId: Long): PC {
                return entries.firstOrNull { it.messageId == messageId } ?: DUMMY
            }
        }
    }
}
