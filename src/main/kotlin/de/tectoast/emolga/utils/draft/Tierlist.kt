package de.tectoast.emolga.utils.draft

import de.tectoast.emolga.database.exposed.NameConventions
import de.tectoast.emolga.utils.json.emolga.draft.League
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.Random
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@Suppress("unused")
@Serializable
class Tierlist(val guild: Long) {

    /**
     * The price for each tier
     */
    val prices: MutableMap<String, Int> = mutableMapOf()
    val freepicks: MutableMap<String, Int> = mutableMapOf()

    var mode: TierlistMode = TierlistMode.POINTS
    var points = 0


    val order get() = prices.keys.toList()


    /**
     * if this tierlist is pointbased
     */
    val isPointBased get() = mode == TierlistMode.POINTS

    val freePicksAmount get() = freepicks["#AMOUNT#"] ?: 0

    val autoComplete: Set<String> by lazy {
        getAllForAutoComplete(guild)
    }

    fun setup() {
        tierlists[this.guild] = this
    }


    fun getPointsNeeded(s: String): Int = when (mode) {
        TierlistMode.POINTS -> prices[getTierOf(s)] ?: error("Tier for $s not found")
        TierlistMode.TIERS_WITH_FREE -> freepicks[getTierOf(s)] ?: error("Tier for $s not found")
        else -> error("Unknown mode for points $mode")
    }

    fun getTierOf(s: String): String {
        return getTier(guild, s) ?: ""
    }

    fun addPokemon(mon: String, tier: String) = transaction {
        insert {
            it[guild] = this@Tierlist.guild
            it[pokemon] = mon
            it[this.tier] = tier
        }
    }

    fun getByTier(tier: String): List<String>? {
        return transaction {
            Tierlist.select { Tierlist.guild eq guild and (Tierlist.tier eq tier) }.map { it[pokemon] }.ifEmpty { null }
        }
    }

    companion object : ReadOnlyProperty<League, Tierlist>, Table("tierlists") {
        /**
         * All tierlists
         */
        val guild = long("guild")
        val pokemon = varchar("pokemon", 30)
        val tier = varchar("tier", 8)

        fun getAllForAutoComplete(guildId: Long) = transaction {
            val list = select { guild eq guildId }.map { it[pokemon] }
            (list + NameConventions.getAllEnglishSpecified(list)).toSet()
        }

        fun getTier(guildId: Long, mon: String) = transaction {
            select { guild eq guildId and (pokemon eq mon) }.map { it[tier] }.firstOrNull()
        }

        fun retrieveTierlistMap(guildId: Long, map: Map<String, Int>) = transaction {
            map.entries.flatMap { (tier, amount) ->
                select { guild eq guildId and (this@Companion.tier eq tier) }.orderBy(Random()).limit(amount)
                    .map { DraftPokemon(it[pokemon], tier) }
            }
        }

        val tierlists: MutableMap<Long, Tierlist> = mutableMapOf()
        private val REPLACE_NONSENSE = Regex("[^a-zA-Z\\d-:%ßäöüÄÖÜé ]")
        fun setup() {
            tierlists.clear()
            for (file in File("./Tierlists/").listFiles()!!) {
                if (file.isFile) Json.decodeFromString<Tierlist>(file.readText()).setup()
            }
        }

        fun getByGuild(guild: String): Tierlist? {
            return getByGuild(guild.toLong())
        }


        fun getByGuild(guild: Long): Tierlist? {
            return tierlists[guild]
        }

        override fun getValue(thisRef: League, property: KProperty<*>): Tierlist {
            return getByGuild(thisRef.guild)!!
        }
    }
}

@Suppress("unused")
enum class TierlistMode(val withPoints: Boolean, val withTiers: Boolean) {
    POINTS(true, false),
    TIERS(false, true),
    TIERS_WITH_FREE(true, true);

    fun isPoints() = this == POINTS
    fun isTiers() = this == TIERS
    fun isTiersWithFree() = this == TIERS_WITH_FREE

}
