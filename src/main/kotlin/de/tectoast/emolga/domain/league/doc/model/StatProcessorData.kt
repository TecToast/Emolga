package de.tectoast.emolga.domain.league.doc.model

import de.tectoast.emolga.utils.dsl.AstEnvironment
import de.tectoast.emolga.utils.dsl.ValidVariableProvider
import kotlin.reflect.KClass
import kotlin.reflect.cast


class StatProcessorData : AstEnvironment {
    private val memIdx: Int
    private val weekIndex: Int
    val battleIndex: Int
    val indexInBattle: Int
    val matchNum: Int
    private val monIndex: Int
    val monIterationIndex: Int

    private fun matchNum() = matchNum.also { retainData.game = true }
    private fun monIndex() = monIndex.also { retainData.pokemon = true }
    private fun monIterationIndex() = monIterationIndex.also { retainData.pokemon = true }

    val retainData = StatProcessorRetainData()

    constructor(
        memIdx: Int,
        weekIndex: Int,
        battleindex: Int,
        indexInBattle: Int,
        matchNum: Int,
        monindex: Int,
        monIterationIndex: Int
    ) {
        this.memIdx = memIdx
        this.weekIndex = weekIndex
        this.battleIndex = battleindex
        this.indexInBattle = indexInBattle
        this.matchNum = matchNum
        this.monIndex = monindex
        this.monIterationIndex = monIterationIndex
    }

    override fun <T : Any> resolve(variable: String, clazz: KClass<T>): T {
        val result = when (variable) {
            IDX -> memIdx
            WEEK_INDEX -> weekIndex
            BATTLE_INDEX -> battleIndex
            INDEX_IN_BATTLE -> indexInBattle
            MATCH_NUM -> matchNum()
            MON_INDEX -> monIndex()
            MON_ITERATION_INDEX -> monIterationIndex()
            else -> throw IllegalArgumentException("Unknown variable $variable")
        }
        if (!clazz.isInstance(result)) throw IllegalArgumentException("Variable $variable is not of type $clazz")
        return clazz.cast(result)
    }

    companion object : ValidVariableProvider {
        override val validVariables =
            setOf(IDX, WEEK_INDEX, BATTLE_INDEX, INDEX_IN_BATTLE, MATCH_NUM, MON_INDEX, MON_ITERATION_INDEX)

        const val IDX = "IDX"
        const val WEEK_INDEX = "WEEK_INDEX"
        const val BATTLE_INDEX = "BATTLE_INDEX"
        const val INDEX_IN_BATTLE = "INDEX_IN_BATTLE"
        const val MATCH_NUM = "MATCH_NUM"
        const val MON_INDEX = "MON_INDEX"
        const val MON_ITERATION_INDEX = "MON_ITERATION_INDEX"
    }
}