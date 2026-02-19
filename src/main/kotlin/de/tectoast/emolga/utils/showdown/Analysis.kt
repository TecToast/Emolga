package de.tectoast.emolga.utils.showdown

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.database.Database
import de.tectoast.emolga.database.exposed.*
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.draft.during.generic.K18n_NoWritePermissionInChannel
import de.tectoast.emolga.features.flo.SendFeatures
import de.tectoast.emolga.league.GamedayData
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.json.LeagueResult
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.json.mdb
import de.tectoast.k18n.generated.K18nLanguage
import de.tectoast.k18n.generated.K18nMessage
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import java.util.concurrent.TimeUnit
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
        val g = resultchannelParam.guild
        val gid = customGuild ?: g.idLong
        val lang = GuildLanguageDB.getLanguage(gid)
        suspend fun send(msg: K18nMessage) {
            fromReplayCommand?.reply(msg) ?: resultchannelParam.sendMessage(msg.translateTo(lang)).queue()
        }

        val selfMember = resultchannelParam.guild.selfMember
        if (fromReplayCommand != null && (resultchannelParam.type == ChannelType.TEXT && !selfMember.hasPermission(
                resultchannelParam, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND
            ) || ((resultchannelParam.type == ChannelType.GUILD_PUBLIC_THREAD || resultchannelParam.type == ChannelType.GUILD_PRIVATE_THREAD) && !selfMember.hasPermission(
                resultchannelParam, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND_IN_THREADS
            )))
        ) {
            send(K18n_NoWritePermissionInChannel(resultchannelParam.idLong))
            return
        }
        var league: League? = null
        var uindicesInOrder: List<Int>? = null
        var gamedayData: GamedayData? = null
        val games = mutableListOf<ReplayData>()
        for (urlProvided in urlsProvided) {

            val data = try {
                analysisData ?: analyse(urlProvided, ::send, resultchannelParam.guild.idLong == Constants.G.MY)
                //game = Analysis.analyse(url, m);
            } catch (ex: Exception) {
                when (ex) {
                    is ShowdownDoesNotAnswerException -> {
                        send(K18n_Analysis.ErrorShowdownDoesNotAnswer)
                    }

                    is ShowdownDoesntExistException -> {
                        send(K18n_Analysis.ErrorShowdownDoesntExist)
                    }

                    is ShowdownParseException -> {
                        send(K18n_Analysis.ErrorShowdownParse)
                    }

                    is InvalidReplayException -> {
                        send(K18n_Analysis.ErrorInvalidReplay)
                    }

                    else -> {
                        send(K18n_Analysis.ErrorGeneric)
                        logger.error(
                            "Error on replay analysis: $urlProvided ${resultchannelParam.guild.name} ${resultchannelParam.asMention} ChannelID: ${resultchannelParam.id}",
                            ex
                        )
                    }
                }
                return
            }
            val (game, ctx, dontTranslateFromReplayServer) = data
            val url = ctx.url
            val u1 = game[0].nickname
            val u2 = game[1].nickname
            val uid1db = SDNamesDB.getIDByName(u1)
            val uid2db = SDNamesDB.getIDByName(u2)
            logger.info("Analysed!")
            val spoiler = SpoilerTagsDB.contains(gid)
            game.forEach { player ->
                player.pokemon.addAll(List((player.teamSize - player.pokemon.size).coerceAtLeast(0)) {
                    SDPokemon(
                        "_unbekannt_", -1
                    )
                })
                // TODO: Refactor this
                player.pokemon.forEach { mon ->
                    mon.draftname = getMonName(mon.pokemon, gid)
                }
            }
            val leaguedata = mdb.leagueByGuildAdvanced(
                gid, game, ctx, null, uid1db, uid2db
            )
            league = leaguedata?.league
            val uindices = leaguedata?.uindices
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
            val gamedayDataPair = league?.getGamedayData(uindices!![0], uindices[1])
            val tosend = MessageCreate(
                content = url,
                embeds = league?.appendedEmbed(data, leaguedata.uindices, gamedayDataPair!!.first.gameday)?.build()
                    ?.into().orEmpty()
            )
            val gameday = gamedayDataPair?.first?.gameday
            val shouldntSend = gameday != null && league.config.hideGames?.gamedays?.contains(gameday) == true
            if (shouldntSend) {
                send(K18n_Analysis.BattleSaved)
                fromReplayCommand?.hook?.deleteOriginal()?.queueAfter(3, TimeUnit.SECONDS)
                with(league) {
                    (fromReplayCommand?.messageChannel ?: replayChannel)!!.sendResultEntryMessage(
                        gameday,
                        ResultEntryDescription.MatchPresent(
                            uindices.reversedIf(gamedayDataPair.second).map { this[it] })
                    )
                }
            } else {
                replayChannel?.sendMessage(tosend)?.queue()
                fromReplayCommand?.reply(msgCreateData = tosend)
                val dontTranslate = dontTranslateFromReplayServer || EnglishResultsDB.contains(gid)
                val description = generateDescription(game, spoiler, leaguedata, ctx, dontTranslate, lang)
                if (league != null) {
                    resultChannel.sendMessageEmbeds(Embed(description = description)).queue()
                } else {
                    resultChannel.sendMessage(description).queue()
                }
                var shouldSendZoro = false
                for (ga in game) {
                    if (ga.containsZoro()) {
                        resultchannelParam.sendMessage(
                            K18n_Analysis.IllusionWarning(ga.nickname).translateTo(lang)
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
                        K18n_Analysis.KillsDeathsNotMatching(
                            (if (shouldSendZoro) K18n_Analysis.PotentialZoroark else K18n_Analysis.OtherIssue).translateTo(
                                lang
                            )
                        ).translateTo(lang)
                    ).queue()
                    SendFeatures.sendToMe((if (shouldSendZoro) "Zoroark... " else "") + "ACHTUNG ACHTUNG! KILLS SIND UNGLEICH DEATHS :o\n$url\n${resultchannelParam.asMention}")
                }
            }

            AnalysisStatistics.addToStatisticsSync(ctx)
            Database.dbScope.launch {
                EmolgaMain.updatePresence()
            }

            logger.info("In Emolga Listener!")
            if (leaguedata != null) {
                val gameInGameplanOrder = game.reversedIf(gamedayDataPair!!.second)
                gamedayData = gamedayDataPair.first
                if (uindicesInOrder == null) {
                    uindicesInOrder = uindices.reversedIf(gamedayDataPair.second)
                }
                val kd = gameInGameplanOrder.map {
                    it.pokemon.associate { p ->
                        p.draftname.official to KD(
                            p.kills, if (p.isDead) 1 else 0
                        )
                    }
                }
                games += ReplayData(
                    kd = kd, url = url, winnerIndex = gameInGameplanOrder.indexOfFirst { it.winnerOfGame }
                )
            }
        }
        if (games.isNotEmpty() && league != null) {
            League.executeOnFreshLock(league.leaguename) {
                docEntry?.analyse(
                    FullGameData(uindices = uindicesInOrder!!, gamedayData = gamedayData!!, games = games),
                    withSort = withSort
                )
            }
        }
    }

    private fun generateDescription(
        game: List<SDPlayer>,
        spoiler: Boolean,
        leaguedata: LeagueResult?,
        ctx: BattleContext,
        dontTranslate: Boolean,
        lang: K18nLanguage
    ): String {
        val monStrings = game.map { player ->
            player.pokemon.joinToString("\n") { mon ->
                val pokemonName = if (dontTranslate) mon.pokemon else mon.draftname.displayName
                pokemonName.condAppend(mon.kills > 0, " ${mon.kills}")
                    .condAppend((!player.allMonsDead || spoiler) && mon.isDead, " X")
            }
        }
        val allDead = K18n_Analysis.AllDead.translateTo(lang)
        val description = game.mapIndexed { index, sdPlayer ->
            mutableListOf<Any>(
                leaguedata?.mentions[index] ?: sdPlayer.nickname,
                sdPlayer.pokemon.count { !it.isDead }.minus(if (ctx.is4v4) 2 else 0)
            ).apply { if (spoiler) add(1, "||") }.let { if (index % 2 > 0) it.asReversed() else it }
        }.joinToString(":") { it.joinToString(" ") }
            .condAppend(ctx.is4v4, "\n(4v4)") + "\n\n" + game.mapIndexed { index, player ->
            "${leaguedata?.mentions[index] ?: player.nickname}:".condAppend(
                player.allMonsDead && !spoiler, allDead
            ) + "\n".condAppend(spoiler, "||") + monStrings[index].condAppend(spoiler, "||")
        }.joinToString("\n\n")
        return description
    }

    suspend fun getMonName(s: String, guildId: Long, withDebug: Boolean = false): DraftName {
        if (withDebug) logger.info("s = $s")
        val split = s.split("-")
        val withoutLast = split.dropLast(1).joinToString("-")
        if (split.last() == "*") return getMonName(withoutLast, guildId, withDebug)
        return if (s == "_unbekannt_") DraftName("_unknown_", "UNKNOWN")
        else {
            var pkdata = mdb.pokedex.get(s.toSDName())
            (NameConventionsDB.getSDTranslation(pkdata?.takeIf { it.requiredAbility != null }?.baseSpecies?.also {
                pkdata = mdb.pokedex.get(
                    it.toSDName()
                )
            } ?: s, guildId) ?: DraftName(
                s, s, otherTl = s, otherOfficial = s
            )).apply { data = pkdata }
        }
        //}
    }


    enum class ReplayServerMode(val dontTranslate: Boolean) {
        LOG(false) {
            override fun getLogFromWebsiteText(text: String) = text
            override fun mapURL(url: String) = "$url.log"
        },
        SCRAPE(false) {
            private val logRegex =
                Regex("<script type=\"text/plain\" class=\"log\">(.*?)</script>", RegexOption.DOT_MATCHES_ALL)

            override fun getLogFromWebsiteText(text: String) = logRegex.find(text)?.groupValues[1] ?: ""
        },
        POKEATHLON(true) {
            override fun getLogFromWebsiteText(text: String) = text

            override fun mapURL(url: String) = "https://sim.pokeathlon.com/replays/${url.substringAfterLast("=")}.log"
        };

        abstract fun getLogFromWebsiteText(text: String): String
        open fun mapURL(url: String) = url
    }

    val modeByServer = mapOf<String, ReplayServerMode>(
        "replay.pokemonshowdown.com" to ReplayServerMode.LOG,
        "replays.tectoast.de" to ReplayServerMode.LOG,
        "replay.reshowdown.top" to ReplayServerMode.LOG,
        "battling.p-insurgence.com/replays" to ReplayServerMode.SCRAPE,
        "replay.pokeathlon.com" to ReplayServerMode.POKEATHLON,
    )
    val regex = Regex("https://(${modeByServer.keys.joinToString("|")}).*")


    suspend fun analyse(
        urlProvided: String, answer: (suspend (K18nMessage) -> Unit)? = null, debugMode: Boolean = false
    ): AnalysisData {
        var gameNullable: List<String>? = null
        val mr = regex.find(urlProvided) ?: throw InvalidReplayException()
        val mode = modeByServer[mr.groupValues[1]] ?: throw InvalidReplayException()
        val url = mr.groupValues[0]
        val mappedURL = mode.mapURL(url)
        for (i in 0..1) {
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
                answer?.invoke(K18n_Analysis.ShowdownDoesntAnswer)
                delay(10.seconds)
            } else break
        }
        logger.info("Starting analyse!")
        val game = gameNullable ?: throw ShowdownDoesNotAnswerException()
        return analyseFromLog(game, url, debugMode, mode.dontTranslate)
    }

    fun analyseFromLog(
        game: List<String>, url: String, debugMode: Boolean = false, dontTranslate: Boolean = false,
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
            AnalysisData(sdPlayers, this, dontTranslate)
        }
    }

    private val logger = KotlinLogging.logger {}
    private val dummyPokemon = SDPokemon("dummy", -1)

    private fun buildDummys(amount: Int) = MutableList(amount) { dummyPokemon }

}

data class AnalysisData(
    val game: List<SDPlayer>, val ctx: BattleContext, val dontTranslate: Boolean
)

sealed class ShowdownException : Exception()
class ShowdownDoesNotAnswerException : ShowdownException()
class ShowdownParseException : ShowdownException()
class ShowdownDoesntExistException : ShowdownException()
class InvalidReplayException : ShowdownException()
