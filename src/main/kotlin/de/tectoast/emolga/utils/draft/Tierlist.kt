package de.tectoast.emolga.utils.draft

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.features.league.draft.K18n_QueuePicks
import de.tectoast.emolga.features.league.draft.generic.K18n_TierNotFound
import de.tectoast.emolga.league.K18n_League
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.TierData
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.draft.PointBasedPriceManager.Companion.pointManager
import de.tectoast.emolga.utils.draft.TierBasedPriceManager.Companion.tierAmountToString
import de.tectoast.emolga.utils.draft.TierlistPriceManager.Companion.deductPicks
import de.tectoast.emolga.utils.json.ErrorOrNull
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.json.mdb
import de.tectoast.generic.K18n_Or
import de.tectoast.k18n.generated.K18nMessage
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.exposed.v1.core.Random
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.update
import org.litote.kmongo.eq

@Suppress("unused")
@Serializable
class Tierlist(
    val guildid: Long,
    val identifier: String = "",
    val priceManager: TierlistPriceManager = TierlistPriceManager.Empty
) {
    var language = Language.GERMAN

    // TODO: Support multiple prices
    val order get() = priceManager.getTiers()

    val basePredicate get() = GUILD eq guildid and (IDENTIFIER eq identifier)

    @Transient
    private val _autoComplete = OneTimeCache { getAllForAutoComplete() }
    suspend fun autoComplete() = _autoComplete() + addedViaCommand
    val addedViaCommand: MutableSet<String> = mutableSetOf()

    @Transient
    val tlToOfficialCache = SizeLimitedMap<String, String>(1000)

    val tierorderingComparator by lazy { compareBy<DraftPokemon>({ order.indexOf(it.tier) }, { it.name }) }
    val tierorderingComparatorWithoutName by lazy { compareBy<DraftPokemon> { order.indexOf(it.tier) } }

    inline fun <reified T : TierlistPriceManager> has() = priceManager is T

    context(league: League)
    inline fun <T> withLeague(block: context(League) Tierlist.(TierlistPriceManager) -> T) = block(priceManager)
    inline fun <T> withTL(block: Tierlist.(TierlistPriceManager) -> T) = block(priceManager)

    inline fun <reified T, R> withPriceManager(block: Tierlist.(T) -> R): R? {
        return if (priceManager is T) {
            block(priceManager)
        } else {
            null
        }
    }

    inline fun <R> withTierBasedPriceManager(block: Tierlist.(TierBasedPriceManager) -> R) =
        withPriceManager<TierBasedPriceManager, R>(block)

    inline fun <T> withTierBasedPriceManager(
        league: League,
        block: context(League) Tierlist.(TierBasedPriceManager) -> T
    ): T? = with(league) {
        return if (priceManager is TierBasedPriceManager) {
            block(priceManager)
        } else {
            null
        }
    }

    inline fun <R> withPointBasedPriceManager(block: Tierlist.(PointBasedPriceManager) -> R) =
        withPriceManager<PointBasedPriceManager, R>(block)

    fun setup() {
        tierlists.getOrPut(guildid) { mutableMapOf() }[identifier] = this
    }

    suspend fun addPokemon(mon: String, tier: String, identifier: String = "") = dbTransaction {
        insert {
            it[GUILD] = guildid
            it[POKEMON] = mon
            it[TIER] = tier
            it[IDENTIFIER] = identifier
        }
    }

    suspend fun getByTier(tier: String): List<String>? {
        return dbTransaction {
            selectAll().where { basePredicate and (TIER eq tier) }.map { it[POKEMON] }.toList().ifEmpty { null }
        }
    }

    private suspend fun getAllForAutoComplete() = dbTransaction {
        val list = selectAll().where { basePredicate }.map { it[POKEMON] }.toSet()
        (list + NameConventionsDB.getAllOtherSpecified(list, language, guildid)).toSet()
    }

    suspend fun getTierOf(mon: String) =
        dbTransaction {
            selectAll().where { basePredicate and (POKEMON eq mon) }.map { it[TIER] }.firstOrNull()
        }

    suspend fun getTierOfCommand(pokemon: DraftName, requestedTier: String?): TierData? {
        val real = getTierOf(pokemon.tlName) ?: return null
        return if (requestedTier != null && has<TierBasedPriceManager>()) {
            // TODO maybe dont return an empty string
            TierData(order.firstOrNull {
                requestedTier.equals(
                    it, ignoreCase = true
                )
            } ?: "",
                real)
        } else {
            TierData(real, real)
        }
    }


    suspend fun retrieveTierlistMap(map: Map<String, Int>) = dbTransaction {
        map.entries.flatMap { (tier, amount) ->
            selectAll().where { basePredicate and (TIER eq tier) }.orderBy(Random()).limit(amount)
                .map { DraftPokemon(it[POKEMON], tier) }.toList()
        }
    }

    suspend fun getWithTierAndType(tier: String, type: String) = dbTransaction {
        selectAll().where { basePredicate and (TIER eq tier) and (TYPE eq type) }
            .map { it[POKEMON] }.toList()
    }

    suspend fun retrieveAll() = dbTransaction {
        selectAll().where { basePredicate }.map { DraftPokemon(it[POKEMON], it[TIER]) }.toList()
    }

    suspend fun addOrUpdateTier(mon: String, tier: String, identifier: String = "") {
        val existing = getTierOf(mon)
        if (existing != null) {
            if (existing != tier) {
                dbTransaction {
                    if (tier in order)
                        update({ basePredicate and (POKEMON eq mon) }) {
                            it[this.TIER] = tier
                        }
                    else deleteWhere { basePredicate and (POKEMON eq mon) }
                }
            }
        } else {
            addPokemon(mon, tier, identifier)
        }
    }

    suspend fun deleteAllMons() = dbTransaction {
        deleteWhere { basePredicate }
    }

    suspend fun getMonCount() = dbTransaction {
        selectAll().where { basePredicate }.count().toInt()
    }

    companion object : Table("tierlists") {
        /**
         * All tierlists
         */
        val GUILD = long("guild")
        val IDENTIFIER = varchar("identifier", 30)
        val POKEMON = varchar("pokemon", 64)
        val TIER = varchar("tier", 8)
        val TYPE = varchar("type", 10).nullable()

        override val primaryKey = PrimaryKey(GUILD, IDENTIFIER, POKEMON)
        private var setupCalled = false

        val tierlists: MutableMap<Long, MutableMap<String, Tierlist>> = mutableMapOf()
        suspend fun setup() {
            tierlists.clear()
            setupCalled = true
            mdb.tierlist.find().toFlow().collect { it.setup() }
        }

        /**
         * Gets the tierlist for the given guild (or fetches it in case it's not in the cache, which is only possible in test env)
         */
        operator fun get(guild: Long, identifier: String? = null): Tierlist? {
            return tierlists[guild]?.get(identifier ?: "")
                ?: if (setupCalled) null
                else runBlocking { mdb.tierlist.findOne(Tierlist::guildid eq guild) }?.apply { setup() }
        }

        fun getAnyTierlist(guild: Long) = tierlists[guild]?.values?.firstOrNull()
    }
}

val Tierlist?.isEnglish get() = this?.language == Language.ENGLISH

@Suppress("unused")
enum class TierlistMode(val withPoints: Boolean, val withTiers: Boolean) {
    POINTS(true, false),
    TIERS(false, true),
    TIERS_WITH_FREE(true, true);

    fun isPoints() = this == POINTS
    fun isTiers() = this == TIERS
    fun isTiersWithFree() = this == TIERS_WITH_FREE

}

interface TierBasedPriceManager : TierlistPriceManager {
    val updraftHandler: UpdraftHandler

    context(league: League, tl: Tierlist)
    override fun handleDraftAction(action: DraftAction, context: DraftActionContext?): ErrorOrNull {
        updraftHandler.handleUpdraft(action)?.let { return it }
        return handleDraftActionAfterGeneralTierCheck(action)
    }

    context(league: League, tl: Tierlist)
    fun handleDraftActionAfterGeneralTierCheck(action: DraftAction): ErrorOrNull

    context(tl: Tierlist)
    fun getSingleMap(): Map<String, Int>

    context(league: League, tl: Tierlist)
    fun getCurrentAvailableTiers(): List<String>

    fun getTierInsertIndex(picks: List<DraftPokemon>): Int

    fun getPicksInDocOrder(league: League, picks: List<DraftPokemon>): List<DraftPokemon> {
        val indexMap = getPicksWithInsertOrder(league, picks)
        return picks.indices.map { indexMap[it]!! }
    }

    fun getPicksWithInsertOrder(league: League, picks: List<DraftPokemon>): Map<Int, DraftPokemon> {
        val indexMap = mutableMapOf<Int, DraftPokemon>()
        for (i in picks.indices) {
            val subList = picks.subList(0, i + 1)
            val index = getTierInsertIndex(subList)
            indexMap[index] = picks[i]
        }
        return indexMap
    }

    companion object {
        fun tierAmountToString(tier: String, amount: Int) =
            "${amount}x **".condAppend(tier.toIntOrNull() != null, "Tier ") + "${tier}**"
    }
}

interface CombinedOptionsPriceManager : TierBasedPriceManager {
    val combinedOptions: List<Map<String, Int>>
    val tierOrder: List<String>

    context(league: League, tl: Tierlist)
    override fun handleDraftActionAfterGeneralTierCheck(action: DraftAction): ErrorOrNull {
        val specifiedTier = action.specifiedTier
        val allTiers = getAllPossibleTiers()
        if (allTiers.all { map -> map.getOrDefault(specifiedTier, 0) <= 0 }) {
            if (combinedOptions.all { p -> p[specifiedTier] == 0 }) {
                return K18n_Tierlist.MustUpdraft(specifiedTier)
            }
            if (action.switch != null) return null
            return K18n_Tierlist.CantPickTier(specifiedTier)
        }
        return null
    }

    context(league: League, tl: Tierlist)
    override fun getCurrentAvailableTiers(): List<String> {
        val cpicks = league.picks()
        return combinedOptions.flatMap { opt ->
            val deducted = opt.deductPicks(cpicks)
            if (deducted.any { it.value < 0 }) emptyList() else deducted.entries.filter { it.value > 0 }
                .map { it.key }
        }
    }

    override fun getTiers() = tierOrder

    context(league: League, tl: Tierlist)
    fun getAllPossibleTiers(idx: Int = league.current): List<Map<String, Int>> =
        combinedOptions.map { it.deductPicks(league.picks(idx)) }

    context(league: League, tl: Tierlist)
    override suspend fun checkLegalityOfQueue(
        idx: Int,
        currentState: List<QueuedAction>
    ): ErrorOrNull {
        val res = getAllPossibleTiers(idx)
        val finalMaps = res.map { map ->
            val tempMap = map.toMutableMap()
            currentState.forEach {
                tempMap.add(league.tierlist.getTierOf(it.g.tlName)!!, -1)
                it.y?.let { y -> tempMap.add(league.tierlist.getTierOf(y.tlName)!!, 1) }
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

interface FreePickPriceManager : TierlistPriceManager

interface PointBasedPriceManager : TierlistPriceManager {
    val globalPoints: Int
    fun getPointsForTier(tier: String): Int?
    fun getPointsForMon(pokemon: DraftPokemon): Int {
        return getPointsForTier(pokemon.tier)
            ?: error("Tier ${pokemon.tier} not found for pokemon ${pokemon.name}")
    }

    context(tl: Tierlist)
    suspend fun getPointsForMon(tlName: String): Int {
        val tier = tl.getTierOf(tlName) ?: error("Mon $tlName not found in tierlist ${tl.guildid} ${tl.identifier}")
        val price = getPointsForTier(tier)
            ?: error("Tier $tier not found in tierlist ${tl.guildid} ${tl.identifier} for mon $tlName")
        return price
    }

    context(league: League, tl: Tierlist)
    fun getPointsOfUser(idx: Int) = pointManager()[idx]

    companion object {
        val pointsManagers = mutableMapOf<String, PointsManager>()

        context(league: League)
        fun pointManager() = pointsManagers.getOrPut(league.leaguename) {
            PointsManager()
        }
    }

    class PointsManager {
        private val points = mutableMapOf<Int, Int>()

        context(league: League, tl: Tierlist, pm: PointBasedPriceManager)
        operator fun get(idx: Int) = points.getOrPut(idx) {
            pm.globalPoints - league.picks[idx].orEmpty().filterNot { it.quit || it.noCost }.sumOf {
                pm.getPointsForMon(it)
            }
        }

        operator fun set(idx: Int, points: Int) {
            this.points[idx] = points
        }

        context(league: League, tl: Tierlist, pm: PointBasedPriceManager)
        fun add(idx: Int, points: Int) {
            this.points[idx] = this[idx] + points
        }
    }
}

@Serializable
sealed interface GeneralCheck {
    context(league: League, tl: Tierlist, pm: TierlistPriceManager)
    suspend fun check(action: DraftAction): ErrorOrNull

    @Serializable
    @SerialName("OnlyOneMega")
    data object OnlyOneMega : GeneralCheck {
        context(league: League, tl: Tierlist, pm: TierlistPriceManager)
        override suspend fun check(action: DraftAction): ErrorOrNull {
            val isMega = action.official.isMega
            if (isMega && league.picks().any { it.name.isMega }) {
                return K18n_Tierlist.OnlyOneMega
            }
            return null
        }
    }

    @Serializable
    @SerialName("SpeciesClause")
    data object SpeciesClause : GeneralCheck {
        context(league: League, tl: Tierlist, pm: TierlistPriceManager)
        override suspend fun check(action: DraftAction): ErrorOrNull {
            val existingDexNumbers = league.picks().mapTo(mutableSetOf()) {
                getDexNumber(it.name)
            }
            val actionDexNumber = getDexNumber(action.official)
            if (actionDexNumber in existingDexNumbers) {
                return K18n_Tierlist.SpeciesClause
            }
            return null
        }

        context(league: League)
        private suspend fun getDexNumber(official: String): Int = officialToDexNumberCache.getOrPut(official) {
            mdb.pokedex.get(
                NameConventionsDB.getSDTranslation(
                    official,
                    league.guild,
                    english = true
                )!!.official.toSDName()
            )!!.num
        }


        private val officialToDexNumberCache = SizeLimitedMap<String, Int>(1000)
    }
}

@Serializable
sealed interface TierlistPriceManager {
    val generalChecks: List<GeneralCheck>

    context(tl: Tierlist)
    fun compareTiers(tierA: String, tierB: String): Int? =
        getTiers().compareTiersFromOrder(tierA, tierB)

    context(league: League, tl: Tierlist)
    suspend fun handleDraftActionWithGeneralChecks(
        action: DraftAction,
        context: DraftActionContext? = null
    ): ErrorOrNull {
        checkGeneralChecks(action)?.let { return it }
        return handleDraftAction(action, context)
    }

    context(league: League, tl: Tierlist)
    suspend fun checkGeneralChecks(action: DraftAction): ErrorOrNull {
        for (check in generalChecks) {
            check.check(action)?.let { return it }
        }
        return null
    }

    context(league: League, tl: Tierlist)
    fun handleDraftAction(action: DraftAction, context: DraftActionContext? = null): ErrorOrNull

    context(league: League, tl: Tierlist)
    suspend fun buildAnnounceData(idx: Int = league.current): K18nMessage?

    fun getTiers(): List<String>

    context(league: League, tl: Tierlist)
    suspend fun checkLegalityOfQueue(idx: Int, currentState: List<QueuedAction>): ErrorOrNull

    @Serializable
    @SerialName("SimpleTierBased")
    data class SimpleTierBased(
        val tiers: Map<String, Int>,
        override val updraftHandler: UpdraftHandler = UpdraftHandler.Default,
        override val generalChecks: List<GeneralCheck> = emptyList()
    ) :
        TierlistPriceManager, TierBasedPriceManager {
        context(league: League, tl: Tierlist)
        override fun handleDraftActionAfterGeneralTierCheck(action: DraftAction): ErrorOrNull {
            val options = getPossibleTiers()
            if (options[action.specifiedTier]!! <= 0) {
                if (tiers[action.specifiedTier] == 0) {
                    return K18n_Tierlist.MustUpdraft(action.specifiedTier)
                }
                if (action.switch != null) return null
                return K18n_Tierlist.CantPickTier(action.specifiedTier)
            }
            return null
        }

        context(league: League, tl: Tierlist)
        override suspend fun buildAnnounceData(idx: Int): K18nMessage? {
            return getPossibleTiers(idx).entries.filterNot { it.value == 0 }
                .joinToString { tierAmountToString(it.key, it.value) }.let {
                    if (it.isEmpty()) null else K18n_League.PossibleTiers(it)
                }
        }

        override fun getTiers() = tiers.keys.toList()

        context(tl: Tierlist)
        override fun getSingleMap() = tiers

        context(league: League, tl: Tierlist)
        private fun getPossibleTiers(idx: Int = league.current) = tiers.deductPicks(league.picks(idx))

        context(league: League, tl: Tierlist)
        override fun getCurrentAvailableTiers(): List<String> {
            return getPossibleTiers().filter { it.value > 0 }.keys.toList()
        }

        override fun getTierInsertIndex(picks: List<DraftPokemon>): Int {
            val tier = picks.lastOrNull()?.tier ?: error("No picks to determine tier for index")
            var index = 0
            for (entry in tiers.entries) {
                if (entry.key == tier) {
                    return picks.count { !it.free && !it.quit && it.tier == tier } + index - 1
                }
                index += entry.value
            }
            error("Tier $tier not found by")
        }

        override fun getPicksInDocOrder(league: League, picks: List<DraftPokemon>): List<DraftPokemon> {
            return picks.sortedWith(league.tierlist.tierorderingComparatorWithoutName)
        }

        context(league: League, tl: Tierlist)
        override suspend fun checkLegalityOfQueue(
            idx: Int,
            currentState: List<QueuedAction>
        ): ErrorOrNull {
            val map = getPossibleTiers(idx).toMutableMap()
            val tl = league.tierlist
            currentState.forEach {
                map.add(tl.getTierOf(it.g.tlName)!!, -1)
                it.y?.let { y -> map.add(tl.getTierOf(y.tlName)!!, 1) }
            }
            val result = map.entries.firstOrNull { it.value < 0 }
            val isIllegal = result != null
            if (isIllegal) {
                return K18n_QueuePicks.LegalTooManyInSingleTier(result.key)
            }
            return null
        }
    }

    interface OnlyPointBasedPriceManager : TierlistPriceManager, PointBasedPriceManager {
        fun getMinimumPrice(): Int

        context(league: League, tl: Tierlist)
        override fun handleDraftAction(action: DraftAction, context: DraftActionContext?): ErrorOrNull {
            val pointManager = pointManager()
            val currentPoints = pointManager[league.current]
            val cost = getPointsForTier(action.specifiedTier)
                ?: return K18n_TierNotFound(action.specifiedTier)
            val pointsBack = action.switch?.let { switched -> getPointsForTier(switched.tier)!! } ?: 0
            val newPoints = currentPoints - cost + pointsBack
            if (newPoints < 0) {
                return K18n_Tierlist.NotEnoughPoints("$currentPoints - $cost${if (pointsBack == 0) "" else " + $pointsBack"} = $newPoints < 0")
            }
            val cpicks = league.picks()
            if (action.switch != null) {
                val minimumRequired =
                    minimumNeededPointsForTeamCompletion(cpicks.count { !it.noCost } + 1)
                if (newPoints < minimumRequired) {
                    return K18n_Tierlist.MinimumNeededError(minimumRequired, newPoints)
                }
            }
            pointManager[league.current] = newPoints
            return null
        }

        context(league: League)
        private fun minimumNeededPointsForTeamCompletion(picksSizeAfter: Int): Int =
            (league.teamsize - picksSizeAfter) * getMinimumPrice()


        context(league: League, tl: Tierlist)
        override suspend fun buildAnnounceData(idx: Int): K18nMessage {
            return K18n_League.PossiblePoints(pointManager()[idx])
        }

        context(league: League, tl: Tierlist)
        override suspend fun checkLegalityOfQueue(
            idx: Int,
            currentState: List<QueuedAction>
        ): ErrorOrNull {
            var gPoints = 0
            var yPoints = 0
            for (data in currentState) {
                gPoints += getPointsForTier(league.tierlist.getTierOf(data.g.tlName)!!)!!
                yPoints += data.y?.let { getPointsForTier(league.tierlist.getTierOf(it.tlName)!!)!! }
                    ?: 0
            }
            if (pointManager()[idx] - gPoints + yPoints < minimumNeededPointsForTeamCompletion(
                    (league.picks[idx]?.size ?: 0) + currentState.size
                )
            ) return K18n_QueuePicks.LegalTeamCompletion
            return null
        }
    }

    @Serializable
    @SerialName("SimplePointBased")
    data class SimplePointBased(
        val prices: Map<String, Int>, override val globalPoints: Int,
        override val generalChecks: List<GeneralCheck> = emptyList()
    ) : TierlistPriceManager,
        OnlyPointBasedPriceManager {

        override fun getPointsForTier(tier: String) = prices[tier]

        override fun getMinimumPrice(): Int {
            return prices.values.min()
        }

        override fun getTiers(): List<String> {
            return prices.keys.toList()
        }
    }

    @Serializable
    @SerialName("RangePointBased")
    data class RangePointBased(
        val maxTier: Int,
        val minTier: Int,
        override val globalPoints: Int,
        override val generalChecks: List<GeneralCheck> = emptyList()
    ) : TierlistPriceManager, OnlyPointBasedPriceManager {
        override fun getTiers(): List<String> {
            return (maxTier..minTier).map { it.toString() }
        }

        override fun getPointsForTier(tier: String): Int? {
            val tierInt = tier.toIntOrNull() ?: return null
            if (tierInt !in minTier..maxTier) return null
            return tierInt
        }

        override fun getMinimumPrice() = minTier
    }

    @Serializable
    @SerialName("OptionsTierBased")
    data class OptionsTierBased(
        override val tierOrder: List<String>,
        val genericTiers: Map<String, Int>,
        val options: List<List<Map<String, Int>>>,
        override val updraftHandler: UpdraftHandler = UpdraftHandler.Default,
        override val generalChecks: List<GeneralCheck> = emptyList()
    ) : TierlistPriceManager, CombinedOptionsPriceManager {

        override val combinedOptions by lazy {
            buildList {
                for (set in options) {
                    for (option in set) {
                        add(genericTiers.addFrom(option))
                    }
                }
            }
        }

        context(tl: Tierlist)
        override fun getSingleMap(): Map<String, Int> {
            return genericTiers.toMutableMap().apply {
                options.forEach { optionList ->
                    this.addFromMutable(optionList.firstOrNull().orEmpty())
                }
            }
        }

        override fun getTierInsertIndex(picks: List<DraftPokemon>): Int {
            error("Can't get tier insert index for option based tierlist")
        }

        context(league: League, tl: Tierlist)
        override suspend fun buildAnnounceData(idx: Int): K18nMessage? {
            val res = getAllPossibleTiers(idx)
            val allTiers = res.flatMapTo(mutableSetOf()) { it.keys }.sortedBy {
                tierOrder.indexOf(it)
            }
            val minValues = allTiers.associateWith { tier ->
                res.minOf { it[tier] ?: 0 }
            }
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
                }.joinToString(" **--- ${K18n_Or.translateToGuildLanguage(league.guild)} ---** ")
                append(baseData)
                if (additionalData.isNotEmpty()) {
                    append(" + [")
                    append(additionalData)
                    append("]")
                }
            }
            return if (str.isEmpty()) null else K18n_League.PossibleTiers(str)
        }

        private fun MutableMap<String, Int>.addFromMutable(other: Map<String, Int>) {
            for ((key, value) in other) {
                this.add(key, value)
            }
        }

        private fun Map<String, Int>.addFrom(other: Map<String, Int>): Map<String, Int> {
            val result = this.toMutableMap()
            result.addFromMutable(other)
            return result
        }

        private fun Map<String, Int>.subtractFrom(other: Map<String, Int>): Map<String, Int> {
            val result = this.toMutableMap()
            for ((key, value) in other) {
                result.add(key, -value)
            }
            return result
        }
    }

    @Serializable
    @SerialName("ChoiceTierBased")
    data class ChoiceTierBased(
        override val tierOrder: List<String>, val genericTiers: Map<String, Int>, val choices: List<ChoiceTierOption>,
        override val updraftHandler: UpdraftHandler = UpdraftHandler.Default,
        override val generalChecks: List<GeneralCheck> = emptyList()
    ) : TierlistPriceManager, CombinedOptionsPriceManager {

        override val combinedOptions by lazy {
            generateAllOptions(choices, genericTiers)
        }

        context(tl: Tierlist)
        override fun getSingleMap(): Map<String, Int> {
            return genericTiers.toMutableMap().apply {
                for (choice in choices) {
                    val firstOption = choice.tiers.first()
                    this.add(firstOption, choice.amount)
                }
            }
        }


        override fun getTierInsertIndex(picks: List<DraftPokemon>): Int {
            val tierToInsert = picks.lastOrNull()?.tier
                ?: error("No tier found in picks to use")
            var index = 0
            var tierBefore: String? = null
            for (entry in genericTiers.entries) {
                val sumOfChoiceSlots =
                    choices.filter { entry.key in it.tiers && (tierBefore == null || tierBefore in it.tiers) }
                        .sumOf { it.amount }
                index += sumOfChoiceSlots
                if (entry.key == tierToInsert) {
                    val picksAmountInTier = picks.count { !it.free && !it.quit && it.tier == tierToInsert }
                    val monsInChoiceSlots = picksAmountInTier - entry.value
                    return if (monsInChoiceSlots > 0) {
                        if (sumOfChoiceSlots > 0) index - monsInChoiceSlots else index + monsInChoiceSlots
                    } else
                        picksAmountInTier + index - 1
                }
                index += entry.value
                tierBefore = entry.key
            }
            error("Tier $tierToInsert not found")
        }

        context(league: League, tl: Tierlist)
        override suspend fun buildAnnounceData(idx: Int): K18nMessage? {
            val cpicks = league.picks(idx)
            val fromGeneric = genericTiers.deductPicks(cpicks)
            val singularOptions = getSingularChoiceList()
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

        fun getSingularChoiceList() = ChoiceTierOption.createSingularList(choices)

        companion object {
            fun generateAllOptions(
                choices: List<ChoiceTierOption>,
                genericTiers: Map<String, Int>
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
                recursiveBuild(ChoiceTierOption.createSingularList(choices), genericTiers)
            }.distinct()
        }
    }

    @Serializable
    @SerialName("Empty")
    data object Empty : TierlistPriceManager {
        override val generalChecks: List<GeneralCheck> = emptyList()

        context(league: League, tl: Tierlist)
        override fun handleDraftAction(action: DraftAction, context: DraftActionContext?): ErrorOrNull = null

        context(league: League, tl: Tierlist)
        override suspend fun buildAnnounceData(idx: Int) = null

        override fun getTiers(): List<String> = emptyList()

        context(league: League, tl: Tierlist)
        override suspend fun checkLegalityOfQueue(
            idx: Int,
            currentState: List<QueuedAction>
        ) = null
    }

    companion object {
        fun Map<String, Int>.deductPicks(list: List<DraftPokemon>): Map<String, Int> {
            val map = toMutableMap()
            for (pick in list) {
                pick.takeUnless { it.free || it.quit }?.let { map.add(it.tier, -1) }
            }
            return map
        }

        fun List<String>.compareTiersFromOrder(tierA: String, tierB: String): Int? {
            val indexA = indexOf(tierA)
            val indexB = indexOf(tierB)
            if (indexA == -1 || indexB == -1) return null
            return indexA - indexB
        }
    }

}

data class DraftAction(
    val officialTier: String,
    val official: String,
    val specifiedTier: String = officialTier,
    val free: Boolean = false,
    val tera: Boolean = false,
    val switch: DraftPokemon? = null
)

data class DraftActionContext(
    var isValidFreePick: Boolean = false
)

@Serializable
data class ChoiceTierOption(
    val tiers: Set<String>,
    val amount: Int
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

@Serializable
sealed interface UpdraftHandler {
    context(league: League, tl: Tierlist, priceManager: TierlistPriceManager)
    fun handleUpdraft(action: DraftAction): ErrorOrNull

    @Serializable
    @SerialName("Default")
    data object Default : UpdraftHandler {
        context(league: League, tl: Tierlist, priceManager: TierlistPriceManager)
        override fun handleUpdraft(action: DraftAction): ErrorOrNull {
            val compareResult = priceManager.compareTiers(action.specifiedTier, action.officialTier)
                ?: return K18n_TierNotFound(action.specifiedTier)
            if (compareResult < 0 && action.switch == null) {
                return K18n_Tierlist.CantUpdraft(action.official, action.specifiedTier)
            }
            return null
        }
    }

    @Serializable
    @SerialName("OnlyWithGap")
    data class OnlyWithGap(val gap: Int) : UpdraftHandler {
        context(league: League, tl: Tierlist, priceManager: TierlistPriceManager)
        override fun handleUpdraft(action: DraftAction): ErrorOrNull {
            val diff = priceManager.compareTiers(action.specifiedTier, action.officialTier)
                ?: return K18n_TierNotFound(action.specifiedTier)
            if (diff < 0 && action.switch == null) {
                if (-diff > gap) {
                    return K18n_Tierlist.GapError(action.official, gap)
                }
            }
            return null
        }
    }

    @Serializable
    @SerialName("Disabled")
    data object Disabled : UpdraftHandler {
        context(league: League, tl: Tierlist, priceManager: TierlistPriceManager)
        override fun handleUpdraft(action: DraftAction): ErrorOrNull {
            val compareResult = priceManager.compareTiers(action.specifiedTier, action.officialTier)
                ?: return K18n_TierNotFound(action.specifiedTier)
            if (compareResult != 0) {
                return K18n_Tierlist.UpdraftDisabled
            }
            return null
        }
    }
}
