package de.tectoast.emolga.domain.game.service.process.analysis

import de.tectoast.emolga.domain.game.model.analysis.*
import de.tectoast.emolga.utils.showdown.K18n_Analysis
import de.tectoast.k18n.generated.K18nMessage
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.seconds

@Single
class AnalysisService(val httpClient: HttpClient) {

    private val modeByServer = mapOf(
        "replay.pokemonshowdown.com" to ReplayServerMode.LOG,
        "replays.tectoast.de" to ReplayServerMode.LOG,
        "replay.reshowdown.top" to ReplayServerMode.LOG,
        "battling.p-insurgence.com/replays" to ReplayServerMode.SCRAPE,
        "play.champsnatdex.dynv6.net/replays" to ReplayServerMode.SCRAPE,
        "replay.pokeathlon.com" to ReplayServerMode.POKEATHLON,
    )
    private val regex = Regex("https://(${modeByServer.keys.joinToString("|")}).*")


    suspend fun analyse(
        urlProvided: String, answer: (suspend (K18nMessage) -> Unit)? = null
    ): AnalysisData {
        var gameNullable: List<String>? = null
        val mr = regex.find(urlProvided) ?: throw InvalidReplayException()
        val mode = modeByServer[mr.groupValues[1]] ?: throw InvalidReplayException()
        val url = mr.groupValues[0]
        val mappedURL = mode.mapURL(url)
        for (unused in 0..1) {
            var statusCode: HttpStatusCode? = null
            val retrieved = runCatching {
                logger.info("Reading URL... {}", url)
                val text = httpClient.get(mappedURL).also { statusCode = it.status }.bodyAsText()
                mode.getLogFromWebsiteText(text).split("\n")
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
        return analyseFromLog(game, url, mode.dontTranslate)
    }

    fun analyseFromLog(
        game: List<String>, url: String, dontTranslate: Boolean = false,
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

    private fun Double.roundToDigits(digits: Int) = "%.${digits}f".format(this)

    private val logger = KotlinLogging.logger {}
    private val dummyPokemon = SDPokemon("dummy", -1)

    private fun buildDummys(amount: Int) = MutableList(amount) { dummyPokemon }
}