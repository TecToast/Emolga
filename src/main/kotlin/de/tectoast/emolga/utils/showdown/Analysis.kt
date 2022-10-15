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
    suspend fun analyse(link: String): List<SDPlayer> {
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
        val allMons: MutableList<SDPokemon> = mutableListOf()
        val nicknames: MutableList<String> = mutableListOf()
        var playerCount = 2
        for (line in game) {
            val split = line.cleanSplit()
            if (line.startsWith("|poke|")) {
                allMons.add(SDPokemon(split[2].substringBefore(","), split[1][1].digitToInt() - 1))
            }
            if (line == "|gametype|doubles") amount = 2
            if (line == "|gametype|freeforall") playerCount = 4
            if (line.startsWith("|player|")) nicknames += split[2]
            if (line == "|teampreview") break
        }
        val distinctedNicknames = nicknames.distinct()
        val bothMons = allMons.groupBy { it.player }
        println(allMons.joinToString { "${it.pokemon} ${it.player}" })
        return with(
            BattleContext(
                List(playerCount) { buildDummys(amount) },
                "",
                (0 until playerCount).map {
                    SDPlayer(
                        distinctedNicknames[it],
                        bothMons[it].orEmpty().toMutableList()
                    )
                }
            )
        ) {
            for (line in game) {
                val split = line.cleanSplit()
                if (split.isEmpty()) continue
                val operation = split[0]
                if (operation == "move") lastMove = line
                SDEffect.effects[operation]?.let { it.forEach { e -> e.execute(split, this) } }
                lastLine = line
            }
            sdPlayers
        }
    }

    private val logger = LoggerFactory.getLogger(Analysis::class.java)
    private val dummyPokemon = SDPokemon("dummy", 0)

    private fun buildDummys(amount: Int) = MutableList(amount) { dummyPokemon }

}
