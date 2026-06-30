package de.tectoast.emolga.domain.league.teamgraphic.service

import de.tectoast.emolga.domain.league.teamgraphic.model.PokemonToCropData
import de.tectoast.emolga.domain.league.teamgraphic.repository.PokemonCropRepository
import de.tectoast.emolga.domain.league.teamgraphic.repository.PokemonCropTable
import de.tectoast.emolga.domain.league.teamgraphic.repository.TeamGraphicMetaRepository
import de.tectoast.emolga.domain.pokemon.repository.PokedexRepository
import de.tectoast.emolga.domain.pokemon.service.PokemonDisplayService
import de.tectoast.emolga.utils.Language
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.time.Clock

@Single
class PokemonCropService(
    private val cropRepo: PokemonCropRepository,
    private val teamGraphicsMetaRepo: TeamGraphicMetaRepository,
    private val pokedexRepo: PokedexRepository,
    private val pokemonDisplayService: PokemonDisplayService,
    private val clock: Clock
) {
    private val mutex = Mutex()
    suspend fun getNewPokemonToCrop(guild: Long): PokemonToCropData? = mutex.withLock {
        val spriteStyle = teamGraphicsMetaRepo.getSpriteStyle(guild) ?: return@withLock null
        val result = cropRepo.getNewPokemonToCrop(guild, clock.now())
            ?.let { sdName ->
                val pokemon = pokedexRepo.get(sdName)!!
                val spriteName = pokemon.calcSpriteName()
                val path = "/api/emolga/monimg/$spriteStyle/$spriteName.png"
                val done = cropRepo.getDoneCount(guild)
                val total = cropRepo.getTotalCount(guild)
                PokemonToCropData(
                    pokemonDisplayService.getDisplayName(sdName, guild, Language.ENGLISH),
                    sdName,
                    path,
                    done,
                    total
                )
            }
        if (result != null) {
            PokemonCropTable.upsert {
                it[this.guild] = guild
                it[showdownId] = result.official
                it[wipSince] = clock.now()
            }
        }
        result
    }

    suspend fun generateOverviewImage(guild: Long) {
        val spriteStyle = teamGraphicsMetaRepo.getSpriteStyle(guild) ?: return
        val list = cropRepo.getFinished(guild)
        val listSize = list.size
        val baseImage = BufferedImage(128 * 10, 128 * ((listSize + 9) / 10), BufferedImage.TYPE_INT_ARGB)
        val g2d = baseImage.createGraphics()
        g2d.setCommonRenderingHints()
        val map = pokedexRepo.getAll(list.map { it.showdownId })
        for ((index, data) in list.sortedBy { map[it.showdownId]!!.num }.withIndex()) {
            val showdownId = data.showdownId
            val spriteName = map[showdownId]!!.calcSpriteName()
            val image = withContext(Dispatchers.IO) {
                ImageIO.read(File("/teamgraphics/sprites/$spriteStyle/$spriteName.png"))
            }
            val size = data.size.toFloat()
            val shape = Ellipse2D.Float(0f, 0f, size, size)
            g2d.drawImage(
                image.flipIf(data.flipped)
                    .cropShape(data.x, data.y, shape),
                (index % 10) * 128,
                (index / 10) * 128,
                128,
                128,
                null
            )
        }
        g2d.dispose()
        withContext(Dispatchers.IO) {
            ImageIO.write(baseImage, "png", File("/teamgraphics/$guild.png"))
        }
    }
}