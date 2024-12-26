package de.tectoast.emolga.utils.showdown

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.database.Database
import de.tectoast.emolga.database.exposed.*
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.flo.SendFeatures
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.increment
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.records.toCoord
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.into
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import mu.KotlinLogging
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.io.File
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

object Analysis {

    suspend fun analyseReplay(
        urlProvided: String,
        customReplayChannel: GuildMessageChannel? = null,
        resultchannelParam: GuildMessageChannel,
        message: Message? = null,
        fromReplayCommand: InteractionData? = null,
        customGuild: Long? = null,
        withSort: Boolean = true,
        analysisData: AnalysisData? = null,
        useReplayResultChannelAnyways: Boolean = false
    ) = analyseReplay(
        listOf(urlProvided), customReplayChannel, resultchannelParam, message, fromReplayCommand, customGuild, withSort,
        analysisData, useReplayResultChannelAnyways
    )

    suspend fun analyseReplay(
        urlsProvided: List<String>,
        customReplayChannel: GuildMessageChannel? = null,
        resultchannelParam: GuildMessageChannel,
        message: Message? = null,
        fromReplayCommand: InteractionData? = null,
        customGuild: Long? = null,
        withSort: Boolean = true,
        analysisData: AnalysisData? = null,
        useReplayResultChannelAnyways: Boolean = false
    ) {
        //defaultScope.launch {
        if (EmolgaMain.BOT_DISABLED && resultchannelParam.guild.idLong != Constants.G.MY) {
            (message?.channel ?: resultchannelParam).sendMessage(EmolgaMain.DISABLED_TEXT).queue()
            return
        }

        logger.info("REPLAY! Channel: {}", message?.channel?.id ?: resultchannelParam.id)
        fun send(msg: String) {
            fromReplayCommand?.reply(msg) ?: resultchannelParam.sendMessage(msg).queue()
        }
        if (fromReplayCommand != null && !resultchannelParam.guild.selfMember.hasPermission(
                resultchannelParam, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND
            )
        ) {
            send("Ich habe keine Berechtigung, im konfigurierten Channel ${resultchannelParam.asMention} zu schreiben!")
            return
        }
        var league: League? = null
        val replayDatas = mutableListOf<ReplayData>()
        for (urlProvided in urlsProvided) {

            val data = try {
                analysisData ?: analyse(urlProvided, ::send, resultchannelParam.guild.idLong == Constants.G.MY)
                //game = Analysis.analyse(url, m);
            } catch (ex: Exception) {
                when (ex) {
                    is ShowdownDoesNotAnswerException -> {
                        send("Showdown antwortet nicht. Versuche es später nochmal.")
                    }

                    is ShowdownDoesntExistException -> {
                        send("Das Replay existiert nicht (mehr)! Sicher, dass der Link korrekt ist?")
                    }

                    is ShowdownParseException -> {
                        send("Das Replay konnte nicht analysiert werden! Sicher, dass es ein valides Replay ist? Wenn ja, melde dich bitte auf meinem im Profil verlinkten Support-Server.")
                    }

                    is InvalidReplayException -> {
                        send("Das ist kein gültiges Replay!")
                    }

                    else -> {
                        val msg =
                            "Beim Auswerten des Replays ist ein Fehler aufgetreten! Sehr wahrscheinlich liegt es an einem Bug in der neuen Engine, mein Programmierer wurde benachrichtigt."
                        send(msg)
                        logger.error(
                            "Fehler beim Auswerten des Replays: $urlProvided ${resultchannelParam.guild.name} ${resultchannelParam.asMention} ChannelID: ${resultchannelParam.id}",
                            ex
                        )
                    }
                }
                return
            }
            val (game, ctx) = data
            val url = ctx.url
            val g = resultchannelParam.guild
            val gid = customGuild ?: g.idLong
            val u1 = game[0].nickname
            val u2 = game[1].nickname
            val uid1db = SDNamesDB.getIDByName(u1)
            val uid2db = SDNamesDB.getIDByName(u2)
            logger.info("Analysed!")
            val spoiler = SpoilerTagsDB.contains(gid)
            game.forEach {
                it.pokemon.addAll(List(it.teamSize - it.pokemon.size) { SDPokemon("_unbekannt_", -1) })
            }
            val activePassive = ActivePassiveKillsDB.hasEnabled(gid)
            val monStrings = game.map { player ->
                player.pokemon.map { mon ->
                    getMonName(mon.pokemon, gid).also {
                        mon.draftname = it
                    }.displayName.let {
                        if (activePassive) {
                            "$it (${mon.activeKills} aktive Kills, ${mon.passiveKills} passive Kills)"
                        } else {
                            it.condAppend(mon.kills > 0, " ${mon.kills}")
                        }
                    }.condAppend((!player.allMonsDead || spoiler) && mon.isDead, " X")
                }.joinToString("\n")
            }
            val leaguedata = db.leagueByGuildAdvanced(
                gid, game, ctx, null, uid1db, uid2db
            )
            league = leaguedata?.league
            val uindices = leaguedata?.uindices
            val draftPlayerList = game.map(SDPlayer::toDraftPlayer)
            val gamedayData = defaultScope.async {
                league?.getGamedayData(uindices!![0], uindices[1], draftPlayerList)
            }
            val description = game.mapIndexed { index, sdPlayer ->
                mutableListOf<Any>(
                    leaguedata?.mentions[index] ?: sdPlayer.nickname,
                    sdPlayer.pokemon.count { !it.isDead }.minus(if (ctx.vgc) 2 else 0)
                ).apply { if (spoiler) add(1, "||") }.let { if (index % 2 > 0) it.asReversed() else it }
            }.joinToString(":") { it.joinToString(" ") }
                .condAppend(ctx.vgc, "\n(VGC)") + "\n\n" + game.mapIndexed { index, player ->
                "${leaguedata?.mentions[index] ?: player.nickname}:".condAppend(
                    player.allMonsDead && !spoiler, " (alle tot)"
                ) + "\n".condAppend(spoiler, "||") + monStrings[index].condAppend(spoiler, "||")
            }.joinToString("\n\n")
            val embed = Embed(description = description)

//            if (league is ASL) {
//                val gdData = gamedayData.await()
//                if (gdData?.gameday == 10) {
//                    message?.channel?.sendMessage("Replay ist angekommen, wird aber erst später ausgewertet!")?.queue()
//                    return
//                }
//            }
            val jda = resultchannelParam.jda
            val replayChannel =
                league?.provideReplayChannel(jda).takeIf { useReplayResultChannelAnyways || customGuild == null }
                    ?: customReplayChannel
            val resultChannel =
                league?.provideResultChannel(jda).takeIf { useReplayResultChannelAnyways || customGuild == null }
                    ?: resultchannelParam
            logger.info("uids = $uindices")
            logger.info("u1 = $u1")
            logger.info("u2 = $u2")
            if (league != null) {
                resultChannel.sendMessageEmbeds(embed).queue()
            } else {
                resultChannel.sendMessage(description).queue()
            }
            defaultScope.launch {
                val gdData = gamedayData.await()
                val tosend = MessageCreate(
                    content = url,
                    embeds = league?.appendedEmbed(data, leaguedata!!, gdData!!)?.build()?.into().orEmpty()
                )
                replayChannel?.sendMessage(tosend)?.queue()
                fromReplayCommand?.reply(msgCreateData = tosend)
            }
            if (resultchannelParam.guild.idLong != Constants.G.MY) {
                db.statistics.increment("analysis")
                game.forEach { player ->
                    player.pokemon.filterNot { "unbekannt" in it.pokemon }.forEach {
                        FullStatsDB.add(
                            it.draftname.official, it.kills, if (it.isDead) 1 else 0, player.winnerOfGame
                        )
                    }
                }
                defaultScope.launch {
                    EmolgaMain.updatePresence()
                }
            }
            var shouldSendZoro = false
            for (ga in game) {
                if (ga.containsZoro()) {
                    resultchannelParam.sendMessage(
                        "Im Team von ${ga.nickname} befindet sich ein Pokemon mit Illusion! Bitte noch einmal die Kills überprüfen!"
                    ).queue()
                    shouldSendZoro = true
                }
            }
            if (shouldSendZoro) {
                jda.getTextChannelById(1016636599305515018)!!.sendMessage(url).queue()
            }
            if (gid != Constants.G.MY && game.indices.fold(0 to 0) { old, i ->
                    val (kills, deaths) = game[i].totalKDCount
                    (old.first + kills) to (old.second + deaths)
                }.let { it.first != it.second }) {
                SendFeatures.sendToMe((if (shouldSendZoro) "Zoroark... " else "") + "ACHTUNG ACHTUNG! KILLS SIND UNGLEICH DEATHS :o\n$url\n${resultchannelParam.asMention}")
            }
            logger.info("In Emolga Listener!")
            Database.dbScope.launch {
                newSuspendedTransaction {
                    val replayChannelId =
                        fromReplayCommand?.tc ?: replayChannel?.idLong ?: return@newSuspendedTransaction
                    val endlessId =
                        EndlessLeagueChannelsDB.selectAll().where { EndlessLeagueChannelsDB.CHANNEL eq replayChannelId }
                            .firstOrNull()?.get(
                                EndlessLeagueChannelsDB.ID
                            ) ?: return@newSuspendedTransaction
                    if (EndlessLeagueDataDB.insertIgnore {
                            it[ID] = endlessId
                            it[URL] = url
                        }.insertedCount > 0) {
                        val newSize =
                            EndlessLeagueDataDB.select(EndlessLeagueDataDB.ID)
                                .where { EndlessLeagueDataDB.ID eq endlessId }
                                .count()
                        // new replay for this id
                        val info = EndlessLeagueInfoDB.selectAll().where { EndlessLeagueInfoDB.ID eq endlessId }.first()
                        RequestBuilder(info[EndlessLeagueInfoDB.SID]).addRow(
                            info[EndlessLeagueInfoDB.STARTCOORD].toCoord().plusY(newSize.toInt() - 1), listOf(
                                url,
                                *listOf(u1, u2).let { if (game[0].winnerOfGame) it else it.reversed() }.toTypedArray(),
                                game[0].totalKDCount.let { it.first - it.second }.absoluteValue
                            )
                        ).execute()
                    }
                }
            }
            val kd =
                game.map { it.pokemon.associate { p -> p.draftname.official to (p.kills to if (p.isDead) 1 else 0) } }
            if (leaguedata != null) {
                replayDatas += ReplayData(
                    game = draftPlayerList,
                    uindices = uindices!!,
                    kd = kd,
                    mons = game.map { it.pokemon.map { mon -> mon.draftname.official } },
                    url = url,
                    gamedayData = gamedayData.await()!!,
                    otherForms = leaguedata.otherForms,
                )
            }
        }
        if (replayDatas.isNotEmpty()) {
            league?.docEntry?.analyse(
                replayDatas, withSort = withSort
            )
        }
    }

    suspend fun getMonName(s: String, guildId: Long, withDebug: Boolean = false): DraftName {
        if (withDebug) logger.info("s = $s")
        val split = s.split("-")
        val withoutLast = split.dropLast(1).joinToString("-")
        if (split.last() == "*") return getMonName(withoutLast, guildId, withDebug)
        return if (s == "_unbekannt_") DraftName("_unbekannt_", "UNKNOWN")
        else {
            var pkdata = db.pokedex.get(s.toSDName())
            (NameConventionsDB.getSDTranslation(pkdata?.takeIf { it.requiredAbility != null }?.baseSpecies?.also {
                pkdata = db.pokedex.get(
                    it.toSDName()
                )
            } ?: s, guildId) ?: DraftName(
                s, s
            )).apply { data = pkdata }
        }
        //}
    }


    enum class ReplayServerMode {
        LOG {
            override fun getLogFromWebsiteText(text: String) = text
            override fun mapURL(url: String) = "$url.log"
        },
        SCRAPE {
            private val logRegex =
                Regex("<script type=\"text/plain\" class=\"log\">(.*?)</script>", RegexOption.DOT_MATCHES_ALL)

            override fun getLogFromWebsiteText(text: String) = logRegex.find(text)?.groupValues[1] ?: ""
        };

        abstract fun getLogFromWebsiteText(text: String): String
        open fun mapURL(url: String) = url
    }

    val regex =
        Regex("https://(replays?\\.(?:tectoast\\.de|pokemonshowdown\\.com)|battling.p-insurgence.com/replays)/(?:[a-z]+-)?[^-]+-\\d+[-a-z0-9]*")

    val modeByServer = mapOf<String, ReplayServerMode>(
        "replay.pokemonshowdown.com" to ReplayServerMode.LOG,
        "replays.tectoast.de" to ReplayServerMode.LOG,
        "battling.p-insurgence.com/replays" to ReplayServerMode.SCRAPE
    )

    suspend fun analyse(
        urlProvided: String, answer: ((String) -> Unit)? = null, debugMode: Boolean = false
    ): AnalysisData {
        var gameNullable: List<String>? = null
        val mr = regex.find(urlProvided) ?: throw InvalidReplayException()
        val mode = modeByServer[mr.groupValues[1]] ?: throw InvalidReplayException()
        val url = mr.groupValues[0]
        val mappedURL = mode.mapURL(url)
        @Suppress("unused") for (i in 0..1) {
            var statusCode: HttpStatusCode? = null
            val retrieved = runCatching {
                withContext(Dispatchers.IO) {
                    logger.info("Reading URL... {}", url)
                    val text = httpClient.get(mappedURL).also { statusCode = it.status }.bodyAsText()
                    mode.getLogFromWebsiteText(text).split("\n")
                }
            }.getOrDefault(listOf(""))
            gameNullable = retrieved.takeIf { it.size > 1 }
            if (gameNullable == null) {
                logger.info(retrieved.toString())
                if (statusCode == HttpStatusCode.NotFound) {
                    throw ShowdownDoesntExistException()
                }
                logger.info("Showdown antwortet nicht")
                answer?.invoke("Der Showdown-Server antwortet nicht, ich versuche es in 10 Sekunden erneut...")
                delay(10.seconds)
            } else break
        }
        logger.info("Starting analyse!")
        val game = gameNullable ?: throw ShowdownDoesNotAnswerException()
        return analyseFromLog(game, url, debugMode)
    }

    fun analyseFromLog(
        game: List<String>, url: String, debugMode: Boolean = false
    ): AnalysisData {
        var amount = 1
        val nicknames: MutableMap<Int, String> = mutableMapOf()
        var playerCount = 2
        val allMons = mutableMapOf<Int, MutableList<SDPokemon>>()
        var randomBattle = false
        for ((index, line) in game.withIndex()) {
            val split = line.cleanSplit().filter { it.isNotBlank() }
            if (line.startsWith("|poke|")) {
                val player = split[1][1].digitToInt() - 1
                allMons.getOrPut(player) { mutableListOf() }.add(
                    SDPokemon(
                        split[2].substringBefore(","), player
                    )
                )
            }
            if (line == "|gametype|doubles") amount = 2
            if (line == "|gametype|freeforall") playerCount = 4
            if (line.startsWith("|player|")) {
                val i = split[1][1].digitToInt() - 1
                //if (i !in nicknames)
                if (split.size > 2) nicknames[i] = split[2].also { logger.info("Setting nickname of $i to $it") }
            }
            if (line.startsWith("|switch") || line.startsWith("|drag")) {
                val (player, _) = split[1].parsePokemonLocation()
                val monName = split[2].substringBefore(",")
                if (player !in allMons && !randomBattle) randomBattle = true
                if (randomBattle) {
                    if (allMons[player]?.any { it.hasName(monName) } != true) {
                        allMons.getOrPut(player) { mutableListOf() }.add(SDPokemon(monName, player))
                    }
                }
            }
            if (line.startsWith("|detailschange|")) {
                val (player, _) = split[1].parsePokemonLocation()
                val oldMonName = split[1].substringAfter(" ")
                allMons[player]?.firstOrNull { it.hasName(oldMonName) || it.pokemon.startsWith(oldMonName) }?.otherNames?.add(
                    split[2].substringBefore(",")
                )
            }
            if (line.startsWith("|replace|")) {
                val (player, _) = split[1].parsePokemonLocation()
                val monloc = split[1].substringBefore(":")
                val monname = split[2].substringBefore(",")
                val mon = allMons[player]!!.firstOrNull { it.hasName(monname) } ?: SDPokemon(
                    monname, player
                ).also { allMons.getOrPut(player) { mutableListOf() }.add(it) }
                var downIndex = index - 1
                while (!game[downIndex].startsWith("|switch|$monloc")) downIndex--
                allMons[player]!!.first {
                    game[downIndex].cleanSplit()[2].substringBefore(",").startsWith(it.pokemon.replace("-*", ""))
                }.zoroLines[downIndex..index] = mon
            }
        }
        return with(
            BattleContext(
                url = url,
                monsOnField = List(playerCount) { buildDummys(amount) },
                sdPlayers = (0 until playerCount).map {
                    SDPlayer(
                        nicknames[it] ?: run {
                            File("replayerrors/$url${System.currentTimeMillis()}.txt").also { f -> f.createNewFile() }
                                .writeText(game.joinToString("\n"))
                            throw ShowdownParseException()
                        }, allMons[it].orEmpty().toMutableList()
                    )
                },
                randomBattle = randomBattle,
                game = game,
                debugMode = debugMode
            )
        ) {
            for (line in game) {
                currentLineIndex++
                val split = line.cleanSplit()
                if (split.isEmpty()) continue
                val operation = split[0]
                if (operation == "move") lastMove = IndexedValue(currentLineIndex, split[1])
                logger.debug(line)
                nextLine = IndexedValue(currentLineIndex + 1, game.getOrNull(currentLineIndex + 1) ?: "")
                lastLine = IndexedValue(currentLineIndex - 1, game.getOrNull(currentLineIndex - 1) ?: "")
                SDEffect.effects[operation]?.let { it.forEach { e -> e.execute(split) } }
            }
            logger.info("Finished analyse!")
            val totalDmg = totalDmgAmount.toDouble()
            var calcedTotalDmg = 0
            sdPlayers.flatMap { it.pokemon }.forEach {
                calcedTotalDmg += it.damageDealt
                logger.info(
                    "Pokemon: ${it.pokemon}: HP: ${it.hp} Dmg: ${it.damageDealt} Percent: ${
                        (it.damageDealt.toDouble() / totalDmg * 100.0).roundToDigits(
                            2
                        )
                    } Healed: ${it.healed}"
                )
            }
            logger.info("Total Dmg: $totalDmg, Calced: $calcedTotalDmg")
            AnalysisData(sdPlayers, this)
        }
    }

    private val logger = KotlinLogging.logger {}
    private val dummyPokemon = SDPokemon("dummy", 0)

    private fun buildDummys(amount: Int) = MutableList(amount) { dummyPokemon }

}

data class AnalysisData(
    val game: List<SDPlayer>, val ctx: BattleContext
)

sealed class ShowdownException : Exception()
class ShowdownDoesNotAnswerException : ShowdownException()
class ShowdownParseException : ShowdownException()
class ShowdownDoesntExistException : ShowdownException()
class InvalidReplayException : ShowdownException()
