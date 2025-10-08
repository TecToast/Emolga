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
import de.tectoast.emolga.database.exposed.WRCMatchupsDB
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
            mid, WRCUserSignupDB.buildSignupButton(wrcName, gameday, disabled = true).into()
        ).await()
    }

    suspend fun drawParticipants(wrcName: String, gameday: Int, channel: Long) {
        val tc = getChannel(channel, wrcName, "DrawParticipants") ?: return
        val (warriors, challengers) = WRCUserSignupDB.getAllSignupsForGameday(wrcName, gameday)
        val allRegisteredUsers = WRCUserSignupDB.getAllRegisteredUsers(wrcName)
        val selectedWarrior = warriors.filter { it !in allRegisteredUsers }.randomOrNull() ?: warriors.randomOrNull()
        val (newChallengers, oldChallengers) = challengers.partition { it !in allRegisteredUsers }
        val selectedChallengers = (if (newChallengers.size >= 3) newChallengers.shuffled().take(3) else {
            (newChallengers + oldChallengers.shuffled()
                .take(3 - newChallengers.size)).shuffled().take(3)
        }).toMutableList()
        val battlingUids = mutableListOf<Long>()
        tc.send(buildString {
            when {
                selectedWarrior != null && selectedChallengers.isNotEmpty() -> {
                    val againstWarrior = selectedChallengers.random()
                    selectedChallengers -= againstWarrior
                    append("**Beat the Warrior:** <@${selectedWarrior}> vs <@${againstWarrior}\n")
                    WRCMatchupsDB.insertMatchup(wrcName, gameday, 0, selectedWarrior, againstWarrior)
                    battlingUids += selectedWarrior
                    battlingUids += againstWarrior
                    if (selectedChallengers.size == 2) {
                        append("**Server-Challenge:** <@${selectedChallengers[0]}> vs <@${selectedChallengers[1]}>")
                        WRCMatchupsDB.insertMatchup(wrcName, gameday, 1, selectedChallengers[0], selectedChallengers[1])
                        battlingUids += selectedChallengers
                    } else {
                        append("**Server-Challenge:** _nicht genug Challenger vorhanden_")
                    }
                }

                selectedWarrior != null -> {
                    append("**Beat the Warrior:** _kein Challenger vorhanden_\n")
                    append("**Server-Challenge:** _nicht genug Challenger vorhanden_")
                }

                selectedChallengers.size >= 2 -> {
                    append("**Beat the Warrior:** _kein Warrior vorhanden_\n")
                    append("**Server-Challenge:** <@${selectedChallengers[0]}> vs <@${selectedChallengers[1]}>")
                    WRCMatchupsDB.insertMatchup(wrcName, gameday, 1, selectedChallengers[0], selectedChallengers[1])
                    battlingUids += selectedChallengers
                }

                else -> {
                    append("**Beat the Warrior:** _kein Warrior vorhanden_\n")
                    append("**Server-Challenge:** _nicht genug Challenger vorhanden_")
                }
            }
        }).queue()
        if (battlingUids.isNotEmpty()) {

        }
    }

    suspend fun drawMons() {

    }


    private fun getChannel(channelId: Long, wrcName: String? = null, action: String? = null) =
        jda.getTextChannelById(channelId) ?: run {
            logger.warn("Channel {} not found for wrc {} {}", channelId, action, wrcName)
            null
        }
}
