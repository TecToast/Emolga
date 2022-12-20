package de.tectoast.emolga.utils.records

import de.tectoast.emolga.commands.DocRange

class SorterData(
    val formulaRange: List<DocRange>,
    val directCompare: Boolean = false,
    val indexer: ((String) -> Int)? = null,
    val newMethod: Boolean = false,
    val cols: List<Int>
) {
    constructor(
        formulaRange: DocRange,
        directCompare: Boolean = false,
        indexer: ((String) -> Int)? = null,
        newMethod: Boolean = false,
        cols: List<Int>
    ) : this(
        listOf(formulaRange),
        directCompare, indexer, newMethod, cols
    )
}

