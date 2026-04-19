package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.features.league.draft.K18n_QueuePicks
import de.tectoast.emolga.features.league.draft.generic.K18n_TierNotFound
import de.tectoast.emolga.league.K18n_League
import de.tectoast.emolga.league.TierData
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.K18n_Tierlist
import de.tectoast.emolga.utils.json.ErrorOrNull
import de.tectoast.generic.K18n_Or
import de.tectoast.k18n.generated.K18nMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.reflect.KClass


class HandlerRegistry<C : Any, H : BaseHandler<C>>(handlers: List<H>) {
    private val handlerMap = handlers.associateBy { it.targetClass }

    fun getHandler(config: C): H {
        val klass = config::class
        return handlerMap[klass] ?: error("No handler found for class $klass among handlers ${handlerMap.keys}")
    }
}

interface BaseHandler<C : Any> : KoinComponent {
    val targetClass: KClass<C>
}

// TODO: maybe use another class in some places (eg getCurrentAvailableTiers, announceData) where only picks are relevant
data class ValidationRelevantData(val picks: List<DraftPokemon>, val idx: Int, val teamSize: Int)

@Serializable
sealed interface GeneralCheck {

    @Serializable
    @SerialName("OnlyOneMega")
    data object OnlyOneMega : GeneralCheck

    @Serializable
    @SerialName("SpeciesClause")
    data object SpeciesClause : GeneralCheck
}

interface GeneralCheckOperations<C : GeneralCheck> {
    context(data: ValidationRelevantData)
    suspend fun check(config: C, action: DraftAction): ErrorOrNull
}

interface GeneralCheckHandler<C : GeneralCheck> : BaseHandler<C>, GeneralCheckOperations<C>

@Single
class GeneralCheckDispatcher(
    handlers: List<GeneralCheckHandler<GeneralCheck>>
) : GeneralCheckOperations<GeneralCheck> {
    val registry = HandlerRegistry(handlers)

    context(data: ValidationRelevantData)
    override suspend fun check(
        config: GeneralCheck,
        action: DraftAction
    ): ErrorOrNull {
        return registry.getHandler(config).check(config, action)
    }
}
@Single
class OnlyOneMegaGeneralCheckHandler : GeneralCheckHandler<GeneralCheck.OnlyOneMega> {
    override val targetClass = GeneralCheck.OnlyOneMega::class

    context(data: ValidationRelevantData)
    override suspend fun check(config: GeneralCheck.OnlyOneMega, action: DraftAction): ErrorOrNull {
        val isMega = action.official.isMega
        if (isMega && data.picks.any { it.name.isMega }) {
            return K18n_Tierlist.OnlyOneMega
        }
        return null
    }
}
@Single
class SpeciesClauseGeneralCheckHandler(val pokedexRepository: PokedexRepository) :
    GeneralCheckHandler<GeneralCheck.SpeciesClause> {
    override val targetClass = GeneralCheck.SpeciesClause::class

    context(data: ValidationRelevantData)
    override suspend fun check(config: GeneralCheck.SpeciesClause, action: DraftAction): ErrorOrNull {
        fetchDexNumbers(data.picks.map { it.name }.toSet() + action.official)
        val existingDexNumbers = data.picks.mapTo(mutableSetOf()) {
            officialToDexNumberCache[it.name]!!
        }
        val actionDexNumber = officialToDexNumberCache[action.official]!!
        if (actionDexNumber in existingDexNumbers) {
            return K18n_Tierlist.SpeciesClause
        }
        return null
    }

    private suspend fun fetchDexNumbers(officials: Set<String>) {
        val toFetch = officialToDexNumberCache.keys - officials
        if (toFetch.isEmpty()) return
        officialToDexNumberCache.putAll(pokedexRepository.getPokedexNumbers(toFetch))
    }


    private val officialToDexNumberCache = SizeLimitedMap<String, Int>(1000)
}

interface TierlistPriceManagerOperations<C : TierlistPriceConfig> {
    fun publicTierToDBTier(config: C, tier: String): String
    fun compareTiers(config: C, tierA: String, tierB: String): Int?

    context(data: ValidationRelevantData)
    suspend fun handleDraftActionWithGeneralChecks(
        config: C, action: DraftAction, context: DraftActionContext? = null
    ): ErrorOrNull

    context(data: ValidationRelevantData)
    suspend fun buildAnnounceData(config: C, picks: List<DraftPokemon>): K18nMessage?
    fun getTiers(config: C): List<String>

    context(data: ValidationRelevantData)
    suspend fun checkLegalityOfQueue(config: C, idx: Int, currentState: List<QueuedAction>): ErrorOrNull
}
@Single
class TierlistPriceConfigDispatcher(handlers: List<TierlistPriceManagerHandler<TierlistPriceConfig>>) :
    TierlistPriceManagerOperations<TierlistPriceConfig> {
    private val registry = HandlerRegistry(handlers)

    override fun publicTierToDBTier(
        config: TierlistPriceConfig,
        tier: String
    ) = registry.getHandler(config).publicTierToDBTier(config, tier)

    override fun compareTiers(
        config: TierlistPriceConfig,
        tierA: String,
        tierB: String
    ) = registry.getHandler(config).compareTiers(config, tierA, tierB)

    context(data: ValidationRelevantData)
    override suspend fun handleDraftActionWithGeneralChecks(
        config: TierlistPriceConfig,
        action: DraftAction,
        context: DraftActionContext?
    ) = registry.getHandler(config).handleDraftActionWithGeneralChecks(config, action, context)

    context(data: ValidationRelevantData)
    override suspend fun buildAnnounceData(
        config: TierlistPriceConfig,
        picks: List<DraftPokemon>
    ) = registry.getHandler(config).buildAnnounceData(config, picks)

    override fun getTiers(config: TierlistPriceConfig) = registry.getHandler(config).getTiers(config)

    context(data: ValidationRelevantData)
    override suspend fun checkLegalityOfQueue(
        config: TierlistPriceConfig,
        idx: Int,
        currentState: List<QueuedAction>
    ) = registry.getHandler(config).checkLegalityOfQueue(config, idx, currentState)
}

abstract class TierlistPriceManagerHandler<C : TierlistPriceConfig> : BaseHandler<C>,
    TierlistPriceManagerOperations<C> {

    val generalCheckDispatcher: GeneralCheckDispatcher by inject()

    fun getTierOrderingComparatorWithoutName(config: C): Comparator<DraftPokemon> {
        val tierOrder = getTiers(config)
        return compareBy { tierOrder.indexOf(it.tier) }
    }

    context(data: ValidationRelevantData)
    abstract fun handleDraftAction(config: C, action: DraftAction, context: DraftActionContext? = null): ErrorOrNull

    override fun publicTierToDBTier(config: C, tier: String) = tier
    override fun compareTiers(config: C, tierA: String, tierB: String): Int? {
        val tiers = getTiers(config)
        val indexA = tiers.indexOf(tierA)
        val indexB = tiers.indexOf(tierB)
        if (indexA == -1 || indexB == -1) return null
        return indexA - indexB
    }

    context(data: ValidationRelevantData)
    override suspend fun handleDraftActionWithGeneralChecks(
        config: C,
        action: DraftAction,
        context: DraftActionContext?
    ): ErrorOrNull {
        for (check in config.generalChecks) {
            generalCheckDispatcher.check(check, action)?.let { return it }
        }
        return handleDraftAction(config, action, context)
    }

    fun Map<String, Int>.deductPicks(list: List<DraftPokemon>): Map<String, Int> {
        val map = toMutableMap()
        for (pick in list) {
            pick.takeUnless { it.free || it.quit }?.let { map.add(it.tier, -1) }
        }
        return map
    }
}

interface TierBasedPriceManagerOperations<C : TierBasedPriceConfig> : TierlistPriceManagerOperations<C> {

    fun getSingleMap(config: C): Map<String, Int>

    context(data: ValidationRelevantData)
    fun getCurrentAvailableTiers(config: C): List<String>

    fun getTierInsertIndex(config: C, picks: List<DraftPokemon>): Int

    fun getPicksInDocOrder(config: C, picks: List<DraftPokemon>): List<DraftPokemon>

    fun getPicksWithInsertOrder(config: C, picks: List<DraftPokemon>): Map<Int, DraftPokemon>
}
@Single
class TierValidationHelper(val updraftHandler: UpdraftConfigDispatcher) {
    fun <C : TierBasedPriceConfig> check(config: C, action: DraftAction, priceConfigHandler: TierBasedPriceManagerHandler<C>): ErrorOrNull {
        return updraftHandler.handleUpdraft(config.updraftConfig, config, action, priceConfigHandler)
    }
}

abstract class TierBasedPriceManagerHandler<C : TierBasedPriceConfig>(val tierValidationHelper: TierValidationHelper) :
    TierlistPriceManagerHandler<C>(),
    TierBasedPriceManagerOperations<C> {

    context(data: ValidationRelevantData)
    abstract fun handleDraftActionAfterGeneralTierCheck(
        config: C,
        action: DraftAction,
        context: DraftActionContext?
    ): ErrorOrNull

    context(data: ValidationRelevantData)
    override fun handleDraftAction(
        config: C,
        action: DraftAction,
        context: DraftActionContext?
    ): ErrorOrNull {
        tierValidationHelper.check(config, action, this)?.let { return it }
        return handleDraftActionAfterGeneralTierCheck(config, action, context)
    }

    override fun getPicksInDocOrder(
        config: C,
        picks: List<DraftPokemon>
    ): List<DraftPokemon> {
        val indexMap = getPicksWithInsertOrder(config, picks)
        return picks.indices.map { indexMap[it]!! }
    }

    override fun getPicksWithInsertOrder(
        config: C,
        picks: List<DraftPokemon>
    ): Map<Int, DraftPokemon> {
        val indexMap = mutableMapOf<Int, DraftPokemon>()
        for (i in picks.indices) {
            val subList = picks.subList(0, i + 1)
            val index = getTierInsertIndex(config, subList)
            indexMap[index] = picks[i]
        }
        return indexMap
    }
}

interface PointBasedPriceConfigOperations<C : PointBasedPriceConfig> : TierlistPriceManagerOperations<C> {
    fun getPointsForTier(config: C, tier: String): Int?

    fun getPointsForMon(config: C, pokemon: DraftPokemon): Int {
        return getPointsForTier(config, pokemon.tier)
            ?: error("Tier ${pokemon.tier} not found for pokemon ${pokemon.name}")
    }

    context(data: ValidationRelevantData)
    fun getPointsOfUser(config: C): Int {
        return config.globalPoints - data.picks.sumOf {
            if (it.quit || it.noCost) 0
            else getPointsForMon(config, it)
        }
    }
}

interface OnlyPointBasedPriceConfigOperations<C : OnlyPointBasedPriceConfig> : PointBasedPriceConfigOperations<C> {
    fun getMinimumPrice(config: C): Int
}

abstract class PointBasedPriceConfigHandler<C : PointBasedPriceConfig> : TierlistPriceManagerHandler<C>(),
    PointBasedPriceConfigOperations<C>

abstract class OnlyPointBasedPriceConfigHandler<C : OnlyPointBasedPriceConfig> : PointBasedPriceConfigHandler<C>(),
    OnlyPointBasedPriceConfigOperations<C> {
    context(data: ValidationRelevantData)
    override fun handleDraftAction(config: C, action: DraftAction, context: DraftActionContext?): ErrorOrNull {
        val currentPoints = getPointsOfUser(config)
        val cost = getPointsForTier(config, action.officialTier) ?: return K18n_TierNotFound(action.officialTier)
        val pointsBack = action.switch?.let { switched -> getPointsForTier(config, switched.tier)!! } ?: 0
        val newPoints = currentPoints - cost + pointsBack
        if (newPoints < 0) {
            return K18n_Tierlist.NotEnoughPoints("$currentPoints - $cost${if (pointsBack == 0) "" else " + $pointsBack"} = $newPoints < 0")
        }
        val cpicks = data.picks
        if (action.switch == null) {
            val minimumRequired = minimumNeededPointsForTeamCompletion(config, cpicks.count { !it.noCost } + 1)
            if (newPoints < minimumRequired) {
                return K18n_Tierlist.MinimumNeededError(minimumRequired, newPoints)
            }
        }
        if (action.tera) {
            config.teraMaxPoints?.let {
                if (cpicks.sumOf { dp ->
                        if (dp.tera) getPointsForMon(
                            config,
                            dp
                        ) else 0
                    } + cost - (if (action.switch?.tera == true) pointsBack else 0) > it) {
                    return K18n_Tierlist.TeraMaxPoints(it)
                }
            }
        }
        return null
    }

    context(data: ValidationRelevantData)
    private fun minimumNeededPointsForTeamCompletion(config: C, picksSizeAfter: Int): Int =
        (data.teamSize - picksSizeAfter) * getMinimumPrice(config)

    context(data: ValidationRelevantData)
    override suspend fun buildAnnounceData(
        config: C,
        picks: List<DraftPokemon>
    ): K18nMessage? {
        val points = getPointsOfUser(config)
        return K18n_League.PossiblePoints(points)
    }

    context(data: ValidationRelevantData)
    override suspend fun checkLegalityOfQueue(
        config: C,
        idx: Int,
        currentState: List<QueuedAction>
    ): ErrorOrNull {
        var gPoints = 0
        var yPoints = 0
        for (data in currentState) {
            // TODO
            /*
            gPoints += getPointsForTier(league.tierlist.getTierOf(data.g.tlName)!!)!!
            yPoints += data.y?.let { getPointsForTier(league.tierlist.getTierOf(it.tlName)!!)!! } ?: 0
            */
        }
        if (getPointsOfUser(config) - gPoints + yPoints < minimumNeededPointsForTeamCompletion(
                config,
                (data.picks.size) + currentState.size
            )
        ) return K18n_QueuePicks.LegalTeamCompletion
        config.teraMaxPoints?.let {
            val cpicks = data.picks
            val teraPoints = cpicks.sumOf { dp -> if (dp.tera) getPointsForMon(config, dp) else 0 }
            if (teraPoints > it) {
                return K18n_Tierlist.TeraMaxPoints(it)
            }
        }
        return null
    }
}

abstract class CombinedOptionsPriceConfigHandler<C : CombinedOptionsPriceConfig>(tierValidationHelper: TierValidationHelper) :
    TierBasedPriceManagerHandler<C>(tierValidationHelper) {

    context(data: ValidationRelevantData)
    override fun handleDraftActionAfterGeneralTierCheck(
        config: C,
        action: DraftAction,
        context: DraftActionContext?
    ): ErrorOrNull {
        val specifiedTier = action.specifiedTier
        val allTiers = getAllPossibleTiers(config)
        if (allTiers.all { map -> map.getOrDefault(specifiedTier, 0) <= 0 }) {
            if (config.combinedOptions.all { p -> p[specifiedTier] == 0 }) {
                return K18n_Tierlist.MustUpdraft(specifiedTier)
            }
            if (action.switch != null) return null
            return K18n_Tierlist.CantPickTier(specifiedTier)
        }
        return null
    }

    context(data: ValidationRelevantData)
    override fun getCurrentAvailableTiers(config: C): List<String> {
        val cpicks = data.picks
        return config.combinedOptions.flatMap { opt ->
            val deducted = opt.deductPicks(cpicks)
            if (deducted.any { it.value < 0 }) emptyList() else deducted.entries.filter { it.value > 0 }.map { it.key }
        }
    }

    context(data: ValidationRelevantData)
    fun getAllPossibleTiers(config: C): List<Map<String, Int>> =
        config.combinedOptions.map { it.deductPicks(data.picks) }

    override fun getTiers(config: C): List<String> {
        return config.tierOrder
    }


    context(data: ValidationRelevantData)
    override suspend fun checkLegalityOfQueue(
        config: C,
        idx: Int,
        currentState: List<QueuedAction>
    ): ErrorOrNull {
        val res = getAllPossibleTiers(config)
        val finalMaps = res.map { map ->
            val tempMap = map.toMutableMap()
            currentState.forEach {
                // TODO: Save tier in QueuedAction to avoid this lookup
                /*tempMap.add(league.tierlist.getTierOf(it.g.tlName)!!, -1)
                it.y?.let { y -> tempMap.add(league.tierlist.getTierOf(y.tlName)!!, 1) }*/
            }
            tempMap
        }
        val isIllegal = finalMaps.all { map -> map.any { it.value < 0 } }
        if (isIllegal) {
            return K18n_QueuePicks.LegalTooManyInTier
        }
        return null
    }
}

interface CombinedOptionsPriceConfig : TierBasedPriceConfig {
    val combinedOptions: List<Map<String, Int>>
    val tierOrder: List<String>
}

interface FreePickPriceConfig : TierlistPriceConfig


interface PointBasedPriceConfig : TierlistPriceConfig {
    val globalPoints: Int
    val teraMaxPoints: Int?
}

interface OnlyPointBasedPriceConfig : PointBasedPriceConfig


interface TierBasedPriceConfig : TierlistPriceConfig {
    val updraftConfig: UpdraftConfig
}

@Single(binds = [TierlistPriceManagerHandler::class])
class SimpleTierBasedHandler(tierValidationHelper: TierValidationHelper) :
    TierBasedPriceManagerHandler<TierlistPriceConfig.SimpleTierBased>(tierValidationHelper) {

    override val targetClass = TierlistPriceConfig.SimpleTierBased::class

    context(data: ValidationRelevantData)
    override fun handleDraftActionAfterGeneralTierCheck(
        config: TierlistPriceConfig.SimpleTierBased,
        action: DraftAction,
        context: DraftActionContext?
    ): ErrorOrNull {
        val options = getPossibleTiers(config, data.picks)
        if (options[action.specifiedTier]!! <= 0) {
            if (config.tiers[action.specifiedTier] == 0) {
                return K18n_Tierlist.MustUpdraft(action.specifiedTier)
            }
            if (action.switch != null) return null
            return K18n_Tierlist.CantPickTier(action.specifiedTier)
        }
        return null
    }

    private fun getPossibleTiers(config: TierlistPriceConfig.SimpleTierBased, picks: List<DraftPokemon>) =
        config.tiers.deductPicks(picks)

    context(data: ValidationRelevantData)
    override suspend fun buildAnnounceData(
        config: TierlistPriceConfig.SimpleTierBased,
        picks: List<DraftPokemon>
    ): K18nMessage? {
        return getPossibleTiers(config, picks).entries.filterNot { it.value == 0 }
            .joinToString { tierAmountToString(it.key, it.value) }.let {
                if (it.isEmpty()) null else K18n_League.PossibleTiers(it)
            }
    }


    override fun getTiers(config: TierlistPriceConfig.SimpleTierBased) = config.tiers.keys.toList()

    context(data: ValidationRelevantData)
    override suspend fun checkLegalityOfQueue(
        config: TierlistPriceConfig.SimpleTierBased,
        idx: Int,
        currentState: List<QueuedAction>
    ): ErrorOrNull {
        val map = getPossibleTiers(config, data.picks).toMutableMap()
//        val tl = league.tierlist
        currentState.forEach {
            // TODO same as above
            /*map.add(tl.getTierOf(it.g.tlName)!!, -1)
            it.y?.let { y -> map.add(tl.getTierOf(y.tlName)!!, 1) }*/
        }
        val result = map.entries.firstOrNull { it.value < 0 }
        val isIllegal = result != null
        if (isIllegal) {
            return K18n_QueuePicks.LegalTooManyInSingleTier(result.key)
        }
        return null
    }

    override fun getSingleMap(config: TierlistPriceConfig.SimpleTierBased) = config.tiers

    context(data: ValidationRelevantData)
    override fun getCurrentAvailableTiers(config: TierlistPriceConfig.SimpleTierBased) =
        getPossibleTiers(config, data.picks).filter { it.value > 0 }.keys.toList()

    override fun getTierInsertIndex(
        config: TierlistPriceConfig.SimpleTierBased,
        picks: List<DraftPokemon>
    ): Int {
        val tier = picks.lastOrNull()?.tier ?: error("No picks to determine tier for index")
        var index = 0
        for (entry in config.tiers.entries) {
            if (entry.key == tier) {
                return picks.count { !it.free && !it.quit && it.tier == tier } + index - 1
            }
            index += entry.value
        }
        error("Tier $tier not found by")
    }

    override fun getPicksInDocOrder(
        config: TierlistPriceConfig.SimpleTierBased,
        picks: List<DraftPokemon>
    ): List<DraftPokemon> {
        return picks.sortedWith(getTierOrderingComparatorWithoutName(config))
    }
}
@Single(binds = [TierlistPriceManagerHandler::class])
class SimplePointBasedHandler : OnlyPointBasedPriceConfigHandler<TierlistPriceConfig.SimplePointBased>() {
    override val targetClass = TierlistPriceConfig.SimplePointBased::class

    override fun getTiers(config: TierlistPriceConfig.SimplePointBased) = config.prices.keys.toList()

    override fun getPointsForTier(config: TierlistPriceConfig.SimplePointBased, tier: String) = config.prices[tier]

    override fun getMinimumPrice(config: TierlistPriceConfig.SimplePointBased) = config.prices.values.min()
}
@Single(binds = [TierlistPriceManagerHandler::class])
class RangePointBasedHandler : OnlyPointBasedPriceConfigHandler<TierlistPriceConfig.RangePointBased>() {
    override val targetClass = TierlistPriceConfig.RangePointBased::class

    override fun getTiers(config: TierlistPriceConfig.RangePointBased): List<String> {
        return (config.maxTier..config.minTier).map { it.toString() }
    }

    override fun getPointsForTier(config: TierlistPriceConfig.RangePointBased, tier: String): Int? {
        val tierInt = tier.toIntOrNull() ?: return null
        if (tierInt !in config.minTier..config.maxTier) return null
        return tierInt
    }

    override fun getMinimumPrice(config: TierlistPriceConfig.RangePointBased) = config.minTier
}
@Single(binds = [TierlistPriceManagerHandler::class])
class OptionsTierBasedHandler(tierValidationHelper: TierValidationHelper) :
    CombinedOptionsPriceConfigHandler<TierlistPriceConfig.OptionsTierBased>(tierValidationHelper) {
    override val targetClass = TierlistPriceConfig.OptionsTierBased::class

    context(data: ValidationRelevantData)
    override suspend fun buildAnnounceData(
        config: TierlistPriceConfig.OptionsTierBased,
        picks: List<DraftPokemon>
    ): K18nMessage? {
        val res = getAllPossibleTiers(config)
        val allTiers = res.flatMapTo(mutableSetOf()) { it.keys }.sortedBy {
            config.tierOrder.indexOf(it)
        }
        val minValues = allTiers.associateWith { tier ->
            res.minOf { it[tier] ?: 0 }
        }
        if (res.all { map -> map.all { it.value <= 0 } }) {
            return null
        }
        return b {
            val str = buildString {
                val baseData = allTiers.filter { minValues[it]!! > 0 }.joinToString(", ") {
                    tierAmountToString(it, minValues[it]!!)
                }
                val additionalData = res.mapNotNull { map ->
                    val reduced = map.subtractFrom(minValues)
                    if (reduced.all { it.value <= 0 }) null else allTiers.filter { reduced[it]!! > 0 }
                        .joinToString(", ") {
                            tierAmountToString(it, reduced[it]!!)
                        }
                }.joinToString(" **--- ${K18n_Or()} ---** ")
                append(baseData)
                if (additionalData.isNotEmpty()) {
                    append(" + [")
                    append(additionalData)
                    append("]")
                }
            }
            K18n_League.PossibleTiers(str)()
        }
    }

    override fun getSingleMap(config: TierlistPriceConfig.OptionsTierBased): Map<String, Int> {
        return config.genericTiers.toMutableMap().apply {
            config.options.forEach { optionList ->
                this.addFromMutable(optionList.firstOrNull().orEmpty())
            }
        }
    }

    override fun getTierInsertIndex(
        config: TierlistPriceConfig.OptionsTierBased,
        picks: List<DraftPokemon>
    ): Int {
        error("Can't get tier insert index for option based tierlist")
    }
}
@Single(binds = [TierlistPriceManagerHandler::class])
class ChoiceTierBasedHandler(tierValidationHelper: TierValidationHelper) :
    CombinedOptionsPriceConfigHandler<TierlistPriceConfig.ChoiceTierBased>(tierValidationHelper) {
    override val targetClass = TierlistPriceConfig.ChoiceTierBased::class

    context(data: ValidationRelevantData)
    override suspend fun buildAnnounceData(
        config: TierlistPriceConfig.ChoiceTierBased,
        picks: List<DraftPokemon>
    ): K18nMessage? {
        val cpicks = data.picks
        val fromGeneric = config.genericTiers.deductPicks(cpicks)
        val singularOptions = getSingularChoiceList(config)
        for (tier in fromGeneric.flatMap { genericEntry ->
            if (genericEntry.value >= 0) emptyList()
            else List(-genericEntry.value) { genericEntry.key }
        }) {
            val result = singularOptions.removeOne { tier in it.tiers }
            if (result == null) error("Couldn't find tier $tier in choices")
        }
        val availableOptions = singularOptions.groupingBy { it.tiers }.eachCount().toMutableMap()
        val str = buildString {
            val baseData = fromGeneric.entries.filter { it.value > 0 }.flatMap {
                buildList {
                    add(tierAmountToString(it.key, it.value))
                    availableOptions.entries.firstOrNull { en -> it.key in en.key }?.let { entry ->
                        availableOptions.remove(entry.key)
                        add(tierAmountToString(entry.key.joinToString("/"), entry.value))
                    }
                }
            }.joinToString(", ")
            val additionalData = availableOptions.entries.joinToString { (tiers, amount) ->
                tierAmountToString(tiers.joinToString("/"), amount)
            }
            append(baseData)
            if (additionalData.isNotEmpty()) {
                append(", ")
                append(additionalData)
            }
        }
        return if (str.isEmpty()) null else K18n_League.PossibleTiers(str)
    }

    override fun getSingleMap(config: TierlistPriceConfig.ChoiceTierBased): Map<String, Int> {
        return config.genericTiers.toMutableMap().apply {
            for (choice in config.choices) {
                val firstOption = choice.tiers.first()
                this.add(firstOption, choice.amount)
            }
        }
    }

    override fun getTierInsertIndex(
        config: TierlistPriceConfig.ChoiceTierBased,
        picks: List<DraftPokemon>
    ): Int {
        val tierToInsert = picks.lastOrNull()?.tier ?: error("No tier found in picks to use")
        var index = 0
        var tierBefore: String? = null
        for (entry in config.genericTiers.entries) {
            val sumOfChoiceSlots =
                config.choices.filter { entry.key in it.tiers && (tierBefore == null || tierBefore in it.tiers) }
                    .sumOf { it.amount }
            index += sumOfChoiceSlots
            if (entry.key == tierToInsert) {
                val picksAmountInTier = picks.count { !it.free && !it.quit && it.tier == tierToInsert }
                val monsInChoiceSlots = picksAmountInTier - entry.value
                return if (monsInChoiceSlots > 0) {
                    if (sumOfChoiceSlots > 0) index - monsInChoiceSlots else index + monsInChoiceSlots
                } else picksAmountInTier + index - 1
            }
            index += entry.value
            tierBefore = entry.key
        }
        error("Tier $tierToInsert not found")
    }

    fun generateAllOptions(
        config: TierlistPriceConfig.ChoiceTierBased
    ): List<Map<String, Int>> = buildList {
        fun recursiveBuild(remainingChoices: List<SingularChoiceTierOption>, map: Map<String, Int>) {
            if (remainingChoices.isEmpty()) {
                add(map)
                return
            }
            val first = remainingChoices.first()
            val rest = remainingChoices.drop(1)
            for (tier in first.tiers) {
                val copy = map.toMutableMap()
                copy.add(tier, 1)
                recursiveBuild(rest, copy)
            }
        }
        recursiveBuild(ChoiceTierOption.createSingularList(config.choices), config.genericTiers)
    }.distinct()

    fun getSingularChoiceList(config: TierlistPriceConfig.ChoiceTierBased) =
        ChoiceTierOption.createSingularList(config.choices)
}
@Single(binds = [TierlistPriceManagerHandler::class])
class TierAndPointHandler(tierValidationHelper: TierValidationHelper) :
    TierBasedPriceManagerHandler<TierlistPriceConfig.TierAndPoint>(tierValidationHelper),
    PointBasedPriceConfigOperations<TierlistPriceConfig.TierAndPoint> {
    override val targetClass = TierlistPriceConfig.TierAndPoint::class

    context(data: ValidationRelevantData)
    override fun handleDraftActionAfterGeneralTierCheck(
        config: TierlistPriceConfig.TierAndPoint,
        action: DraftAction,
        context: DraftActionContext?
    ): ErrorOrNull {
        val officialCost =
            getPointsForTier(config, action.officialTier) ?: return K18n_TierNotFound(action.officialTier)
        val cost = if (action.tier.isTierSpecified) {
            val possibleTiers = config.tiers[action.specifiedTier]!!.tiers
            if (officialCost > possibleTiers.max()) return K18n_Tierlist.CantUpdraft(
                officialCost.toString().pointsToActualTier(config), action.specifiedTier
            )
            possibleTiers.min().coerceAtLeast(officialCost)
        } else officialCost
        val specifiedTier = cost.toString().pointsToActualTier(config)
        val options = getPossibleTiers(config)
        if (options[specifiedTier]!! <= 0) {
            if (config.tiers[specifiedTier]?.amount == 0) {
                return K18n_Tierlist.MustUpdraft(specifiedTier)
            }
            if (action.switch != null) return null
            return K18n_Tierlist.CantPickTier(specifiedTier)
        }
        val currentPoints = getPointsOfUser(config)
        if (officialCost - (officialCost % 2) - (cost - (cost % 2)) > 2) {
            return K18n_Tierlist.GapError(action.official, 1)
        }
        val pointsBack = action.switch?.let { switched -> getPointsForTier(config, switched.tier)!! } ?: 0
        val newPoints = currentPoints - cost + pointsBack
        if (newPoints < 0) {
            return K18n_Tierlist.NotEnoughPoints("$currentPoints - $cost${if (pointsBack == 0) "" else " + $pointsBack"} = $newPoints < 0")
        }
        if (action.switch == null && !canFinishTeamAfterPick(config, data.picks, options, cost, newPoints)) {
            return K18n_Tierlist.TeamNotCompletable
        }
        context?.saveTier = cost.toString()
        return null
    }

    context(data: ValidationRelevantData)
    override suspend fun buildAnnounceData(
        config: TierlistPriceConfig.TierAndPoint,
        picks: List<DraftPokemon>
    ): K18nMessage? {
        return getPossibleTiers(config).entries.filterNot { it.value == 0 }
            .joinToString { tierAmountToString(it.key, it.value) }.let {
                if (it.isEmpty()) null else K18n_League.PossibleTiers(it)
            }?.let { tierMsg ->
                b {
                    "${tierMsg()}, ${K18n_League.PossiblePoints(getPointsOfUser(config))()}"
                }
            }
    }

    override fun getTiers(config: TierlistPriceConfig.TierAndPoint) = config.tiers.keys.toList()

    context(data: ValidationRelevantData)
    override suspend fun checkLegalityOfQueue(
        config: TierlistPriceConfig.TierAndPoint,
        idx: Int,
        currentState: List<QueuedAction>
    ): ErrorOrNull {
        val map = getPossibleTiers(config).toMutableMap()
        // TODO
        currentState.forEach {
            /*map.add(tl.getTierOf(it.g.tlName)!!.pointsToActualTier(), -1)
            it.y?.let { y -> map.add(tl.getTierOf(y.tlName)!!.pointsToActualTier(), 1) }*/
        }
        val result = map.entries.firstOrNull { it.value < 0 }
        val isIllegalFromTiers = result != null
        if (isIllegalFromTiers) {
            return K18n_QueuePicks.LegalTooManyInSingleTier(result.key)
        }
        val cpoints =/*
            getPointsOfUser(config) - currentState.sumOf { getPointsForTier(tl.getTierOf(it.g.tlName)!!)!! } + currentState.sumOf {
                it.y?.let { y -> getPointsForTier(tl.getTierOf(y.tlName)!!)!! } ?: 0
            }*/ 0
        if (cpoints < 0 || !canFinishWithOptions(config, cpoints, map)) {
            return K18n_QueuePicks.LegalTeamCompletion
        }
        return null
    }

    override fun getSingleMap(config: TierlistPriceConfig.TierAndPoint) = config.tiers.mapValues { it.value.amount }

    context(data: ValidationRelevantData)
    override fun getCurrentAvailableTiers(config: TierlistPriceConfig.TierAndPoint) =
        getPossibleTiers(config).filter { it.value > 0 }.keys.toList()

    override fun getTierInsertIndex(
        config: TierlistPriceConfig.TierAndPoint,
        picks: List<DraftPokemon>
    ): Int {
        val tier = picks.lastOrNull()?.tier ?: error("No picks to determine tier for index")
        var index = 0
        for (entry in config.tiers.entries) {
            if (entry.key == tier) {
                return picks.count { !it.free && !it.quit && it.tier == tier } + index - 1
            }
            index += entry.value.amount
        }
        error("Tier $tier not found by")
    }

    override fun getPointsForTier(
        config: TierlistPriceConfig.TierAndPoint,
        tier: String
    ) = tier.toIntOrNull()

    override fun publicTierToDBTier(
        config: TierlistPriceConfig.TierAndPoint,
        tier: String
    ): String {
        return config.tiers[tier]?.tiers?.min()?.toString() ?: error("Tier $tier not found")
    }

    override fun getPicksInDocOrder(
        config: TierlistPriceConfig.TierAndPoint,
        picks: List<DraftPokemon>
    ): List<DraftPokemon> {
        return picks.sortedWith(getTierOrderingComparatorWithoutName(config))
    }

    context(data: ValidationRelevantData)
    private fun getPossibleTiers(config: TierlistPriceConfig.TierAndPoint) = deductPicks(config, data.picks)

    fun canFinishTeamAfterPick(
        config: TierlistPriceConfig.TierAndPoint,
        picks: List<DraftPokemon>,
        options: Map<String, Int>,
        aboutToPickCost: Int,
        newPoints: Int
    ): Boolean {
        val aboutToPickTier = aboutToPickCost.toString().pointsToActualTier(config)
        val tempOptions = options.toMutableMap()
        tempOptions.add(aboutToPickTier, -1)
        return canFinishWithOptions(config, newPoints, tempOptions)
    }

    private fun canFinishWithOptions(
        config: TierlistPriceConfig.TierAndPoint,
        newPoints: Int,
        tempOptions: Map<String, Int>
    ): Boolean {
        var points = newPoints
        var pointsFromPreviousTier: Int? = null
        for (tier in getTiers(config).reversed()) {
            val pointsFromThisTier = config.tiers[tier]!!.tiers.min()
            tempOptions[tier]?.takeIf { it > 0 }?.let { amount ->
                val actualPoints = pointsFromPreviousTier ?: pointsFromThisTier
                for (i in 0 until amount) {
                    points -= actualPoints
                    if (points < 0) return false
                }
            }
            pointsFromPreviousTier = pointsFromThisTier
        }
        return points >= 0
    }

    fun deductPicks(config: TierlistPriceConfig.TierAndPoint, list: List<DraftPokemon>): Map<String, Int> {
        val map = getSingleMap(config).toMutableMap()
        for (pick in list) {
            pick.takeUnless { it.free || it.quit }?.let { map.add(it.tier.pointsToActualTier(config), -1) }
        }
        return map
    }

    private fun String.pointsToActualTier(config: TierlistPriceConfig.TierAndPoint): String {
        return config.pointsToTier[this] ?: error("No tier found for points $this")
    }
}
@Single(binds = [TierlistPriceManagerHandler::class])
class EmptyPriceManagerHandler : TierlistPriceManagerHandler<TierlistPriceConfig.Empty>() {
    override val targetClass = TierlistPriceConfig.Empty::class

    context(data: ValidationRelevantData)
    override fun handleDraftAction(
        config: TierlistPriceConfig.Empty,
        action: DraftAction,
        context: DraftActionContext?
    ) = null

    context(data: ValidationRelevantData)
    override suspend fun buildAnnounceData(
        config: TierlistPriceConfig.Empty,
        picks: List<DraftPokemon>
    ) = null

    override fun getTiers(config: TierlistPriceConfig.Empty): List<String> = emptyList()

    context(data: ValidationRelevantData)
    override suspend fun checkLegalityOfQueue(
        config: TierlistPriceConfig.Empty,
        idx: Int,
        currentState: List<QueuedAction>
    ) = null
}

@Serializable
sealed interface TierlistPriceConfig {
    @Serializable
    @SerialName("TierAndPoint")
    data class TierAndPoint(
        override val updraftConfig: UpdraftConfig = UpdraftConfig.NoCheck,
        override val generalChecks: List<GeneralCheck> = emptyList(),
        override val globalPoints: Int,
        override val teraMaxPoints: Int? = null,
        val tiers: Map<String, SingleTierAndPointData>
    ) : TierlistPriceConfig, TierBasedPriceConfig, PointBasedPriceConfig {
        @Serializable
        data class SingleTierAndPointData(
            val amount: Int, val tiers: List<Int>
        )

        val pointsToTier by lazy {
            tiers.entries.flatMap { it.value.tiers.map { num -> num.toString() to it.key } }.toMap()
        }

    }

    val generalChecks: List<GeneralCheck>

    @Serializable
    @SerialName("SimpleTierBased")
    data class SimpleTierBased(
        val tiers: Map<String, Int>,
        override val updraftConfig: UpdraftConfig = UpdraftConfig.Default,
        override val generalChecks: List<GeneralCheck> = emptyList()
    ) : TierlistPriceConfig, TierBasedPriceConfig


    @Serializable
    @SerialName("SimplePointBased")
    data class SimplePointBased(
        val prices: Map<String, Int>,
        override val globalPoints: Int,
        override val generalChecks: List<GeneralCheck> = emptyList(),
        override val teraMaxPoints: Int? = null
    ) : TierlistPriceConfig, OnlyPointBasedPriceConfig

    @Serializable
    @SerialName("RangePointBased")
    data class RangePointBased(
        val maxTier: Int,
        val minTier: Int,
        override val globalPoints: Int,
        override val generalChecks: List<GeneralCheck> = emptyList(),
        override val teraMaxPoints: Int? = null
    ) : TierlistPriceConfig, OnlyPointBasedPriceConfig

    @Serializable
    @SerialName("OptionsTierBased")
    data class OptionsTierBased(
        override val tierOrder: List<String>,
        val genericTiers: Map<String, Int>,
        val options: List<List<Map<String, Int>>>,
        override val updraftConfig: UpdraftConfig = UpdraftConfig.Default,
        override val generalChecks: List<GeneralCheck> = emptyList()
    ) : TierlistPriceConfig, CombinedOptionsPriceConfig {
        override val combinedOptions by lazy {
            buildList {
                for (set in options) {
                    for (option in set) {
                        add(genericTiers.addFrom(option))
                    }
                }
            }
        }
    }

    @Serializable
    @SerialName("ChoiceTierBased")
    data class ChoiceTierBased(
        override val tierOrder: List<String>,
        val genericTiers: Map<String, Int>,
        val choices: List<ChoiceTierOption>,
        override val updraftConfig: UpdraftConfig = UpdraftConfig.Default,
        override val generalChecks: List<GeneralCheck> = emptyList()
    ) : TierlistPriceConfig, CombinedOptionsPriceConfig {
        override val combinedOptions: List<Map<String, Int>> by lazy {
            buildList {
                fun recursiveBuild(remainingChoices: List<SingularChoiceTierOption>, map: Map<String, Int>) {
                    if (remainingChoices.isEmpty()) {
                        add(map)
                        return
                    }
                    val first = remainingChoices.first()
                    val rest = remainingChoices.drop(1)
                    for (tier in first.tiers) {
                        val copy = map.toMutableMap()
                        copy.add(tier, 1)
                        recursiveBuild(rest, copy)
                    }
                }
                recursiveBuild(ChoiceTierOption.createSingularList(choices), genericTiers)
            }.distinct()
        }
    }


    @Serializable
    @SerialName("Empty")
    data object Empty : TierlistPriceConfig {
        override val generalChecks: List<GeneralCheck> = emptyList()
    }
}


data class DraftAction(
    val tier: TierData,
    val official: String,
    val free: Boolean = false,
    val tera: Boolean = false,
    val switch: DraftPokemon? = null
) {

    constructor(official: String, officialTier: String) : this(
        tier = TierData(officialTier, officialTier, false), official = official
    )

    val officialTier: String
        get() = tier.official

    val specifiedTier: String
        get() = tier.specified
}

data class DraftActionContext(
    var saveTier: String? = null, var freePick: Boolean = false
)

@Serializable
data class ChoiceTierOption(
    val tiers: Set<String>, val amount: Int
) {
    companion object {
        fun createSingularList(list: List<ChoiceTierOption>) = list.flatMapTo(mutableListOf()) { option ->
            List(option.amount) {
                SingularChoiceTierOption(option.tiers)
            }
        }
    }
}

data class SingularChoiceTierOption(
    val tiers: Set<String>
)

interface UpdraftConfigOperations<C : UpdraftConfig> {
    fun <T : TierBasedPriceConfig> handleUpdraft(config: C, priceConfig: T, action: DraftAction, priceConfigHandler: TierBasedPriceManagerHandler<T>): ErrorOrNull
}

@Single
class UpdraftConfigDispatcher(
    handlers: List<UpdraftConfigHandler<UpdraftConfig>>
) : UpdraftConfigOperations<UpdraftConfig> {
    val registry = HandlerRegistry(handlers)

    override fun <T : TierBasedPriceConfig> handleUpdraft(
        config: UpdraftConfig,
        priceConfig: T,
        action: DraftAction,
        priceConfigHandler: TierBasedPriceManagerHandler<T>
    ) = registry.getHandler(config).handleUpdraft(config, priceConfig, action, priceConfigHandler)
}

interface UpdraftConfigHandler<C : UpdraftConfig> : BaseHandler<C>, UpdraftConfigOperations<C>

@Single
class DefaultUpdraftConfigHandler(val priceConfigHandler: TierlistPriceConfigDispatcher) :
    UpdraftConfigHandler<UpdraftConfig.Default> {
    override val targetClass = UpdraftConfig.Default::class

    override fun <T : TierBasedPriceConfig> handleUpdraft(
        config: UpdraftConfig.Default,
        priceConfig: T,
        action: DraftAction,
        priceConfigHandler: TierBasedPriceManagerHandler<T>
    ): ErrorOrNull {
        val compareResult = priceConfigHandler.compareTiers(priceConfig, action.specifiedTier, action.officialTier)
            ?: return K18n_TierNotFound(action.specifiedTier)
        if (action.switch != null) return null
        if (compareResult > 0) {
            return K18n_Tierlist.CantUpdraft(action.official, action.specifiedTier)
        }
        return null
    }
}

@Single
class OnlyWithGapUpdraftConfigHandler :
    UpdraftConfigHandler<UpdraftConfig.OnlyWithGap> {
    override val targetClass = UpdraftConfig.OnlyWithGap::class

    override fun <T : TierBasedPriceConfig> handleUpdraft(
        config: UpdraftConfig.OnlyWithGap,
        priceConfig: T,
        action: DraftAction,
        priceConfigHandler: TierBasedPriceManagerHandler<T>
    ): ErrorOrNull {
        val diff = priceConfigHandler.compareTiers(priceConfig, action.specifiedTier, action.officialTier)
            ?: return K18n_TierNotFound(
                action.specifiedTier
            )
        if (action.switch != null) return null
        if (diff > 0) {
            return K18n_Tierlist.CantUpdraft(action.official, action.specifiedTier)
        }
        if (diff < 0 && -diff > config.gap) {
            return K18n_Tierlist.GapError(action.official, config.gap)
        }
        return null
    }
}

@Single
class DisabledUpdraftConfigHandler :
    UpdraftConfigHandler<UpdraftConfig.Disabled> {
    override val targetClass = UpdraftConfig.Disabled::class

    override fun <T : TierBasedPriceConfig> handleUpdraft(
        config: UpdraftConfig.Disabled,
        priceConfig: T,
        action: DraftAction,
        priceConfigHandler: TierBasedPriceManagerHandler<T>
    ): ErrorOrNull {
        val compareResult = priceConfigHandler.compareTiers(priceConfig, action.specifiedTier, action.officialTier)
            ?: return K18n_TierNotFound(action.specifiedTier)
        if (compareResult != 0) {
            return K18n_Tierlist.UpdraftDisabled
        }
        return null
    }
}

@Single
class NoCheckUpdraftConfigHandler : UpdraftConfigHandler<UpdraftConfig.NoCheck> {
    override val targetClass = UpdraftConfig.NoCheck::class

    override fun <T : TierBasedPriceConfig> handleUpdraft(
        config: UpdraftConfig.NoCheck,
        priceConfig: T,
        action: DraftAction,
        priceConfigHandler: TierBasedPriceManagerHandler<T>
    ): ErrorOrNull = null
}


@Serializable
sealed interface UpdraftConfig {

    @Serializable
    @SerialName("Default")
    data object Default : UpdraftConfig

    @Serializable
    @SerialName("OnlyWithGap")
    data class OnlyWithGap(val gap: Int) : UpdraftConfig

    @Serializable
    @SerialName("Disabled")
    data object Disabled : UpdraftConfig

    @Serializable
    @SerialName("NoCheck")
    data object NoCheck : UpdraftConfig
}
