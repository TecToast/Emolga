package de.tectoast.emolga.utils.records

import java.util.function.Function

class SorterData(
    val formulaRange: List<String>,
    val directCompare: Boolean,
    val indexer: Function<String, Int>?,
    vararg val cols: Int
) {
    constructor(
        formulaRange: String,
        directCompare: Boolean,
        indexer: Function<String, Int>?,
        vararg cols: Int
    ) : this(
        listOf<String>(formulaRange),
        directCompare, indexer, *cols
    )
}

