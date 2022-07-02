package de.tectoast.emolga.utils.records

import java.util.function.Function

class SorterData(
    val formulaRange: List<String>,
    val pointRange: List<String>,
    val directCompare: Boolean,
    val indexer: Function<String, Int>?,
    vararg val cols: Int
) {
    constructor(
        formulaRange: String,
        pointRange: String,
        directCompare: Boolean,
        indexer: Function<String, Int>?,
        vararg cols: Int
    ) : this(
        listOf<String>(formulaRange), listOf<String>(pointRange),
        directCompare, indexer, *cols
    )
}

