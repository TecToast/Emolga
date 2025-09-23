package de.tectoast.emolga.utils.records

import de.tectoast.emolga.utils.DocEntry
import de.tectoast.emolga.utils.Google
import de.tectoast.emolga.utils.OneTimeCache
import de.tectoast.emolga.utils.UserTableData
import de.tectoast.emolga.utils.json.LeagueEvent
import de.tectoast.emolga.utils.json.db
import org.bson.Document
import org.litote.kmongo.eq

sealed class TableSorter(val formulaRange: String, val indexer: (String) -> Int, val docEntry: DocEntry) {
    val formulaRangeParsed = DocRange[formulaRange]
    abstract suspend fun getSortedFormulas(): List<List<Any>>
}

fun DocEntry.defaultSorter(formulaRange: String, indexer: (String) -> Int, sortOptions: List<TableSortOption>) =
    DefaultSorter(this, formulaRange, indexer, sortOptions)

fun DocEntry.newSystemSorter(formulaRange: String, sortOptions: List<TableSortOption>) =
    DefaultSorter(this, formulaRange, { str: String ->
        rowNumToIndex(
            str.replace("$", "").substring(league.dataSheet.length + 4).substringBefore(":").toInt()
        )
    }, sortOptions)

class DefaultSorter(
    docEntry: DocEntry, formulaRange: String, indexer: (String) -> Int, val sortOptions: List<TableSortOption>
) : TableSorter(formulaRange, indexer, docEntry) {
    override suspend fun getSortedFormulas(): List<List<Any>> {
        return with(TableSortDataStorage(this)) {
            val league = docEntry.league
            val indices = league.table.indices.toMutableList()
            val sortedIndices = if (sortOptions.none { it is DirectCompareSortOption }) {
                indices.insertionSortSuspending { a, b ->
                    for (option in sortOptions) {
                        val res = with(option) { getValue(a).compareTo(getValue(b)) }
                        if (res != 0) return@insertionSortSuspending -res
                    }
                    0
                }
            } else {
                val colsUntilDirect = sortOptions.takeWhile { it !is DirectCompareSortOption }
                val directOptions = sortOptions.first { it is DirectCompareSortOption } as DirectCompareSortOption
                val colsAfterDirect = sortOptions.dropWhile { it !is DirectCompareSortOption }.drop(1)
                val groups = indices.groupBy {
                    colsUntilDirect.map { option -> with(option) { getValue(it) } }
                }
                val preSorted = groups.entries.sortedWith { a, b ->
                    for (index in a.key.indices) {
                        val compare = a.key[index].compareTo(b.key[index])
                        if (compare != 0) return@sortedWith -compare
                    }
                    0
                }
                preSorted.flatMap { pre ->
                    val toCompare = pre.value
                    if (toCompare.size == 1) return@flatMap toCompare
                    val allRelevantEvents = db.matchresults.find(
                        LeagueEvent::leaguename eq league.leaguename, Document(
                            $$"$expr", Document(
                                $$"$setIsSubset", listOf(
                                    $$"$indices", toCompare
                                )
                            )
                        )
                    ).toList()
                    val data = UserTableData.createFromEvents(toCompare, allRelevantEvents)
                    data.entries.toMutableList().insertionSortSuspending { a, b ->
                        for (directCompareOption in directOptions.options) {
                            val res = directCompareOption.getDataValue(a.value)
                                .compareTo(directCompareOption.getDataValue(b.value))
                            if (res != 0) return@insertionSortSuspending -res
                        }
                        for (option in colsAfterDirect) {
                            val res = with(option) { getValue(a.key).compareTo(getValue(b.key)) }
                            if (res != 0) return@insertionSortSuspending -res
                        }
                        0
                    }.map { it.key }
                }
            }
            sortedIndices.map { formula()[idxToFormulaLoc()[it]!!] }
        }
    }
}

class TableSortDataStorage(val sorter: TableSorter) {
    val league = sorter.docEntry.league
    val formula = OneTimeCache { Google.get(league.sid, sorter.formulaRange, true) }
    val idxToFormulaLoc =
        OneTimeCache { formula().withIndex().associate { sorter.indexer(it.value[0].toString()) to it.index } }

    val docData = OneTimeCache {
        Google.get(league.sid, sorter.formulaRange, false).withIndex().associate { it.index to it.value }
    }
    val matchResultData = OneTimeCache {
        UserTableData.createFromEvents(
            league.table.indices.toList(), db.matchresults.find(LeagueEvent::leaguename eq league.leaguename).toList()
        )
    }/* maybe for next ASL coach season
    val matchResultDataAllLevels = OneTimeCache {
        LeagueEvent::leaguename regex "^${
            docEntry.league.leaguename.dropLast(1)
        }"
    }
    */
}

sealed interface TableSortOption {
    suspend fun TableSortDataStorage.getValue(idx: Int): Int

    companion object {
        fun fromCols(
            cols: List<Int>, options: List<DataTableSortOption> = listOf(
                TableCompareOption.POINTS, TableCompareOption.DIFF, TableCompareOption.KILLS
            )
        ) = cols.map { if (it == -1) DirectCompareSortOption(options) else ColSortOption(it) }
    }
}

interface DataTableSortOption : TableSortOption {
    fun getDataValue(data: UserTableData): Int
    override suspend fun TableSortDataStorage.getValue(idx: Int): Int {
        return matchResultData()[idx]?.let { getDataValue(it) } ?: 0
    }
}

operator fun DataTableSortOption.minus(other: DataTableSortOption) = createOperatorOption(other, Int::minus)
operator fun DataTableSortOption.div(other: DataTableSortOption) =
    createOperatorOption(other) { a, b ->
        if (b == 0) Int.MAX_VALUE else (a.toDouble() / b.toDouble()).times(1000).toInt()
    }

inline fun DataTableSortOption.createOperatorOption(other: DataTableSortOption, crossinline func: (Int, Int) -> Int) =
    object : DataTableSortOption {
        override fun getDataValue(data: UserTableData): Int =
            func(this@createOperatorOption.getDataValue(data), other.getDataValue(data))
    }

enum class TableCompareOption : DataTableSortOption {
    POINTS {
        override fun getDataValue(data: UserTableData): Int = data.points
    },
    DIFF {
        override fun getDataValue(data: UserTableData): Int = data.diff
    },
    KILLS {
        override fun getDataValue(data: UserTableData): Int = data.kills
    },
    DEATHS {
        override fun getDataValue(data: UserTableData): Int = data.deaths
    },
    WINS {
        override fun getDataValue(data: UserTableData): Int = data.wins
    },
    LOSSES {
        override fun getDataValue(data: UserTableData): Int = data.losses
    }
}

data class ColSortOption(val col: Int) : TableSortOption {
    override suspend fun TableSortDataStorage.getValue(idx: Int): Int {
        return docData()[idx]?.get(col).toString().toIntOrNull() ?: 0
    }
}

data class DirectCompareSortOption(
    val options: List<DataTableSortOption> = listOf(
        TableCompareOption.POINTS, TableCompareOption.DIFF, TableCompareOption.KILLS
    )
) : TableSortOption {
    override suspend fun TableSortDataStorage.getValue(idx: Int): Int {
        return 0
    }
}

data class DocRange(val sheet: String, val xStart: String, val yStart: Int, val xEnd: String, val yEnd: Int) {
    override fun toString() = "$sheet!$xStart$yStart:$xEnd$yEnd"
    val firstHalf: String get() = "$sheet!$xStart$yStart"
    val withoutSheet: String get() = "$xStart$yStart:$xEnd$yEnd"

    companion object {
        private val numbers = Regex("[0-9]")
        private val chars = Regex("[A-Z]")
        operator fun get(string: String): DocRange {
            val split = string.split('!')
            val range = (split.getOrNull(1) ?: split[0]).split(':')
            return DocRange(
                split[0],
                range[0].replace(numbers, ""),
                range[0].replace(chars, "").toInt(),
                range[1].replace(numbers, ""),
                range[1].replace(chars, "").toInt()
            )
        }
    }
}

suspend fun <T> MutableList<T>.insertionSortSuspending(
    comparator: suspend (T, T) -> Int
): MutableList<T> {
    for (i in 1 until size) {
        val key = this[i]
        var j = i - 1
        while (j >= 0 && comparator(this[j], key) > 0) {
            this[j + 1] = this[j]
            j--
        }
        this[j + 1] = key
    }
    return this
}