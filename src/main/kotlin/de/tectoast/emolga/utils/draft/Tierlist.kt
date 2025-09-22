package de.tectoast.emolga.utils.draft

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.league.TierData
import de.tectoast.emolga.utils.Language
import de.tectoast.emolga.utils.OneTimeCache
import de.tectoast.emolga.utils.SizeLimitedMap
import de.tectoast.emolga.utils.json.db
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.exposed.v1.core.Random
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.litote.kmongo.eq

@Suppress("unused")
@Serializable
class Tierlist(val guildid: Long, val identifier: String? = null) {

    /**
     * The price for each tier
     */
    val prices: MutableMap<String, Int> = mutableMapOf()
    val freepicks: MutableMap<String, Int> = mutableMapOf()
    val additionalPrices: List<MutableMap<String, Int>> = mutableListOf()
    val allPrices get() = buildList { add(prices); addAll(additionalPrices) }

    var mode: TierlistMode = TierlistMode.POINTS
    var points = 0
    var language = Language.GERMAN
    val variableMegaPrice = false
    val maxMonsToPay = Int.MAX_VALUE


    val order get() = prices.keys.toList()

    val freePicksAmount get() = freepicks["#AMOUNT#"] ?: 0
    val basePredicate get() = GUILD eq guildid and (IDENTIFIER eq identifier)

    @Transient
    private val _autoComplete = OneTimeCache { getAllForAutoComplete() }
    suspend fun autoComplete() = _autoComplete() + addedViaCommand
    val addedViaCommand: MutableSet<String> = mutableSetOf()

    @Transient
    val tlToOfficialCache = SizeLimitedMap<String, String>(1000)

    val tierorderingComparator by lazy { compareBy<DraftPokemon>({ order.indexOf(it.tier) }, { it.name }) }

    fun setup() {
        tierlists.getOrPut(guildid) { mutableMapOf() }[identifier ?: ""] = this
    }


    fun getPointsNeeded(tier: String): Int {
        if (variableMegaPrice && "#" in tier) return tier.substringAfter("#").toInt()
        return when (mode) {
            TierlistMode.POINTS -> prices[tier] ?: error("Tier for $tier not found")
            TierlistMode.TIERS_WITH_FREE -> freepicks[tier] ?: error("Tier for $tier not found")
            else -> error("Unknown mode for points $mode")
        }
    }

    suspend fun addPokemon(mon: String, tier: String, identifier: String? = null) = dbTransaction {
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

    suspend fun getTierOfCommand(pokemon: DraftName, insertedTier: String?): TierData? {
        val (real, points) = dbTransaction {
            selectAll().where { basePredicate and (POKEMON eq if (isEnglish) pokemon.otherTl!! else pokemon.tlName) }
                .map { it[TIER] to it[POINTS] }.firstOrNull()
        } ?: return null
        return if (insertedTier != null && mode.withTiers) {
            TierData(order.firstOrNull {
                insertedTier.equals(
                    it, ignoreCase = true
                )
            } ?: (if (variableMegaPrice && insertedTier.equals("Mega", ignoreCase = true)) "Mega" else ""),
                real, points)
        } else {
            TierData(real, real, points)
        }
    }

    suspend fun getPointsOf(mon: String) = dbTransaction {
        selectAll().where { basePredicate and (POKEMON eq mon) }.map { it[POINTS] }.firstOrNull()
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

    suspend fun addOrUpdateTier(mon: String, tier: String, identifier: String? = null) {
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
        val POKEMON = varchar("pokemon", 64)
        val TIER = varchar("tier", 8)
        val TYPE = varchar("type", 10).nullable()
        val POINTS = integer("points").nullable()
        val IDENTIFIER = varchar("identifier", 30).nullable()

        init {
            index(isUnique = false, GUILD, IDENTIFIER, POKEMON)
        }

        private var setupCalled = false

        val tierlists: MutableMap<Long, MutableMap<String, Tierlist>> = mutableMapOf()
        suspend fun setup() {
            tierlists.clear()
            setupCalled = true
            db.tierlist.find().toFlow().collect { it.setup() }
        }

        /**
         * Gets the tierlist for the given guild (or fetches it in case it's not in the cache, which is only possible in test env)
         */
        operator fun get(guild: Long, identifier: String? = null): Tierlist? {
            return tierlists[guild]?.get(identifier ?: "")
                ?: if (setupCalled) null
                else runBlocking { db.tierlist.findOne(Tierlist::guildid eq guild) }?.apply { setup() }
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
