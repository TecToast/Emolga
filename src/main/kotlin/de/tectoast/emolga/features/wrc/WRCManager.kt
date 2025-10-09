package de.tectoast.emolga.features.wrc

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.database.exposed.*
import de.tectoast.emolga.database.exposed.WRCDataDB.GAMEDAYS
import de.tectoast.emolga.database.exposed.WRCDataDB.INTERVALMINS
import de.tectoast.emolga.database.exposed.WRCDataDB.LASTSIGNUP
import de.tectoast.emolga.database.exposed.WRCDataDB.SIGNUPCHANNEL
import de.tectoast.emolga.database.exposed.WRCDataDB.SIGNUPDURATIONMINS
import de.tectoast.emolga.database.exposed.WRCDataDB.TEAMSUBMITMINS
import de.tectoast.emolga.database.exposed.WRCDataDB.WRCNAME
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.repeat.RepeatTask
import de.tectoast.emolga.utils.repeat.RepeatTaskType
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import kotlinx.coroutines.delay
import mu.KotlinLogging
import net.dv8tion.jda.api.interactions.components.ActionRow
import org.jetbrains.exposed.v1.core.Random
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.jdbc.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

object WRCManager {
    private val logger = KotlinLogging.logger {}

    const val TERA_AND_SUBMIT_BASE_MESSAGE =
        "Hier kannst du deine Tera-User wählen und deine Auswahl später bestätigen. Die Optionen sind an einer separaten Nachricht, da Discord nur 5 Komponenten pro Nachricht zulässt :^)"

    @OptIn(ExperimentalTime::class)
    suspend fun setupRepeatTasks() {
        dbTransaction {
            for (wrc in WRCDataDB.selectAll().where { LASTSIGNUP greaterEq CurrentTimestamp }) {
                val channel = wrc[SIGNUPCHANNEL]
                val wrcName = wrc[WRCNAME]
                val interval = wrc[INTERVALMINS].minutes
                val lastSignup = wrc[LASTSIGNUP]
                val gamedays = wrc[GAMEDAYS]
                val signupClose = lastSignup + wrc[SIGNUPDURATIONMINS].minutes
                val teamsubmitClose = signupClose + wrc[TEAMSUBMITMINS].minutes
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
                    signupClose,
                    gamedays,
                    interval
                ) { gameday ->
                    executeSignupClose(wrcName, gameday, channel)
                }
                RepeatTask(
                    wrcName,
                    RepeatTaskType.Other("WRC Team Submit Close"),
                    teamsubmitClose,
                    gamedays,
                    interval
                ) {
                    executeTeamSubmitClose(wrcName, it)
                }
            }
        }
    }

    suspend fun executeTeamSubmitClose(wrcName: String, gameday: Int) {
        dbTransaction {

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
        val tc = getChannel(channel, wrcName, "LockSignup") ?: return
        val mid = WRCSignupMessageDB.getAndDeleteMessageIdForGameday(wrcName, gameday) ?: run {
            logger.warn("No message id found for wrc signup close {} {}", wrcName, gameday)
            return
        }
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
            (newChallengers + oldChallengers.shuffled().take(3 - newChallengers.size)).shuffled().take(3)
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
            val (msg, drawnMons) = drawMons(wrcName, gameday) ?: return
            for (uid in battlingUids) {
                val pc = jda.openPrivateChannelById(uid).await()
                pc.send(msg).queue()
                val teraMsgId = pc.send(
                    TERA_AND_SUBMIT_BASE_MESSAGE,
                    components = buildTeraAndSubmitComponents(
                        wrcName,
                        gameday,
                        options = emptyList(),
                        teraSelected = null
                    )
                ).await().idLong
                WRCTeraDB.setMessageIdForUser(wrcName, gameday, uid, teraMsgId)
                pc.send(
                    "## Deine Mons\n_Bitte wähle für jedes Tier deine Mons aus_",
                    components = getSelectMenus(wrcName, gameday, drawnMons)
                ).queue()
                delay(2000)
            }
        }
    }

    fun buildTeraAndSubmitComponents(
        wrcName: String, gameday: Int, options: List<String>, teraSelected: String?
    ): List<ActionRow> = listOf(
        WRCTeraSelectMenu(
            placeholder = if (options.isEmpty()) "Tera-Auswahl, wähle erst Mons aus :)" else "Tera-Auswahl",
            disabled = options.isEmpty(),
            options = options.ifEmpty { listOf("") }.map { SelectOption(it, it, default = it == teraSelected) }
        ) {
            this.wrcname = wrcName
            this.gameday = gameday
        }, WRCMonSubmitButton(disabled = teraSelected == null) {
            this.wrcname = wrcName
            this.gameday = gameday
        }).into()

    private suspend fun getSelectMenus(wrcName: String, gameday: Int, drawnMons: List<List<String>>): List<ActionRow> {
        val tl = WRCDataDB.getTierlistOfWrcName(wrcName) ?: error("No tierlist found for wrc $wrcName")
        return tl.prices.entries.zip(drawnMons).map { (tierData, mons) ->
            ActionRow.of(
                WRCMonSelect(
                    placeholder = tierData.key,
                    options = mons.map { SelectOption(label = it, value = it) },
                    valueRange = tierData.value..tierData.value
                ) {
                    this.wrcname = wrcName
                    this.gameday = gameday
                    this.tier = tierData.key
                })
        }
    }

    suspend fun drawMons(wrcName: String, gameday: Int) = dbTransaction {
        val tl = WRCDataDB.getTierlistOfWrcName(wrcName) ?: return@dbTransaction null
        val allMons = tl.order.map {
            Tierlist.select(Tierlist.POKEMON).where { tl.basePredicate and (Tierlist.TIER eq it) }.except(
                WRCMonsPickedDB.select(WRCMonsPickedDB.MON).where { WRCMonsPickedDB.WRCNAME eq wrcName })
                .orderBy(Random()).limit(10)
        }.reduce { acc, r -> UnionAll(acc, r) }.map { it[Tierlist.POKEMON] }
        val allMonsChunked = allMons.chunked(10)
        return@dbTransaction buildString {
            append("# Pokemon-Wahl für $wrcName - Spieltag $gameday\n")
            for ((tier, mons) in tl.order.zip(allMonsChunked)) {
                append("**$tier:**\n")
                for (mon in mons) {
                    append("$mon\n")
                }
                append("\n")
            }
            WRCMonsOptionsDB.batchInsert(allMons, shouldReturnGeneratedValues = false) {
                this[WRCMonsOptionsDB.WRCNAME] = wrcName
                this[WRCMonsOptionsDB.GAMEDAY] = gameday
                this[WRCMonsOptionsDB.MON] = it
            }
        } to allMonsChunked
    }

    suspend fun checkIfAllSubmitted(wrcname: String, gameday: Int, user: Long) {
        val wrc = WRCDataDB.getByName(wrcname) ?: return
        val (users, index) = WRCMatchupsDB.getUsersIfSubmitted(wrcname, gameday, user) ?: return
        val msg = buildString {
            append("$wrcname - Spieltag $gameday - ")
            if (index == 0) append("Beat the Warrior") else append("Server-Challenge")
            append("\n\n")
            append(users.map {
                "Picks von <@$it>:\n" + WRCMonsPickedDB.buildPickedMonsMessage(wrcname, gameday, it)
            }.joinToString("\n\n"))
        }
        val tc = getChannel(wrc[WRCDataDB.MATCHUPCHANNEL]) ?: return
        tc.send(msg).queue()
    }


    private fun getChannel(channelId: Long, wrcName: String? = null, action: String? = null) =
        jda.getTextChannelById(channelId) ?: run {
            logger.warn("Channel {} not found for wrc {} {}", channelId, action, wrcName)
            null
        }
}
