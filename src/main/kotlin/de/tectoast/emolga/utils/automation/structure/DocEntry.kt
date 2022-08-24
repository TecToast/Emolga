package de.tectoast.emolga.utils.automation.structure

import de.tectoast.emolga.commands.Command.Companion.compareColumns
import de.tectoast.emolga.commands.Command.Companion.getGameDay
import de.tectoast.emolga.commands.Command.Companion.getNumber
import de.tectoast.emolga.commands.Command.Companion.indexPick
import de.tectoast.emolga.commands.ReplayData
import de.tectoast.emolga.commands.names
import de.tectoast.emolga.utils.Google
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.records.SorterData
import de.tectoast.emolga.utils.records.StatLocation
import de.tectoast.emolga.utils.showdown.Player
import de.tectoast.emolga.utils.showdown.Pokemon
import org.slf4j.LoggerFactory

class DocEntry private constructor() {
    companion object {
        private val logger = LoggerFactory.getLogger(DocEntry::class.java)

        private val invalidProcessor: BasicStatProcessor =
            BasicStatProcessor { _: Int, _: Int, _: Int -> StatLocation.invalid() }

        fun create(builder: DocEntry.() -> Unit): DocEntry {
            return DocEntry().apply(builder)
        }
    }

    lateinit var league: League
    var killProcessor: StatProcessor = invalidProcessor
    var deathProcessor: StatProcessor = invalidProcessor
    var useProcessor: BasicStatProcessor = invalidProcessor
    var winProcessor: ResultStatProcessor? = null
    var looseProcessor: ResultStatProcessor? = null
    var resultCreator: ResultCreator? = null
    var sorterData: SorterData? = null
    var setStatIfEmpty = false
    var numberMapper: (String) -> String = { it.ifEmpty { "0" } }
    private var onlyKilllist: (() -> List<String>)? = null

    fun analyse(
        game: Array<Player>,
        uid1: Long,
        uid2: Long,
        kills: List<Map<String, String>>,
        deaths: List<Map<String, String>>,
        replayData: ReplayData
    ) {
        val gameday = getGameDay(league, uid1, uid2)
        val sid = league.sid
        val b = RequestBuilder(sid)
        onlyKilllist?.run {
            val mons = this()
            var monIndex = -1
            for (pick in mons) {
                monIndex++
                for (i in 0..1) {
                    val death = getNumber(
                        deaths[i],
                        pick
                    )
                    if (death.isEmpty() && !setStatIfEmpty) continue
                    val k = (killProcessor as BasicStatProcessor).process(0, monIndex, gameday)
                    if (k.isValid) b.addSingle(
                        k.toString(), numberMapper(
                            getNumber(kills[i], pick)
                        )
                    )
                    val d = (deathProcessor as BasicStatProcessor).process(0, monIndex, gameday)
                    if (d.isValid) b.addSingle(
                        d.toString(),
                        numberMapper(death)
                    )
                    val u = useProcessor.process(0, monIndex, gameday)
                    if (u.isValid) b.addSingle(
                        u.toString(),
                        numberMapper("1")
                    )
                }
            }
            b.execute()
            return
        }
        val uids = listOf(uid1, uid2)
        val picksJson = league.picks
        for ((i, uid) in uids.withIndex()) {
            val index = league.table.indexOf(uid)
            val picks = picksJson[uid].names()
            var monIndex = -1
            var totalKills = 0
            var totalDeaths = 0
            for (pick in picks) {
                monIndex++
                val death = getNumber(deaths[i], pick)
                if (death.isEmpty() && !setStatIfEmpty) continue
                if (killProcessor is BasicStatProcessor) {
                    val k = (killProcessor as BasicStatProcessor).process(index, monIndex, gameday)
                    if (k.isValid) b.addSingle(
                        k.toString(), numberMapper(
                            getNumber(
                                kills[i],
                                pick
                            )
                        )
                    )
                } else if (killProcessor is CombinedStatProcessor) {
                    totalKills += getNumber(kills[i], pick).toInt()
                }
                if (deathProcessor is BasicStatProcessor) {
                    val d = (deathProcessor as BasicStatProcessor).process(index, monIndex, gameday)
                    if (d.isValid) b.addSingle(d.toString(), numberMapper(death))
                } else if (deathProcessor is CombinedStatProcessor) {
                    totalDeaths += death.toInt()
                }
                val u = useProcessor.process(index, monIndex, gameday)
                if (u.isValid) b.addSingle(u.toString(), numberMapper("1"))
            }
            (if (game[i].isWinner) winProcessor else looseProcessor)?.process(index, gameday)
                ?.run { b.addSingle(this.toString(), 1) }
            if (killProcessor is CombinedStatProcessor) {
                b.addSingle((killProcessor as CombinedStatProcessor).process(index, gameday).toString(), totalKills)
            }
            if (deathProcessor is CombinedStatProcessor) {
                b.addSingle((deathProcessor as CombinedStatProcessor).process(index, gameday).toString(), totalDeaths)
            }
            if (game[i].isWinner) {
                league.results["$uid1:$uid2"] = uid
            }
        }
        val battleorder = league.battleorder[gameday]!!.split(";")
        val battleindex =
            battleorder.indices.firstOrNull { battleorder[it].contains(uid1.toString()) } ?: -1
        val battle = battleorder.firstOrNull { it.contains(uid1.toString()) } ?: ""
        val battleusers = battle.split(":")
        val numbers = (0..1).asSequence()
            .sortedBy { battleusers.indexOf(uids[it].toString()) }
            .map { game[it].mons.count { m: Pokemon -> !m.isDead } }
            .toList()
        resultCreator?.run {
            if (this is BasicResultCreator) {
                process(b, gameday - 1, battleindex, numbers[0], numbers[1], replayData.url)
            } else if (this is AdvancedResultCreator) {
                val monList = (0..1).map { deaths[it].keys }.map { it.toList() }
                val picks = (0..1).map(uids::get).map { picksJson[it].names() }
                process(b, gameday - 1, battleindex, numbers[0], numbers[1], replayData.url,
                    monList,
                    (0..1).map { league.table.indexOf(uids[it]) },
                    (0..1).map { monList[it].map { s -> indexPick(picks[it], s) } },
                    (0..1).map { deaths[it].values.map { s -> s == "1" } }
                )
            }
        }
        b.withRunnable(3000) { sort(sid, league) }.execute()
    }

    fun sort(sid: String?, league: League) {
        try {
            logger.info("Start sorting...")
            val b = RequestBuilder(sid!!)
            sorterData?.run {
                for (num in formulaRange.indices) {
                    val formulaRange = formulaRange[num]
                    val formula = Google[sid, formulaRange, true]
                    val points = Google[sid, formulaRange, false].toMutableList()
                    val orig: List<List<Any>?> = ArrayList(points)
                    val table = league.table
                    points.sortWith { o1: List<Any>, o2: List<Any> ->
                        val arr = cols.toList()
                        val first =
                            if (directCompare) arr.subList(0, arr.indexOf(-1)) else arr
                        val c = compareColumns(
                            o1, o2, *first.toIntArray()
                        )
                        if (c != 0) return@sortWith c
                        if (!directCompare) return@sortWith 0
                        val u1 =
                            table[indexer!!.apply(formula[orig.indexOf(o1)][0].toString())]
                        val u2 =
                            table[indexer.apply(formula[orig.indexOf(o2)][0].toString())]

                        val o = league.results
                        o["$u1:$u2"]?.let { return@sortWith if (it == u1) 1 else -1 }
                        o["$u2:$u1"]?.let { return@sortWith if (it == u1) 1 else -1 }
                        val second: List<Int> = arr.subList(arr.indexOf(-1), arr.size)
                        if (second.size > 1) return@sortWith compareColumns(
                            o1, o2, *second.drop(1).toIntArray()
                        )
                        0
                    }
                    points.reverse()
                    val namap = mutableMapOf<Int, List<Any>>()
                    for ((i, objects) in orig.withIndex()) {
                        namap[points.indexOf(objects)] = formula[i]
                    }
                    val sendname: MutableList<List<Any>?> = ArrayList()
                    for (j in points.indices) {
                        sendname.add(namap[j])
                    }
                    b.addAll(formulaRange.substring(0, formulaRange.indexOf(':')), sendname)
                }
                b.execute()
            }
        } catch (ex: Exception) {
            logger.error("I hate my life", ex)
        }
    }
}


interface StatProcessor

fun interface BasicStatProcessor : StatProcessor {
    fun process(plindex: Int, monindex: Int, gameday: Int): StatLocation
}

fun interface CombinedStatProcessor : StatProcessor {
    fun process(plindex: Int, gameday: Int): StatLocation
}

fun interface ResultStatProcessor {
    fun process(plindex: Int, gameday: Int): StatLocation
}

fun interface BasicResultCreator : ResultCreator {
    fun process(b: RequestBuilder, gdi: Int, index: Int, numberOne: Int, numberTwo: Int, url: String)
}

fun interface AdvancedResultCreator : ResultCreator {
    fun process(
        b: RequestBuilder?,
        gdi: Int,
        index: Int,
        numberOne: Int,
        numberTwo: Int,
        url: String?,
        mons: List<List<String?>?>?,
        tableIndexes: List<Int?>?,
        monIndexes: List<List<Int?>?>?,
        dead: List<List<Boolean?>?>?
    )
}

interface ResultCreator