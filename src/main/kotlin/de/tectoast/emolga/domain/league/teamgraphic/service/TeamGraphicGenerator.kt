package de.tectoast.emolga.domain.league.teamgraphic.service

import de.tectoast.emolga.discord.ChannelInterface
import de.tectoast.emolga.domain.league.config.repository.LeagueConfigRepository
import de.tectoast.emolga.domain.league.teamgraphic.model.DrawData
import de.tectoast.emolga.domain.league.teamgraphic.model.TeamData
import de.tectoast.emolga.domain.league.teamgraphic.model.TeamGraphicStyle
import de.tectoast.emolga.domain.league.teamgraphic.model.TeamgraphicSpriteStyle
import de.tectoast.emolga.domain.league.teamgraphic.repository.PokemonCropRepository
import de.tectoast.emolga.domain.league.teamgraphic.repository.TeamGraphicMetaRepository
import de.tectoast.emolga.domain.league.teamgraphic.repository.TeamGraphicRepository
import de.tectoast.emolga.domain.pokemon.repository.PokedexRepository
import de.tectoast.emolga.utils.joinToTeammates
import de.tectoast.emolga.utils.newThreadSafeCache
import de.tectoast.emolga.utils.teamgraphics.ImageUtils
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.MessageEdit
import dev.minn.jda.ktx.messages.into
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import net.dv8tion.jda.api.utils.FileUpload
import org.koin.core.annotation.Single
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Shape
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.min

@Single
class TeamGraphicGenerator(
    private val leagueConfigRepo: LeagueConfigRepository,
    private val teamDataCreationService: TeamDataCreationService,
    private val metaRepo: TeamGraphicMetaRepository,
    private val teamgraphicRepo: TeamGraphicRepository,
    private val cropRepo: PokemonCropRepository,
    private val pokedexRepo: PokedexRepository,
    private val channelInterface: ChannelInterface
) {
    private val logger = KotlinLogging.logger {}

    data class Options(val blankBackground: Boolean = false)

    private suspend fun getTeamGraphicStyleOfLeague(leagueName: String): TeamGraphicStyle {
        return leagueConfigRepo.getConfig(leagueName).teamgraphics?.style!!
    }

    private suspend fun generateForLeague(
        leagueName: String, style: TeamGraphicStyle? = null
    ): List<Pair<TeamData, BufferedImage>> {
        val teamDataList = teamDataCreationService.allFromLeague(leagueName)
        val style = style ?: getTeamGraphicStyleOfLeague(leagueName)
        return teamDataList.map { teamData ->
            logger.info { "Generating team graphic for ${teamData.teamOwner ?: "Unknown Owner"}" }
            teamData to generate(teamData, style)
        }
    }

    suspend fun generateAndSendForLeague(leagueName: String, channelId: Long, style: TeamGraphicStyle? = null) {
        teamgraphicRepo.setChannelId(leagueName, channelId)
        for ((idx, data) in generateForLeague(leagueName, style).withIndex()) {
            val (teamData, image) = data
            val id = channelInterface.sendMessage(
                channelId,
                MessageCreate(
                    teamData.users.joinToTeammates(),
                    files = image.toFileUpload().into()
                )
            )
            if (id != null)
                teamgraphicRepo.setMessageId(leagueName, idx, id)
        }
    }

    suspend fun generateAndStoreInFS(leagueName: String, style: TeamGraphicStyle?) {
        val targetDir = File("/teamgraphics/generated/${leagueName}")
        targetDir.mkdirs()
        withContext(Dispatchers.IO) {
            generateForLeague(leagueName, style).forEachIndexed { index, (_, image) ->
                ImageIO.write(image, "png", targetDir.resolve("$index.png"))
            }
        }
    }

    suspend fun editTeamGraphicForLeague(leagueName: String, idx: Int, style: TeamGraphicStyle? = null) {
        val teamData = teamDataCreationService.singleFromLeague(leagueName, idx)
        val tcid = teamgraphicRepo.getChannelId(leagueName) ?: return
        val msgid = teamgraphicRepo.getMessageId(leagueName, idx) ?: return
        val style = style ?: getTeamGraphicStyleOfLeague(leagueName)
        channelInterface.editMessage(
            channelId = tcid,
            messageId = msgid,
            MessageEdit(
                content = teamData.users.joinToTeammates(),
                files = generate(teamData, style).toFileUpload().into()
            )
        )
    }

    suspend fun generate(
        teamData: TeamData, style: TeamGraphicStyle, options: Options = Options()
    ): BufferedImage {
        val monData =
            cropRepo.getDrawData(style.guild, teamData.picks.values.toList())
        return createOneTeamGraphic(
            teamData.teamOwner,
            teamData.teamName,
            teamData.logo,
            teamData.leaguename,
            teamData.idx,
            teamData.picks.mapValues { (_, name) ->
                (monData[name] ?: error("MonData for $name not found"))
            }.toMap(),
            style,
            options
        )
    }

    private val diskImageCache = newThreadSafeCache<String, BufferedImage>(1000)
    private suspend fun loadImageForBase(path: String): BufferedImage {
        val base = fromCacheOrLoad(path)
        val copy = BufferedImage(base.width, base.height, BufferedImage.TYPE_INT_ARGB)
        copy.createGraphics().apply {
            drawImage(base, 0, 0, null)
            dispose()
        }
        return copy
    }

    private suspend fun fromCacheOrLoad(key: String) = fromCacheOrLoad(key) { key }

    private suspend inline fun fromCacheOrLoad(
        key: String, crossinline keyToPath: suspend (String) -> String
    ): BufferedImage {
        return diskImageCache.getOrPut(key) {
            withContext(Dispatchers.IO) {
                ImageIO.read(File(keyToPath(key)))
            }
        }
    }

    private suspend fun createOneTeamGraphic(
        teamOwner: String?,
        teamName: String?,
        logo: BufferedImage?,
        leaguename: String,
        idx: Int,
        monData: Map<Int, DrawData>,
        style: TeamGraphicStyle,
        options: Options
    ): BufferedImage {
        val bgPath = style.backgroundPath(leaguename, idx)
        val backgroundImage = loadImageForBase(bgPath)
        val image = if (options.blankBackground && !style.individualBackgrounds) {
            BufferedImage(backgroundImage.width, backgroundImage.height, BufferedImage.TYPE_INT_ARGB)
        } else {
            backgroundImage
        }
        val g2d = image.createGraphics()
        g2d.setCommonRenderingHints()
        g2d.drawOptionalText(teamOwner?.let(style::transformUsername), style.playerText)
        g2d.drawOptionalText(teamName, style.teamnameText)
        g2d.drawMons(
            monData,
            style,
            metaRepo.getSpriteStyle(style.guild) ?: TeamgraphicSpriteStyle.SUGIMORI
        )
        style.overlayPath(leaguename, idx)?.let {
            g2d.drawImage(fromCacheOrLoad(it), 0, 0, null)
        }
        g2d.drawLogo(
            logo ?: style.logoProperties?.defaultLogoPath?.let { fromCacheOrLoad(it) }, style.logoProperties
        )
        g2d.dispose()
        return image
    }

    private fun Graphics2D.drawLogo(logo: BufferedImage?, settings: TeamGraphicStyle.LogoProperties?) {
        if (logo == null || settings == null) return
        val imgWidth = logo.width
        val imgHeight = logo.height
        val scaleX = settings.width.toDouble() / imgWidth
        val scaleY = settings.height.toDouble() / imgHeight
        val scale = min(scaleX, scaleY)
        val newWidth = (imgWidth * scale).toInt()
        val newHeight = (imgHeight * scale).toInt()
        val x = settings.startX + (settings.width - newWidth) / 2
        val y = settings.startY + (settings.height - newHeight) / 2
        this.drawImage(logo, x, y, newWidth, newHeight, null)
    }

    private fun Graphics2D.drawOptionalText(
        text: String?, settings: TeamGraphicStyle.TextProperties?
    ) {
        if (text != null && settings != null) {
            drawText(text, settings)
        }
    }

    private fun Graphics2D.drawText(text: String, settings: TeamGraphicStyle.TextProperties) {
        this.font = settings.font
        while (this.fontMetrics.stringWidth(text) > (settings.maxSize ?: Int.MAX_VALUE)) {
            val newSize = this.font.size2D - 1f
            this.font = this.font.deriveFont(newSize)
        }
        this.color = settings.fontColor
        val (x, y) = settings.orientation.calculateTextCoordinates(
            this, text, settings.xCoord, settings.yCoord
        )
        settings.shadow?.let { shadow ->
            val blurredShadow = ImageUtils.createBlurredShadowImage(text, this.font, shadow.color, shadow.blurRadius)
            val padding = shadow.blurRadius * 2
            val shadowX = x + shadow.offset - padding
            val shadowY = y + shadow.offset - padding - this.font.size
            drawImage(blurredShadow, shadowX, shadowY, null)
        }

        this.drawString(text, x, y)
    }

    private val pathCache = newThreadSafeCache<String, String>(1000)

    private suspend fun Graphics2D.drawMons(
        monData: Map<Int, DrawData>,
        style: TeamGraphicStyle,
        spriteStyle: TeamgraphicSpriteStyle
    ) {
        for ((i, data) in monData) {
            val sdName = data.name
            val imagePath = pathCache.getOrPut("$spriteStyle/$sdName") {
                "/teamgraphics/sprites/$spriteStyle/${pokedexRepo.get(sdName)!!.calcSpriteName()}.png"
            }
            val image = withContext(Dispatchers.IO) {
                ImageIO.read(File(imagePath))
            }
            val dataForIndex = style.getDataForIndex(i, data)
            val size = style.sizeOfShape
            drawImage(
                image.flipIf(data.flipped).cropShape(
                    data.x, data.y, dataForIndex.shape
                ), dataForIndex.xInFinal, dataForIndex.yInFinal, size, size, null
            )
        }
    }


}

fun BufferedImage.toFileUpload(fileName: String = "teamgraphic") = FileUpload.fromData(ByteArrayOutputStream().also {
    ImageIO.write(
        this, "png", it
    )
}.toByteArray(), "$fileName.png")

fun BufferedImage.flipIf(doFlip: Boolean): BufferedImage {
    if (!doFlip) return this
    val tx = AffineTransform.getScaleInstance(
        -1.0, 1.0
    )

    // Move the image back into the viewport
    val translateX = -width.toDouble()
    val translateY = 0.0
    tx.translate(translateX, translateY)

    val op = AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR)
    return op.filter(this, null)
}

fun BufferedImage.cropShape(x: Int, y: Int, shape: Shape): BufferedImage {
    val size = maxOf(
        shape.bounds.width, shape.bounds.height
    )
    // 1. Create a new BufferedImage with support for transparency (ARGB)
    val output = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)

    // 2. Create the Graphics2D object to draw on the new image
    val g2 = output.createGraphics()

    // 3. Enable Anti-aliasing for smooth circle edges
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    // 5. Set the clip. Anything drawn after this is restricted to the inside of the circle
    g2.clip(shape)

    // 6. Draw the original image
    // We shift the image by -x and -y so the desired region aligns with the new image's (0,0)
    g2.drawImage(this, -x, -y, null)

    // 7. Clean up resources
    g2.dispose()

    return output
}

fun Graphics2D.setCommonRenderingHints() {
    setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY)
    setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY)
    setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE)
    setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
    setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
    setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
}
