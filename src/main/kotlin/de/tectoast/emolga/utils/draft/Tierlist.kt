package de.tectoast.emolga.utils.draft

import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.Language
import de.tectoast.emolga.utils.OneTimeCache
import de.tectoast.emolga.utils.SizeLimitedMap
import de.tectoast.emolga.utils.json.db
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.litote.kmongo.eq
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@Suppress("unused")
@Serializable
class Tierlist(val guildid: Long, val identifier: String? = null) {

    /**
     * The price for each tier
     */
    val prices: MutableMap<String, Int> = mutableMapOf()
    val freepicks: MutableMap<String, Int> = mutableMapOf()

    var mode: TierlistMode = TierlistMode.POINTS
    var points = 0
    var language = Language.GERMAN
    val variableMegaPrice = false
    val maxMonsToPay = Int.MAX_VALUE


    val order get() = prices.keys.toList()

    val freePicksAmount get() = freepicks["#AMOUNT#"] ?: 0
    val basePredicate get() = guild eq guildid and (identifierCol eq identifier)

    @Transient
    private val _autoComplete = OneTimeCache { getAllForAutoComplete() }
    suspend fun autoComplete() = _autoComplete() + addedViaCommand
    val addedViaCommand: MutableSet<String> = mutableSetOf()

    @Transient
    val tlToOfficialCache = SizeLimitedMap<String, String>(1000)

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

    suspend fun addPokemon(mon: String, tier: String) = newSuspendedTransaction {
        insert {
            it[guild] = this@Tierlist.guildid
            it[pokemon] = mon
            it[this.tier] = tier
            it[this.identifierCol] = identifier
        }
    }

    suspend fun getByTier(tier: String): List<String>? {
        return newSuspendedTransaction {
            selectAll().where { basePredicate and (Tierlist.tier eq tier) }.map { it[pokemon] }.ifEmpty { null }
        }
    }

    private suspend fun getAllForAutoComplete() = newSuspendedTransaction {
        val list = selectAll().where { basePredicate }.map { it[pokemon] }
        (list + NameConventionsDB.getAllOtherSpecified(list, language, guildid)).toSet()
    }

    suspend fun getTierOf(mon: String) =
        newSuspendedTransaction {
            selectAll().where { basePredicate and (pokemon eq mon) }.map { it[tier] }.firstOrNull()
        }


    suspend fun retrieveTierlistMap(map: Map<String, Int>) = newSuspendedTransaction {
        map.entries.flatMap { (tier, amount) ->
            selectAll().where { basePredicate and (Tierlist.tier eq tier) }.orderBy(Random()).limit(amount)
                .map { DraftPokemon(it[pokemon], tier) }
        }
    }

    suspend fun getWithTierAndType(providedTier: String, providedType: String) = newSuspendedTransaction {
        selectAll().where { basePredicate and (tier eq providedTier) and (type eq providedType) }
            .map { it[pokemon] }
    }

    suspend fun retrieveAll() = newSuspendedTransaction {
        selectAll().where { basePredicate }.map { DraftPokemon(it[pokemon], it[tier]) }
    }

    suspend fun addOrUpdateTier(mon: String, tier: String) {
        val existing = getTierOf(mon)
        if (existing != null) {
            if (existing != tier) {
                newSuspendedTransaction {
                    if (tier in order)
                        update({ basePredicate and (pokemon eq mon) }) {
                            it[this.tier] = tier
                        }
                    else deleteWhere { basePredicate and (pokemon eq mon) }
                }
            }
        } else {
            addPokemon(mon, tier)
        }
    }

    suspend fun deleteAllMons() = newSuspendedTransaction {
        deleteWhere { basePredicate }
    }

    suspend fun getMonCount() = newSuspendedTransaction {
        selectAll().where { basePredicate }.count().toInt()
    }

    companion object : ReadOnlyProperty<League, Tierlist>, Table("tierlists") {
        /**
         * All tierlists
         */
        val guild = long("guild")
        val pokemon = varchar("pokemon", 30)
        val tier = varchar("tier", 8)
        val type = varchar("type", 10).nullable()
        val identifierCol = varchar("identifier", 30).nullable()
        private var setupCalled = false

        val tierlists: MutableMap<Long, MutableMap<String, Tierlist>> = mutableMapOf()
        private val REPLACE_NONSENSE = Regex("[^a-zA-Z\\d-:%ßäöüÄÖÜé ]")
        suspend fun setup() {
            tierlists.clear()
            setupCalled = true
            db.tierlist.find().toFlow().collect { it.setup() }
        }

        override fun getValue(thisRef: League, property: KProperty<*>): Tierlist {
            return get(thisRef.guild, thisRef.config.customTierlist?.identifier)!!
        }

        /**
         * Gets the tierlist for the given guild (or fetches it in case it's not in the cache, which is only possible in test env)
         */
        operator fun get(guild: Long, identifier: String? = null): Tierlist? {
            return tierlists[guild]?.get(identifier ?: "")
                ?: if (setupCalled) null
                else runBlocking { db.tierlist.findOne(Tierlist::guildid eq guild) }?.apply { setup() }
        }
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
