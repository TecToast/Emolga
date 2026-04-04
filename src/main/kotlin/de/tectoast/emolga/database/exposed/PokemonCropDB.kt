package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.ktor.TeamgraphicsSpriteStyle
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.json.mdb
import de.tectoast.emolga.utils.teamgraphics.DrawData
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
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object PokemonCropTable : Table("pokemon_crop") {
    val guild = long("guild")
    val official = varchar("official", 50)
    val x = integer("x").default(0)
    val y = integer("y").default(0)
    val user = long("user").default(0)
    val size = integer("size").default(0)
    val flipped = bool("flipped").default(false)
    val wipSince = timestamp("wip_since").nullable()

    override val primaryKey = PrimaryKey(guild, official)
}

object CropAuxiliaryTable : Table("pokemon_crop_auxiliary") {
    val guild = long("guild")
    val pokemon = varchar("official", 50)

    override val primaryKey = PrimaryKey(guild, pokemon)

    init {
        foreignKey(
            guild to TeamGraphicsMetaTable.GUILD,
            onDelete = ReferenceOption.CASCADE,
            onUpdate = ReferenceOption.CASCADE
        )
    }
}

@Single
@OptIn(ExperimentalTime::class)
class PokemonCropRepository(
    private val db: R2dbcDatabase,
    private val teamGraphicsMetaRepo: TeamGraphicsMetaRepository
) {
    private val mutex = Mutex()
    private val logger = KotlinLogging.logger {}

    suspend fun getNewPokemonToCrop(guild: Long): PokemonToCropData? = mutex.withLock {
        suspendTransaction(db) {
            val spriteStyle = teamGraphicsMetaRepo.getSpriteStyle(guild) ?: return@suspendTransaction null
            val result = CropAuxiliaryTable.leftJoin(PokemonCropTable, additionalConstraint = {
                (CropAuxiliaryTable.guild eq PokemonCropTable.guild) and (CropAuxiliaryTable.pokemon eq PokemonCropTable.official)
            })
                .select(
                    CropAuxiliaryTable.pokemon,
                    PokemonCropTable.guild,
                    PokemonCropTable.wipSince,
                )
                .where {
                    CropAuxiliaryTable.guild eq guild and (PokemonCropTable.guild.isNull() or (PokemonCropTable.wipSince less Clock.System.now()
                        .minus(1.minutes)))
                }
                .orderBy(Random())
                .limit(1)
                .firstOrNull()
                ?.let { row ->
                    val official = row[CropAuxiliaryTable.pokemon]
                    val sdName = official.toSDName()
                    val pokemon = mdb.pokedex.get(sdName)!!
                    val spriteName = pokemon.calcSpriteName()
                    val path = "/api/emolga/${guild}/teamgraphics/img/$spriteStyle/$spriteName.png"
                    val done = PokemonCropTable.selectAll().where { PokemonCropTable.guild eq guild }.count()
                    val total = CropAuxiliaryTable.selectAll().where { CropAuxiliaryTable.guild eq guild }.count()
                    PokemonToCropData(official, official, path, done, total)
                }
            if (result != null) {
                PokemonCropTable.upsert {
                    it[this.guild] = guild
                    it[official] = result.official
                    it[wipSince] = Clock.System.now()
                }
            }
            result
        }
    }

    suspend fun insertPokemonCropData(
        guild: Long,
        data: PokemonCropData,
        user: Long,
    ): Unit = suspendTransaction(db) {
        PokemonCropTable.update(where = {
            (PokemonCropTable.guild eq guild) and (PokemonCropTable.official eq data.official)
        }) {
            it[PokemonCropTable.x] = data.x
            it[PokemonCropTable.y] = data.y
            it[PokemonCropTable.size] = data.size
            it[PokemonCropTable.user] = user
            it[PokemonCropTable.flipped] = data.flipped
            it[PokemonCropTable.wipSince] = null
        }
    }

    suspend fun generateOverviewImage(guild: Long): Unit = suspendTransaction(db) {
        val spriteStyle = teamGraphicsMetaRepo.getSpriteStyle(guild) ?: return@suspendTransaction
        val list =
            PokemonCropTable.selectAll()
                .where { PokemonCropTable.guild eq guild and PokemonCropTable.wipSince.isNull() }
                .toList()
        val listSize = list.size
        val baseImage = BufferedImage(128 * 10, 128 * ((listSize + 9) / 10), BufferedImage.TYPE_INT_ARGB)
        val g2d = baseImage.createGraphics()
        g2d.setCommonRenderingHints()
        val map = list.associate {
            val official = it[PokemonCropTable.official]
            official to mdb.pokedex.get(official.toSDName())!!
        }
        for ((index, row) in list.sortedBy { map[it[PokemonCropTable.official]]!!.num }.withIndex()) {
            val official = row[PokemonCropTable.official]
            val spriteName = map[official]!!.calcSpriteName()
            val image = withContext(Dispatchers.IO) {
                ImageIO.read(File("/teamgraphics/sprites/$spriteStyle/$spriteName.png"))
            }
            val size = row[PokemonCropTable.size].toFloat()
            val shape = Ellipse2D.Float(0f, 0f, size, size)
            g2d.drawImage(
                image.flipIf(row[PokemonCropTable.flipped])
                    .cropShape(row[PokemonCropTable.x], row[PokemonCropTable.y], shape),
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

    suspend fun fillCropAuxiliary(guild: Long, pokemonList: List<String>): Unit = suspendTransaction(db) {
        CropAuxiliaryTable.batchInsert(pokemonList, shouldReturnGeneratedValues = false, ignore = true) {
            this[CropAuxiliaryTable.guild] = guild
            this[CropAuxiliaryTable.pokemon] = it
        }
    }

    suspend fun getDrawData(guild: Long, officialNames: List<String>): Map<String, DrawData> =
        suspendTransaction(db) {
            PokemonCropTable.selectAll()
                .where { PokemonCropTable.guild eq guild and (PokemonCropTable.official.inList(officialNames)) }
                .toMap { row ->
                    row[PokemonCropTable.official] to DrawData(
                        name = row[PokemonCropTable.official],
                        x = row[PokemonCropTable.x],
                        y = row[PokemonCropTable.y],
                        size = row[PokemonCropTable.size],
                        flipped = row[PokemonCropTable.flipped]
                    )
                }
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
