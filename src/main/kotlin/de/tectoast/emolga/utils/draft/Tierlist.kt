package de.tectoast.emolga.utils.draft

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.database.exposed.*
import de.tectoast.emolga.features.league.draft.generic.K18n_TierNotFound
import de.tectoast.emolga.ktor.TransactionPokemonData
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.TierData
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.json.CalcResult
import de.tectoast.emolga.utils.json.mdb
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.litote.kmongo.eq

@Suppress("unused")
@Serializable
data class Tierlist(
    val guildid: Long, val identifier: String = "", val priceManager: TierlistPriceConfig = TierlistPriceConfig.Empty
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

    inline fun <reified T : TierlistPriceConfig> has() = priceManager is T

    context(league: League)
    inline fun <T> withLeague(block: context(League) Tierlist.(TierlistPriceConfig) -> T) = block(priceManager)
    inline fun <T> withTL(block: Tierlist.(TierlistPriceConfig) -> T) = block(priceManager)

    inline fun <reified T, R> withPriceManager(block: Tierlist.(T) -> R): R? {
        return if (priceManager is T) {
            block(priceManager)
        } else {
            null
        }
    }

    inline fun <R> withTierBasedPriceManager(block: Tierlist.(TierBasedPriceConfig) -> R) =
        withPriceManager<TierBasedPriceConfig, R>(block)

    inline fun <T> withTierBasedPriceManager(
        league: League, block: context(League) Tierlist.(TierBasedPriceConfig) -> T
    ): T? = with(league) {
        return if (priceManager is TierBasedPriceConfig) {
            block(priceManager)
        } else {
            null
        }
    }

    inline fun <R> withPointBasedPriceManager(block: Tierlist.(PointBasedPriceConfig) -> R) =
        withPriceManager<PointBasedPriceConfig, R>(block)

    fun setup() {
        tierlists.getOrPut(guildid) { mutableMapOf() }[identifier] = this
    }

    suspend fun addPokemon(mon: String, tier: String, identifier: String = "") {
        dbTransaction {
            insert {
                it[GUILD] = guildid
                it[POKEMON] = mon
                it[TIER] = tier
                it[IDENTIFIER] = identifier
            }
            val official = NameConventionsDB.getDiscordTranslation(mon, guildid, english = true)!!.showdownId
            val teamGraphicsMeta = dependency<TeamGraphicsMetaDB>()
            val cropAuxiliary = dependency<CropAuxiliaryDB>()
            suspendTransaction {
                cropAuxiliary.insertIgnore(
                    teamGraphicsMeta.select(teamGraphicsMeta.GUILD, stringParam(official))
                        .where { (teamGraphicsMeta.GUILD eq guildid) })
            }
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

    suspend fun getTierOf(mon: String) = dbTransaction {
        selectAll().where { basePredicate and (POKEMON eq mon) }.map { it[TIER] }.firstOrNull()
    }

    suspend fun getTierOfCommand(pokemon: DraftName, requestedTier: String?): CalcResult<TierData> {
        val real = getTierOf(pokemon.tlName) ?: return CalcResult.Error(K18n_DraftUtils.PokemonNotInTierlist)
        return if (requestedTier != null && has<TierBasedPriceConfig>()) {
            val specified = order.firstOrNull {
                requestedTier.equals(
                    it, ignoreCase = true
                )
            } ?: return CalcResult.Error(K18n_TierNotFound(requestedTier))
            CalcResult.Success(
                TierData(
                    specified = specified, official = real, isTierSpecified = true
                )
            )
        } else {
            CalcResult.Success(TierData(specified = real, official = real, isTierSpecified = false))
        }
    }


    suspend fun retrieveTierlistMap(map: Map<String, Int>) = dbTransaction {
        map.entries.flatMap { (tier, amount) ->
            selectAll().where { basePredicate and (TIER eq tier) }.orderBy(Random()).limit(amount)
                .map { DraftPokemon(it[POKEMON], tier) }.toList()
        }
    }

    suspend fun getWithTierAndType(tier: String, type: String) = dbTransaction {
        selectAll().where { basePredicate and (TIER eq tier) and (TYPE eq type) }.map { it[POKEMON] }.toList()
    }

    suspend fun retrieveAll() = dbTransaction {
        selectAll().where { basePredicate }.map { DraftPokemon(it[POKEMON], it[TIER]) }.toList()
    }

    suspend fun retrieve(mons: Iterable<String>) = dbTransaction {
        selectAll().where { basePredicate and (POKEMON inList mons) }.map { it[POKEMON] to it[TIER] }.toMap()
    }

    suspend fun addOrUpdateTier(mon: String, tier: String, identifier: String = "") {
        val existing = getTierOf(mon)
        if (existing != null) {
            if (existing != tier) {
                dbTransaction {
                    if (tier in order) update({ basePredicate and (POKEMON eq mon) }) {
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

        suspend fun getAllPokemonWithTera(guild: Long, teraIdentifier: String) = dbTransaction {
            val normalTl = selectAll().where { GUILD eq guild and (IDENTIFIER eq "") }.toMap { it[POKEMON] to it[TIER] }
            val teraTl = selectAll().where { GUILD eq guild and (IDENTIFIER eq teraIdentifier) }
                .toMap { it[POKEMON] to it[TIER] }
            normalTl.entries.map { (mon, tier) ->
                val tera = teraTl[mon]
                TransactionPokemonData(mon, tier, tera)
            }
        }

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
            return tierlists[guild]?.get(identifier ?: "") ?: if (setupCalled) tierlists[guild]?.get("")
                ?.copy(identifier = identifier ?: "")
            else runBlocking { mdb.tierlist.findOne(Tierlist::guildid eq guild) }?.apply { setup() }
        }

        fun getAnyTierlist(guild: Long) = tierlists[guild]?.values?.firstOrNull()
    }
}

val Tierlist?.isEnglish get() = this?.language == Language.ENGLISH

@Suppress("unused")
enum class TierlistMode(val withPoints: Boolean, val withTiers: Boolean) {
    POINTS(true, false), TIERS(false, true), TIERS_WITH_FREE(true, true);

    fun isPoints() = this == POINTS
    fun isTiers() = this == TIERS
    fun isTiersWithFree() = this == TIERS_WITH_FREE

}

