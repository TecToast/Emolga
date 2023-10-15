package de.tectoast.emolga.utils.records

import de.tectoast.emolga.commands.DocRange
import de.tectoast.emolga.utils.automation.structure.DirectCompareData

class SorterData(
    val formulaRange: List<DocRange>,
    val directCompare: List<DirectCompareOption> = listOf(DirectCompareOption.DIFF, DirectCompareOption.KILLS),
    val indexer: ((String) -> Int)? = null,
    val newMethod: Boolean = false,
    val cols: List<Int>
) {
    constructor(
        formulaRange: DocRange,
        directCompare: List<DirectCompareOption> = listOf(DirectCompareOption.DIFF, DirectCompareOption.KILLS),
        indexer: ((String) -> Int)? = null,
        newMethod: Boolean = false,
        cols: List<Int>
    ) : this(
        listOf(formulaRange),
        directCompare, indexer, newMethod, cols
    )
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
