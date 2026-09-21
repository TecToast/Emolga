package de.tectoast.emolga.domain.league.draft.model.core

import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.utils.dsl.AstEnvironment
import de.tectoast.emolga.utils.dsl.ValidVariableProvider
import kotlin.reflect.KClass
import kotlin.reflect.cast

abstract class DraftData(
    private val userIndex: Int,
    private val pickIndex: Int,
    private val tlName: String,
    val showdownId: ShowdownID,
    val tier: String,
    private val roundIndex: Int,
    private val indexInRound: Int,
    private val tierInsertIndex: Int
) : AstEnvironment {
    override fun <T : Any> resolve(variable: String, clazz: KClass<T>): T {
        val result = when (variable) {
            IDX -> userIndex
            PICK_INDEX -> pickIndex
            POKEMON -> tlName
            SHOWDOWN_ID -> showdownId
            TIER -> tier
            ROUND_INDEX -> roundIndex
            INDEX_IN_ROUND -> indexInRound
            TIER_INSERT_INDEX -> tierInsertIndex
            else -> resolveSpecific(variable)
        }
        if (clazz != String::class)
            require(clazz.isInstance(result)) { "Resolved value $result is not of the expected type ${clazz.simpleName}" }
        @Suppress("UNCHECKED_CAST")
        return if (clazz == String::class) (result.toString() as T) else clazz.cast(result)
    }

    abstract fun resolveSpecific(variable: String): Any

    companion object {
        const val IDX = "IDX"
        const val PICK_INDEX = "PICK_INDEX"
        const val POKEMON = "POKEMON"
        const val SHOWDOWN_ID = "SHOWDOWN_ID"
        const val TIER = "TIER"
        const val ROUND_INDEX = "ROUND_INDEX"
        const val INDEX_IN_ROUND = "INDEX_IN_ROUND"
        const val TIER_INSERT_INDEX = "TIER_INSERT_INDEX"
    }

    class Pick(
        userIndex: Int,
        pickIndex: Int,
        tlName: String,
        showdownId: ShowdownID,
        tier: String,
        roundIndex: Int,
        indexInRound: Int,
        tierInsertIndex: Int,
        val free: Boolean,
        val updrafted: Boolean,
        val tera: Boolean,
        val points: Int?
    ) : DraftData(userIndex, pickIndex, tlName, showdownId, tier, roundIndex, indexInRound, tierInsertIndex) {
        override fun resolveSpecific(variable: String): Any {
            return when (variable) {
                "FREE" -> free
                "UPDRAFTED" -> updrafted
                "TERA" -> tera
                "POINTS" -> points ?: 0
                else -> throw IllegalArgumentException("Unknown variable: $variable")
            }
        }

        companion object : ValidVariableProvider {
            override val validVariables = setOf(
                FREE, UPDRAFTED, TERA, POINTS,
                IDX, PICK_INDEX, POKEMON, SHOWDOWN_ID, TIER, ROUND_INDEX
            )
            const val FREE = "FREE"
            const val UPDRAFTED = "UPDRAFTED"
            const val TERA = "TERA"
            const val POINTS = "POINTS"

        }
    }

    class Switch(
        userIndex: Int,
        pickIndex: Int,
        tlName: String,
        showdownId: ShowdownID,
        tier: String,
        roundIndex: Int,
        indexInRound: Int,
        tierInsertIndex: Int,
        val oldTlName: String,
        val oldShowdownId: ShowdownID
    ) : DraftData(userIndex, pickIndex, tlName, showdownId, tier, roundIndex, indexInRound, tierInsertIndex) {
        override fun resolveSpecific(variable: String): Any {
            return when (variable) {
                OLD_TL_NAME -> oldTlName
                OLD_SHOWDOWN_ID -> oldShowdownId
                else -> throw IllegalArgumentException("Unknown variable: $variable")
            }
        }

        companion object : ValidVariableProvider {
            override val validVariables = setOf(
                OLD_TL_NAME, OLD_SHOWDOWN_ID,
                IDX, PICK_INDEX, POKEMON, SHOWDOWN_ID, TIER, ROUND_INDEX
            )

            const val OLD_TL_NAME = "OLD_TL_NAME"
            const val OLD_SHOWDOWN_ID = "OLD_SHOWDOWN_ID"
        }
    }

    class Ban(
        userIndex: Int,
        pickIndex: Int,
        tlName: String,
        showdownId: ShowdownID,
        tier: String,
        roundIndex: Int,
        indexInRound: Int,
        tierInsertIndex: Int
    ) : DraftData(userIndex, pickIndex, tlName, showdownId, tier, roundIndex, indexInRound, tierInsertIndex) {
        override fun resolveSpecific(variable: String): Any {
            throw IllegalArgumentException("No specific variables for BanData (trying $variable)")
        }

        companion object : ValidVariableProvider {
            override val validVariables = setOf(
                IDX, PICK_INDEX, POKEMON, SHOWDOWN_ID, TIER, ROUND_INDEX
            )
        }
    }

}