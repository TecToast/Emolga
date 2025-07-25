package de.tectoast.emolga.utils.showdown

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.database.Database
import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.database.exposed.*
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.flo.SendFeatures
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.json.LeagueResult
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.records.toCoord
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import org.jetbrains.exposed.v1.r2dbc.insertIgnore
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
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
        listOf(urlProvided),
        customReplayChannel,
        resultchannelParam,
        message,
        fromReplayCommand,
        customGuild,
        withSort,
        analysisData,
        useReplayResultChannelAnyways
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

        logger.info("REPLAY! Channel: {}", message?.channel?.id ?: resultchannelParam.id)
        fun send(msg: String) {
            fromReplayCommand?.reply(msg) ?: resultchannelParam.sendMessage(msg).queue()
        }

        val selfMember = resultchannelParam.guild.selfMember
        if (fromReplayCommand != null && (resultchannelParam.type == ChannelType.TEXT && !selfMember.hasPermission(
                resultchannelParam, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND
            ) || ((resultchannelParam.type == ChannelType.GUILD_PUBLIC_THREAD || resultchannelParam.type == ChannelType.GUILD_PRIVATE_THREAD) && !selfMember.hasPermission(
                resultchannelParam, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND_IN_THREADS
            )))
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
            game.forEach { player ->
                player.pokemon.addAll(List((player.teamSize - player.pokemon.size).coerceAtLeast(0)) {
                    SDPokemon(
                        "_unbekannt_",
                        -1
                    )
                })
                // TODO: Refactor this
                player.pokemon.forEach { mon ->
                    mon.draftname = getMonName(mon.pokemon, gid)
                }
            }
            val leaguedata = db.leagueByGuildAdvanced(
                gid, game, ctx, null, uid1db, uid2db
            )
            league = leaguedata?.league
            val uindices = leaguedata?.uindices
            val draftPlayerList = game.map(SDPlayer::toDraftPlayer)
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
            // TODO: Refactor this
            val gamedayData = league?.getGamedayData(uindices!![0], uindices[1], draftPlayerList)
            val tosend = MessageCreate(
                content = url,
                embeds = league?.appendedEmbed(data, leaguedata, gamedayData!!)?.build()?.into().orEmpty()
            )
            replayChannel?.sendMessage(tosend)?.queue()
            fromReplayCommand?.reply(msgCreateData = tosend)
            val description = generateDescription(game, spoiler, leaguedata, ctx)
            if (league != null) {
                resultChannel.sendMessageEmbeds(Embed(description = description)).queue()
            } else {
                resultChannel.sendMessage(description).queue()
            }

            Database.dbScope.launch {
                AnalysisStatistics.addToStatistics(game, ctx)
                EmolgaMain.updatePresence()
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
                resultchannelParam.send(
                    "Die Kills und Tode stimmen nicht überein." + (if (shouldSendZoro) " Dies liegt wahrscheinlich an Zoroark." else " Dies liegt sehr wahrscheinlich an einem Bug in meiner Analyse.") + " Bitte überprüfe selbst, wo der Fehler liegt. Mein Programmierer wurde benachrichtigt."
                ).queue()
                SendFeatures.sendToMe((if (shouldSendZoro) "Zoroark... " else "") + "ACHTUNG ACHTUNG! KILLS SIND UNGLEICH DEATHS :o\n$url\n${resultchannelParam.asMention}")
            }
            logger.info("In Emolga Listener!")
            Database.dbScope.launch {
                dbTransaction {
                    val replayChannelId = fromReplayCommand?.tc ?: replayChannel?.idLong ?: return@dbTransaction
                    val endlessId =
                        EndlessLeagueChannelsDB.selectAll().where { EndlessLeagueChannelsDB.CHANNEL eq replayChannelId }
                            .firstOrNull()?.get(
                                EndlessLeagueChannelsDB.ID
                            ) ?: return@dbTransaction
                    if (EndlessLeagueDataDB.insertIgnore {
                            it[ID] = endlessId
                            it[URL] = url
                        }.insertedCount > 0) {
                        val newSize = EndlessLeagueDataDB.select(EndlessLeagueDataDB.ID)
                            .where { EndlessLeagueDataDB.ID eq endlessId }.count()
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
                    gamedayData = gamedayData!!,
                    otherForms = leaguedata.otherForms,
                )
            }
        }
        if (replayDatas.isNotEmpty() && league != null) {
            League.executeOnFreshLock(league.leaguename) {
                docEntry?.analyse(
                    replayDatas, withSort = withSort
                )
            }
        }
    }

    private fun generateDescription(
        game: List<SDPlayer>,
        spoiler: Boolean,
        leaguedata: LeagueResult?,
        ctx: BattleContext
    ): String {
        val monStrings = game.map { player ->
            player.pokemon.joinToString("\n") { mon ->
                mon.draftname.displayName.condAppend(mon.kills > 0, " ${mon.kills}")
                    .condAppend((!player.allMonsDead || spoiler) && mon.isDead, " X")
            }
        }
        val description = game.mapIndexed { index, sdPlayer ->
            mutableListOf<Any>(
                leaguedata?.mentions[index] ?: sdPlayer.nickname,
                sdPlayer.pokemon.count { !it.isDead }.minus(if (ctx.is4v4) 2 else 0)
            ).apply { if (spoiler) add(1, "||") }.let { if (index % 2 > 0) it.asReversed() else it }
        }.joinToString(":") { it.joinToString(" ") }
            .condAppend(ctx.is4v4, "\n(4v4)") + "\n\n" + game.mapIndexed { index, player ->
            "${leaguedata?.mentions[index] ?: player.nickname}:".condAppend(
                player.allMonsDead && !spoiler, " (alle tot)"
            ) + "\n".condAppend(spoiler, "||") + monStrings[index].condAppend(spoiler, "||")
        }.joinToString("\n\n")
        return description
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
                s, s, otherTl = s, otherOfficial = s
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

    val modeByServer = mapOf<String, ReplayServerMode>(
        "replay.pokemonshowdown.com" to ReplayServerMode.LOG,
        "replays.tectoast.de" to ReplayServerMode.LOG,
        "replay.reshowdown.top" to ReplayServerMode.LOG,
        "battling.p-insurgence.com/replays" to ReplayServerMode.SCRAPE
    )
    val regex =
        Regex("https://(${modeByServer.keys.joinToString("|")})/(?:[a-z]+-)?[^-]+-\\d+[-a-z0-9]*")


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
                if (split.size > 2) nicknames[i] = split[2].also { logger.debug { "Setting nickname of $i to $it" } }
            }
            if (line.startsWith("|switch") || line.startsWith("|drag")) {
                val player = split[1].parsePlayer()
                val monName = split[2].substringBefore(",")
                if (player !in allMons && !randomBattle) randomBattle = true
                if (randomBattle) {
                    if (allMons[player]?.any { it.hasName(monName) } != true) {
                        allMons.getOrPut(player) { mutableListOf() }.add(SDPokemon(monName, player))
                    }
                }
            }
            if (line.startsWith("|detailschange|") || line.startsWith("|-formechange|")) {
                val player = split[1].parsePlayer()
                val oldMonName = split[1].substringAfter(" ")
                allMons[player]?.firstOrNull { it.hasName(oldMonName) || it.pokemon.startsWith(oldMonName) }?.otherNames?.add(
                    split[2].substringBefore(",")
                )
            }
            if (line.startsWith("|replace|")) {
                val player = split[1].parsePlayer()
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
                if (operation == "move") {
                    lastMoveUser = IndexedValue(currentLineIndex, split[1])
                    lastMoveUsed = IndexedValue(currentLineIndex, split[2])
                }
                logger.debug(line)
                nextLine = IndexedValue(currentLineIndex + 1, game.getOrNull(currentLineIndex + 1) ?: "")
                lastLine = IndexedValue(currentLineIndex - 1, game.getOrNull(currentLineIndex - 1) ?: "")
                SDEffect.effects[operation]?.let { it.forEach { e -> e.execute(split) } }
            }
            logger.debug("Finished analyse!")
            val totalDmg = totalDmgAmount.toDouble()
            var calcedTotalDmg = 0
            sdPlayers.flatMap { it.pokemon }.forEach {
                calcedTotalDmg += it.damageDealt
                logger.debug {
                    "Pokemon: ${it.pokemon}: HP: ${it.hp} Dmg: ${it.damageDealt} Percent: ${
                        (it.damageDealt.toDouble() / totalDmg * 100.0).roundToDigits(
                            2
                        )
                    } Healed: ${it.healed}"
                }
            }
            logger.debug { "Total Dmg: $totalDmg, Calced: $calcedTotalDmg" }
            AnalysisData(sdPlayers, this)
        }
    }

    private val logger = KotlinLogging.logger {}
    private val dummyPokemon = SDPokemon("dummy", -1)

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
