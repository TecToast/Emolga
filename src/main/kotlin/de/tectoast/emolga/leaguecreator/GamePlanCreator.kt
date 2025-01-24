package de.tectoast.emolga.leaguecreator

import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.records.Coord
import org.slf4j.LoggerFactory

class GamePlanCreator private constructor() {
    var players: Int = -1
    lateinit var indexWrapper: (Int) -> Coord
    lateinit var locationWrapper: (Int) -> Coord
    var advancedLocationWrapper: ((Int, Int) -> String)? = null

    var sid: String = ""
    var format: String = ""
    var gapAfter = 0
    var gapSize = 1
    var randomize = false
    var reversed = false
    val additionalLocations: MutableList<(Int) -> Coord> = mutableListOf()
    var requestBuilder: RequestBuilder? = null

    var disabledDoc = false
    var divisions: List<List<Int>>? = null
    var directFormatSupplier: ((List<Int>) -> String)? = null

    var divisionProvider: ((String) -> List<List<Int>>)? = null

    var startGameDay = 1
    val startGdi by lazy { startGameDay - 1 }

    var pregeneratedIndexes: Map<Int, List<List<Int>>>? = null

    fun execute(): Map<Int, List<List<Int>>> {
        val b = requestBuilder ?: RequestBuilder(sid)
        val indexes = (pregeneratedIndexes ?: (divisionProvider?.invoke(b.sid) ?: divisions)?.let { divs ->
            val indexes = mutableMapOf<Int, MutableList<List<Int>>>()
            divs.forEach { div ->
                val gen = generateIndexes(div.size, randomize, reversed)
                for (i in 1 until div.size) {
                    indexes.getOrPut(i) { mutableListOf() }.addAll(gen[i]!!.map { l -> l.map { div[it] } })
                }
            }
            indexes
        } ?: generateIndexes(players, randomize, reversed)).let {
            val map = mutableMapOf<Int, List<List<Int>>>()
            it.keys.forEach { i ->
                map[i + startGdi] = it[i]!!
            }
            map
        }
        (0 + startGdi until indexes.size + startGdi).forEach { i ->
            val body = indexes[i + 1]!!.map { mu ->
                (directFormatSupplier?.invoke(mu) ?: mu.joinToString(format) {
                    indexWrapper(it).toString().let { iw -> "=" + iw.replace("=", "") }
                }).split("#")
            }.toMutableList().run {
                advancedLocationWrapper?.let { al ->
                    for (mu in withIndex()) {
                        b.addRow(al(i, mu.index), mu.value)
                    }
                    return@run this
                }
                logger.info("gapAfter = {}, gapSize = {}", gapAfter, gapSize)
                if (gapAfter == 0) return@run this
                val size = this.size
                logger.info("size = {}", size)
                val list = mutableListOf<List<String>>()
                for (j in 0 until size) {
                    list.add(this[j])
                    if ((j + 1) % gapAfter == 0) {
                        repeat(gapSize) {
                            list.add(emptyList())
                        }
                    }
                }
                list
            }
            if (!disabledDoc && advancedLocationWrapper == null) {
                b.addAll(locationWrapper(i), body)
                additionalLocations.forEach { loc ->
                    b.addAll(loc(i), body)
                }
            }
        }
        if (requestBuilder == null && !disabledDoc)
            b.execute()
        return indexes
    }


    companion object {
        private val logger = LoggerFactory.getLogger(GamePlanCreator::class.java)

        fun generateIndexes(
            size: Int,
            randomized: Boolean = false,
            reversed: Boolean = false
        ): Map<Int, List<List<Int>>> {
            val numDays = size - 1
            val halfSize = size / 2
            return buildMap {
                val list = mutableListOf<MutableList<List<Int>>>()
                for (day in 0 until numDays) {
                    val file = mutableListOf<List<Int>>()
                    val teamIdx = day % numDays + 1
                    file.add(listOf(teamIdx, 0).let { if (randomized) it.shuffled() else it })
                    for (idx in 1 until halfSize) {
                        val firstTeam = (day + idx) % numDays + 1
                        val secondTeam = (day + numDays - idx) % numDays + 1
                        file.add(listOf(firstTeam, secondTeam).let { if (randomized) it.shuffled() else it })
                    }
                    list += file
                }
                if (randomized) {
                    list.shuffle()
                    list.forEach { it.shuffle() }
                }
                for (i in 0 until numDays) {
                    this[i + 1] = list[i]
                }
                if (reversed) {
                    val copy = this.toMutableMap()
                    for (i in 1..numDays) {
                        this[i] = copy[numDays - i + 1]!!
                    }
                }
            }
        }

        @JvmStatic
        fun create(builder: GamePlanCreator.() -> Unit): GamePlanCreator {
            return GamePlanCreator().apply(builder)
        }

        fun LeagueCreator.fromLeagueCreator(builder: GamePlanCreator.() -> Unit): GamePlanCreator {
            return create {
                players = this@fromLeagueCreator.playerCount
                indexWrapper = playerNameIndexer
                builder()
            }
        }
    }
}
