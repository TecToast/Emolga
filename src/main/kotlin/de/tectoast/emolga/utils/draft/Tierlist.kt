package de.tectoast.emolga.utils.draft

import de.tectoast.emolga.commands.Language
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.utils.SizeLimitedMap
import de.tectoast.emolga.utils.json.emolga.draft.League
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@Suppress("unused")
@Serializable
class Tierlist constructor(val guildid: Long) {

    /**
     * The price for each tier
     */
    val prices: MutableMap<String, Int> = mutableMapOf()
    val freepicks: MutableMap<String, Int> = mutableMapOf()

    var mode: TierlistMode = TierlistMode.POINTS
    var points = 0
    val language = Language.GERMAN


    val order get() = prices.keys.toList()

    val freePicksAmount get() = freepicks["#AMOUNT#"] ?: 0

    val autoComplete: Set<String> by lazy(::getAllForAutoComplete)

    fun setup() {
        tierlists[this.guildid] = this
    }


    fun getPointsNeeded(s: String): Int = when (mode) {
        TierlistMode.POINTS -> prices[getTierOf(s)] ?: error("Tier for $s not found")
        TierlistMode.TIERS_WITH_FREE -> freepicks[getTierOf(s)] ?: error("Tier for $s not found")
        else -> error("Unknown mode for points $mode")
    }

    fun addPokemon(mon: String, tier: String) = transaction {
        insert {
            it[guild] = this@Tierlist.guildid
            it[pokemon] = mon
            it[this.tier] = tier
        }
    }

    fun getByTier(tier: String): List<String>? {
        return transaction {
            select { guild eq guildid and (Tierlist.tier eq tier) }.map { it[pokemon] }.ifEmpty { null }
        }
    }

    private fun getAllForAutoComplete() = transaction {
        val list = select { guild eq guildid }.map { it[pokemon] }
        (list + NameConventionsDB.getAllOtherSpecified(list, language, guildid)).toSet()
    }

    @Transient
    private val tierCache = SizeLimitedMap<String, String>()

    fun getTierOf(mon: String) = tierCache.getOrElse(mon) {
        transaction {
            select { guild eq guildid and (pokemon eq mon) }.map { it[tier] }.firstOrNull()
        }
    }

    fun retrieveTierlistMap(map: Map<String, Int>) = transaction {
        map.entries.flatMap { (tier, amount) ->
            select { guild eq guildid and (Tierlist.tier eq tier) }.orderBy(Random()).limit(amount)
                .map { DraftPokemon(it[pokemon], tier) }
        }
    }

    val monCount
        get() = transaction {
            select { guild eq guildid }.count().toInt()
        }

    companion object : ReadOnlyProperty<League, Tierlist>, Table("tierlists") {
        /**
         * All tierlists
         */
        val guild = long("guild")
        val pokemon = varchar("pokemon", 30)
        val tier = varchar("tier", 8)

        val tierlists: MutableMap<Long, Tierlist> = mutableMapOf()
        private val REPLACE_NONSENSE = Regex("[^a-zA-Z\\d-:%ßäöüÄÖÜé ]")
        fun setup() {
            tierlists.clear()
            for (file in File("./Tierlists/").listFiles()!!) {
                if (file.isFile) Json.decodeFromString<Tierlist>(file.readText()).setup()
            }
        }

        override fun getValue(thisRef: League, property: KProperty<*>): Tierlist {
            return get(thisRef.guild)!!
        }

        operator fun get(guild: Long) = tierlists[guild]
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
