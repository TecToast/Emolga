package de.tectoast.emolga.league.config

import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.SizeLimitedMap
import de.tectoast.emolga.utils.add
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.json.mdb
import de.tectoast.emolga.utils.randomWithCondition
import de.tectoast.emolga.utils.toSDName
import kotlinx.serialization.Serializable

@Serializable
data class RandomPickRoundConfig(
    val rounds: Set<Int> = setOf(), val tiers: Map<String, Int>, val doubleTypeOptOut: Set<Int> = emptySet()
) {
    suspend fun League.getRandomMon(): DraftName {
        val optOut = current in doubleTypeOptOut
        val usedTiers = draftData.randomPickRound.usedTiers
        val tier = tiers.entries.randomWithCondition { (tier, amount) ->
            (usedTiers[current]?.get(tier) ?: 0) < amount
        }!!.key
        val list = tierlist.getByTier(tier)!!.shuffled()
        val typesSoFar = picks[current].orEmpty().flatMap {
            typeCache.getOrPut(it.name) {
                mdb.pokedex.get(
                    NameConventionsDB.getSDTranslation(
                        it.name, guild, english = true
                    )!!.official.toSDName()
                )!!.types
            }
        }.groupingBy { it }.eachCount()
        var bestSoFar: Pair<Int, DraftName>? = null
        firstAvailableMon(list) { german, english ->
            if (optOut) return this
            val types = typeCache.getOrPut(german) { mdb.pokedex.get(english.toSDName())!!.types }
            val count = types.groupingBy { it }.eachCount()
            val score = count.entries.sumOf { (type, amount) ->
                (typesSoFar[type] ?: 0) * amount
            }
            if (bestSoFar == null || score > bestSoFar.first) {
                bestSoFar = score to this
            }
            bestSoFar.first == 0
        }
        usedTiers.getOrPut(current) { mutableMapOf() }.add(tier, 1)
        return bestSoFar?.second ?: error("No mon found")
    }

    companion object {
        val typeCache = SizeLimitedMap<String, List<String>>(200)
    }
}

@Serializable
data class RandomPickRoundData(val usedTiers: MutableMap<Int, MutableMap<String, Int>> = mutableMapOf())