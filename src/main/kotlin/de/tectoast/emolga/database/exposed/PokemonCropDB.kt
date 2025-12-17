package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.toSDName
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object PokemonCropDB : Table("pokemon_crop") {
    val GUILD = long("guild")
    val OFFICIAL = varchar("official", 50)
    val X = integer("x").default(0)
    val Y = integer("y").default(0)
    val USER = long("user").default(0)
    val SIZE = integer("size").default(0)
    val WIP_SINCE = timestamp("wip_since").nullable()

    override val primaryKey = PrimaryKey(GUILD, OFFICIAL)
}

@OptIn(ExperimentalTime::class)
object PokemonCropService {
    val mutex = Mutex()
    suspend fun getNewPokemonToCrop(guild: Long): PokemonToCropData? {
        return mutex.withLock {
            dbTransaction {
                val ncSpecific = NameConventionsDB.alias("nc_specific")
                val ncDefault = NameConventionsDB.alias("nc_default")
                val displayNameExpression = Coalesce(
                    ncSpecific[NameConventionsDB.ENGLISH],
                    ncDefault[NameConventionsDB.ENGLISH],
                    Tierlist.POKEMON
                )
                val aliasedDisplayNameExpression = displayNameExpression.alias("display_name")
                val result = Tierlist
                    .join(
                        ncSpecific,
                        JoinType.LEFT,
                        additionalConstraint = {
                            (Tierlist.POKEMON eq ncSpecific[NameConventionsDB.SPECIFIED]) and
                                    (ncSpecific[NameConventionsDB.GUILD] eq Tierlist.GUILD)
                        }
                    )
                    .join(
                        ncDefault,
                        JoinType.LEFT,
                        additionalConstraint = {
                            (Tierlist.POKEMON eq ncDefault[NameConventionsDB.SPECIFIED]) and
                                    (ncDefault[NameConventionsDB.GUILD] eq 0L)
                        }
                    )
                    .join(PokemonCropDB, JoinType.LEFT, additionalConstraint = {
                        (PokemonCropDB.GUILD eq Tierlist.GUILD) and
                                (PokemonCropDB.OFFICIAL eq displayNameExpression)
                    })
                    .select(
                        Tierlist.POKEMON,
                        Tierlist.GUILD,
                        aliasedDisplayNameExpression,
                        PokemonCropDB.GUILD,
                        PokemonCropDB.WIP_SINCE
                    )
                    .where {
                        Tierlist.GUILD eq guild and (PokemonCropDB.GUILD.isNull() or (PokemonCropDB.WIP_SINCE less Clock.System.now()
                            .minus(10.minutes)))
                    }
                    .orderBy(Random())
                    .limit(1)
                    .firstOrNull()?.let { row ->
                        val official = row[aliasedDisplayNameExpression]
                        val spriteName =
                            de.tectoast.emolga.utils.json.db.pokedex.get(official.toSDName())!!.calcSpriteName()
                        val tlName = row[Tierlist.POKEMON]
                        val path = "/api/emolga/${guild}/teamgraphics/img/$spriteName.png"
                        PokemonToCropData(tlName, official, path)
                    }
                if (result != null) {
                    PokemonCropDB.upsert {
                        it[GUILD] = guild
                        it[OFFICIAL] = result.official
                        it[WIP_SINCE] = Clock.System.now()
                    }
                }
                result
            }
        }
    }

    suspend fun insertPokemonCropData(
        guild: Long,
        data: PokemonCropData,
        user: Long,
    ) = dbTransaction {
        PokemonCropDB.update(where = {
            (PokemonCropDB.GUILD eq guild) and (PokemonCropDB.OFFICIAL eq data.official)
        }) {
            it[X] = data.x
            it[Y] = data.y
            it[SIZE] = data.size
            it[USER] = user
            it[WIP_SINCE] = null
        }
    }
}

@Serializable
data class PokemonToCropData(val tlName: String, val official: String, val path: String)

@Serializable
data class PokemonCropData(val official: String, val x: Int, val y: Int, val size: Int)