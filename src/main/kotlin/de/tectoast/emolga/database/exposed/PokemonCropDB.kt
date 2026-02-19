package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.json.mdb
import de.tectoast.emolga.utils.toSDName
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.update
import org.jetbrains.exposed.v1.r2dbc.upsert
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
    val FLIPPED = bool("flipped").default(false)
    val WIP_SINCE = timestamp("wip_since").nullable()

    override val primaryKey = PrimaryKey(GUILD, OFFICIAL)
}

object CropAuxiliaryDB : Table("pokemon_crop_auxiliary") {
    val GUILD = long("guild")
    val POKEMON = varchar("official", 50)

    override val primaryKey = PrimaryKey(GUILD, POKEMON)
}

@OptIn(ExperimentalTime::class)
object PokemonCropService {
    val mutex = Mutex()
    private val logger = KotlinLogging.logger {}
    suspend fun getNewPokemonToCrop(guild: Long): PokemonToCropData? {
//        if(true) return PokemonToCropData("Umbreon", "Umbreon", "/api/emolga/${guild}/teamgraphics/img/umbreon.png")
        return mutex.withLock {
            dbTransaction {
                val result = CropAuxiliaryDB.leftJoin(PokemonCropDB, additionalConstraint = {
                    (CropAuxiliaryDB.GUILD eq PokemonCropDB.GUILD) and (CropAuxiliaryDB.POKEMON eq PokemonCropDB.OFFICIAL)
                })
                    .select(
                        CropAuxiliaryDB.POKEMON,
                        PokemonCropDB.GUILD,
                        PokemonCropDB.WIP_SINCE,
                    )
                    .where {
                        CropAuxiliaryDB.GUILD eq guild and (PokemonCropDB.GUILD.isNull() or (PokemonCropDB.WIP_SINCE less Clock.System.now()
                            .minus(10.minutes)))
                    }
                    .orderBy(Random())
                    .limit(1)
                    .firstOrNull()
                    ?.let { row ->
                        val official = row[CropAuxiliaryDB.POKEMON]
                        val sdName = official.toSDName()
                        val pokemon = mdb.pokedex.get(sdName)!!
                        val spriteName = pokemon.calcSpriteName()
                        val path = "/api/emolga/${guild}/teamgraphics/img/$spriteName.png"
                        PokemonToCropData(official, official, path)
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
            it[FLIPPED] = data.flipped
            it[WIP_SINCE] = null
        }
    }
}

@Serializable
data class PokemonToCropData(val tlName: String, val official: String, val path: String)

@Serializable
data class PokemonCropData(val official: String, val x: Int, val y: Int, val size: Int, val flipped: Boolean)
