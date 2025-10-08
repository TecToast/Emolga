package de.tectoast.emolga.features.wrc

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.database.exposed.WRCDataDB
import de.tectoast.emolga.database.exposed.WRCDataDB.GAMEDAYS
import de.tectoast.emolga.database.exposed.WRCDataDB.INTERVALMINS
import de.tectoast.emolga.database.exposed.WRCDataDB.LASTSIGNUP
import de.tectoast.emolga.database.exposed.WRCDataDB.SIGNUPCHANNEL
import de.tectoast.emolga.database.exposed.WRCDataDB.SIGNUPDURATIONMINS
import de.tectoast.emolga.database.exposed.WRCDataDB.WRCNAME
import de.tectoast.emolga.database.exposed.WRCSignupMessageDB
import de.tectoast.emolga.database.exposed.WRCUserSignupDB
import de.tectoast.emolga.utils.repeat.RepeatTask
import de.tectoast.emolga.utils.repeat.RepeatTaskType
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import mu.KotlinLogging
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

object WRCManager {
    private val logger = KotlinLogging.logger {}

    @OptIn(ExperimentalTime::class)
    suspend fun setupRepeatTasks() {
        dbTransaction {
            for (wrc in WRCDataDB.selectAll().where { LASTSIGNUP greaterEq CurrentTimestamp }) {
                val channel = wrc[SIGNUPCHANNEL]
                val wrcName = wrc[WRCNAME]
                val interval = wrc[INTERVALMINS].minutes
                val lastSignup = wrc[LASTSIGNUP]
                val gamedays = wrc[GAMEDAYS]
                RepeatTask(
                    wrcName,
                    RepeatTaskType.Other("WRC Signup Open"),
                    lastSignup,
                    gamedays,
                    interval,
                    printTimestamps = true
                ) { gameday ->
                    executeSignupOpen(channel, wrcName, gameday)
                }
                RepeatTask(
                    wrcName,
                    RepeatTaskType.Other("WRC Signup Close"),
                    lastSignup + wrc[SIGNUPDURATIONMINS].minutes,
                    gamedays,
                    interval
                ) { gameday ->
                    executeSignupClose(wrcName, gameday, channel)
                }
            }
        }
    }

    suspend fun executeSignupOpen(channel: Long, wrcName: String, gameday: Int) {
        val tc = getChannel(channel, wrcName, "SignupOpen") ?: return
        val mid = tc.send(
            embeds = WRCUserSignupDB.buildSignupEmbed(wrcName, gameday).into(),
            components = WRCUserSignupDB.buildSignupButton(wrcName, gameday, disabled = false).into()
        ).await().idLong
        WRCSignupMessageDB.setMessageIdForGameday(wrcName, gameday, mid)
    }

    suspend fun executeSignupClose(wrcName: String, gameday: Int, channel: Long) {
        lockSignupMessage(wrcName, gameday, channel)
        drawParticipants(wrcName, gameday, channel)
    }

    suspend fun lockSignupMessage(wrcName: String, gameday: Int, channel: Long) {
        val mid = WRCSignupMessageDB.getMessageIdForGameday(wrcName, gameday) ?: run {
            logger.warn("No message id found for wrc signup close {} {}", wrcName, gameday)
            return
        }
        val tc = getChannel(channel, wrcName, "LockSignup") ?: return
        tc.editMessageComponentsById(
            mid,
            WRCUserSignupDB.buildSignupButton(wrcName, gameday, disabled = true).into()
        ).await()
    }

    suspend fun drawParticipants(wrcName: String, gameday: Int, channel: Long) {
        getChannel(channel, wrcName, "DrawParticipants")

    }


    private fun getChannel(channelId: Long, wrcName: String? = null, action: String? = null) =
        jda.getTextChannelById(channelId) ?: run {
            logger.warn("Channel {} not found for wrc {} {}", channelId, action, wrcName)
            null
        }
}
