package de.tectoast.emolga.utils.showdown

import de.tectoast.emolga.commands.AnalysisData
import de.tectoast.emolga.commands.httpClient
import de.tectoast.emolga.commands.roundToDigits
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.time.Duration.Companion.seconds

object Analysis {
    suspend fun analyse(link: String, answer: ((String) -> Unit)? = null, debugMode: Boolean = false): AnalysisData {
        var gameNullable: List<String>? = null
        for (i in 0..1) {
            val retrieved = runCatching {
                withContext(Dispatchers.IO) {
                    logger.info("Reading URL... {}", link)
                    httpClient.get("$link.log").bodyAsText().split("\n")
                }
            }.getOrDefault(listOf(""))
            gameNullable = retrieved.takeIf { it.size > 1 }
            if (gameNullable == null) {
                println(retrieved)
                println("Showdown antwortet nicht")
                answer?.invoke("Der Showdown-Server antwortet nicht, ich versuche es in 10 Sekunden erneut...")
                delay(10.seconds)
            } else break
        }
        logger.info("Starting analyse!")
        val game = gameNullable ?: throw ShowdownDoesNotAnswerException()
        return analyseFromString(game, link, debugMode)
    }

    fun analyseFromString(
        game: List<String>, link: String, debugMode: Boolean = false
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
                if (split.size > 2) nicknames[i] = split[2].also { logger.warn("Setting nickname of $i to $it") }
            }
            if (line.startsWith("|switch")) {
                val (player, _) = split[1].parsePokemonLocation()
                val monName = split[2].substringBefore(",")
                if (player !in allMons && !randomBattle) randomBattle = true
                if (randomBattle) {
                    allMons[player]?.any { it.hasName(monName) } == true || allMons.getOrPut(player) { mutableListOf() }
                        .add(SDPokemon(monName, player))
                }
            }
            if (line.startsWith("|detailschange|")) {
                val (player, _) = split[1].parsePokemonLocation()
                val oldMonName = split[1].substringAfter(" ")
                allMons[player]?.firstOrNull { it.hasName(oldMonName) }?.otherNames?.add(split[2].substringBefore(","))
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
                url = link,
                monsOnField = List(playerCount) { buildDummys(amount) },
                sdPlayers = (0 until playerCount).map {
                    SDPlayer(
                        nicknames[it] ?: run {
                            File("replayerrors/$link${System.currentTimeMillis()}.txt").also { f -> f.createNewFile() }
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
                val split = line.cleanSplit()
                if (split.isEmpty()) continue
                val operation = split[0]
                if (operation == "move") lastMove = line
                currentLineIndex++
                logger.debug(line)
                nextLine = game.getOrNull(currentLineIndex + 1) ?: ""
                SDEffect.effects[operation]?.let { it.forEach { e -> e.execute(split) } }
                lastLine = line
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

    private val logger = LoggerFactory.getLogger(Analysis::class.java)
    private val dummyPokemon = SDPokemon("dummy", 0)

    private fun buildDummys(amount: Int) = MutableList(amount) { dummyPokemon }

}

sealed class ShowdownException : Exception()
class ShowdownDoesNotAnswerException : ShowdownException()
class ShowdownParseException : ShowdownException()
