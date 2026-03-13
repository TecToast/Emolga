package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.ktor.TeamgraphicsSpriteStyle
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.json.mdb
import de.tectoast.emolga.utils.teamgraphics.cropShape
import de.tectoast.emolga.utils.teamgraphics.flipIf
import de.tectoast.emolga.utils.teamgraphics.setCommonRenderingHints
import de.tectoast.emolga.utils.toSDName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.update
import org.jetbrains.exposed.v1.r2dbc.upsert
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
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

    init {
        foreignKey(
            GUILD to TeamGraphicsMetaDB.GUILD,
            onDelete = ReferenceOption.CASCADE,
            onUpdate = ReferenceOption.CASCADE
        )
    }
}

@OptIn(ExperimentalTime::class)
object PokemonCropService {
    val mutex = Mutex()
    private val logger = KotlinLogging.logger {}
    suspend fun getNewPokemonToCrop(guild: Long): PokemonToCropData? {
//        if(true) return PokemonToCropData("Umbreon", "Umbreon", "/api/emolga/${guild}/teamgraphics/img/umbreon.png")
        return mutex.withLock {
            dbTransaction {
                val spriteStyle = TeamGraphicsMetaDB.getSpriteStyle(guild) ?: return@dbTransaction null
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
                            .minus(1.minutes)))
                    }
                    .orderBy(Random())
                    .limit(1)
                    .firstOrNull()
                    ?.let { row ->
                        val official = row[CropAuxiliaryDB.POKEMON]
                        val sdName = official.toSDName()
                        val pokemon = mdb.pokedex.get(sdName)!!
                        val spriteName = pokemon.calcSpriteName()
                        val path = "/api/emolga/${guild}/teamgraphics/img/$spriteStyle/$spriteName.png"
                        val done = PokemonCropDB.selectAll().where { PokemonCropDB.GUILD eq guild }.count()
                        val total = CropAuxiliaryDB.selectAll().where { CropAuxiliaryDB.GUILD eq guild }.count()
                        PokemonToCropData(official, official, path, done, total)
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

    suspend fun generateOverviewImage(guild: Long) = dbTransaction {
        val spriteStyle = TeamGraphicsMetaDB.getSpriteStyle(guild) ?: return@dbTransaction null
        val list =
            PokemonCropDB.selectAll().where { PokemonCropDB.GUILD eq guild and PokemonCropDB.WIP_SINCE.isNull() }
                .toList()
        val listSize = list.size
        val baseImage = BufferedImage(128 * 10, 128 * ((listSize + 9) / 10), BufferedImage.TYPE_INT_ARGB)
        val g2d = baseImage.createGraphics()
        g2d.setCommonRenderingHints()
        val map = list.associate {
            val official = it[PokemonCropDB.OFFICIAL]
            official to mdb.pokedex.get(official.toSDName())!!
        }
        for ((index, row) in list.sortedBy { map[it[PokemonCropDB.OFFICIAL]]!!.num }.withIndex()) {
            val official = row[PokemonCropDB.OFFICIAL]
            val spriteName = map[official]!!.calcSpriteName()
            val image = withContext(Dispatchers.IO) {
                ImageIO.read(File("/teamgraphics/sprites/$spriteStyle/$spriteName.png"))
            }
            val size = row[PokemonCropDB.SIZE].toFloat()
            val shape = Ellipse2D.Float(0f, 0f, size, size)
            g2d.drawImage(
                image.flipIf(row[PokemonCropDB.FLIPPED]).cropShape(row[PokemonCropDB.X], row[PokemonCropDB.Y], shape),
                (index % 10) * 128,
                (index / 10) * 128,
                128,
                128,
                null
            )
        }
        g2d.dispose()
        ImageIO.write(baseImage, "png", File("/teamgraphics/$guild.png"))
    }
}

@Serializable
data class PokemonToCropData(
    val tlName: String,
    val official: String,
    val path: String,
    val done: Long,
    val total: Long
)

@Serializable
data class PokemonCropData(val official: String, val x: Int, val y: Int, val size: Int, val flipped: Boolean)

@Serializable
data class PokemonCropOverview(val spriteStyle: TeamgraphicsSpriteStyle, val data: List<PokemonCropInfoData>)

@Serializable
data class PokemonCropInfoData(
    val spriteName: String,
    val official: String,
    val x: Int,
    val y: Int,
    val size: Int,
    val flipped: Boolean
)