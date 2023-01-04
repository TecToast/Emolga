package de.tectoast.emolga.utils.showdown

import de.tectoast.emolga.commands.httpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

object Analysis {
    // generate a new analysis engine
    suspend fun analyse(link: String): Pair<List<SDPlayer>, BattleContext> {
        logger.info("Reading URL... {}", link)
        var gameNullable: List<String>? = null
        for (i in 0..1) {
            gameNullable = httpClient.get("$link.log").bodyAsText().split("\n")
            if (gameNullable.size == 1) {
                println("Showdown antwortet nicht")
                delay(10.seconds)
            } else break
        }
        logger.info("Starting analyse!")
        val game = gameNullable ?: throw IOException("Could not read game")
        var amount = 1
        val nicknames: MutableMap<Int, String> = mutableMapOf()
        var playerCount = 2
        val allMons = mutableMapOf<Int, MutableList<SDPokemon>>()
        var randomBattle = false
        for ((index, line) in game.withIndex()) {
            val split = line.cleanSplit()
            if (line.startsWith("|poke|")) {
                val player = split[1][1].digitToInt() - 1
                allMons.getOrPut(player) { mutableListOf() }.add(
                    SDPokemon(
                        split[2].substringBefore(","),
                        player
                    )
                )
            }
            if (line == "|gametype|doubles") amount = 2
            if (line == "|gametype|freeforall") playerCount = 4
            if (line.startsWith("|player|")) {
                val i = split[1][1].digitToInt() - 1
                if (i !in nicknames)
                    nicknames[i] = split[2]
            }
            if (line.startsWith("|switch")) {
                val (player, _) = split[1].parsePokemonLocation()
                val monName = split[2].substringBefore(",")
                if (player !in allMons && !randomBattle) randomBattle = true
                if (randomBattle) {
                    allMons[player]?.any { it.pokemon == monName } == true || allMons.getOrPut(player) { mutableListOf() }
                        .add(SDPokemon(monName, player))
                }
            }
            if (line.startsWith("|replace|")) {
                val (player, _) = split[1].parsePokemonLocation()
                val monloc = split[1].substringBefore(":")
                val monname = split[2].substringBefore(",")
                val mon = allMons[player]!!.firstOrNull { it.pokemon == monname } ?: SDPokemon(
                    monname,
                    player
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
                List(playerCount) { buildDummys(amount) },
                "",
                (0 until playerCount).map {
                    SDPlayer(
                        nicknames[it]!!,
                        allMons[it].orEmpty().toMutableList()
                    )
                },
                randomBattle = randomBattle
            )
        ) {
            for (line in game) {
                val split = line.cleanSplit()
                if (split.isEmpty()) continue
                val operation = split[0]
                if (operation == "move") lastMove = line
                currentLineIndex++
                SDEffect.effects[operation]?.let { it.forEach { e -> e.execute(split, this) } }
                lastLine = line
            }
            sdPlayers to this
        }
    }

    private val logger = LoggerFactory.getLogger(Analysis::class.java)
    private val dummyPokemon = SDPokemon("dummy", 0)

    private fun buildDummys(amount: Int) = MutableList(amount) { dummyPokemon }

}
