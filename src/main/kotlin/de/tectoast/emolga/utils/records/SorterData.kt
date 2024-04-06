package de.tectoast.emolga.utils.records

import de.tectoast.emolga.utils.DirectCompareData

class SorterData(
    val formulaRange: List<String>,
    val directCompare: List<DirectCompareOption> = listOf(DirectCompareOption.DIFF, DirectCompareOption.KILLS),
    val indexer: ((String) -> Int)? = null,
    val newMethod: Boolean = false,
    val cols: List<Int>
) {
    val formulaRangeParsed = formulaRange.map { DocRange[it] }
    constructor(
        formulaRange: String,
        directCompare: List<DirectCompareOption> = listOf(DirectCompareOption.DIFF, DirectCompareOption.KILLS),
        indexer: ((String) -> Int)? = null,
        newMethod: Boolean = false,
        cols: List<Int>
    ) : this(
        listOf(formulaRange), directCompare, indexer, newMethod, cols
    )

}

data class DocRange(val sheet: String, val xStart: String, val yStart: Int, val xEnd: String, val yEnd: Int) {
    override fun toString() = "$sheet!$xStart$yStart:$xEnd$yEnd"
    val firstHalf: String get() = "$sheet!$xStart$yStart"

    companion object {
        private val numbers = Regex("[0-9]")
        private val chars = Regex("[A-Z]")
        operator fun get(string: String): DocRange {
            val split = string.split('!')
            val range = split[1].split(':')
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


enum class DirectCompareOption {
    DIFF {
        override fun getFromData(data: DirectCompareData) = data.kills - data.deaths
    },
    KILLS {
        override fun getFromData(data: DirectCompareData) = data.kills
    };

    abstract fun getFromData(data: DirectCompareData): Int
}
