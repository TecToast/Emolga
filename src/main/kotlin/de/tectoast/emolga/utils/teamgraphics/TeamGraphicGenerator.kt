package de.tectoast.emolga.utils.teamgraphics

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.database.exposed.PokemonCropDB
import de.tectoast.emolga.database.exposed.TeamGraphicChannelDB
import de.tectoast.emolga.database.exposed.TeamGraphicMessageDB
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.OneTimeCache
import de.tectoast.emolga.utils.SizeLimitedMap
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.json.SignUpInput
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.toSDName
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.editMessage
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.utils.FileUpload
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.selectAll
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

object TeamGraphicGenerator {
    suspend fun generateForLeague(
        league: League, style: TeamGraphicStyle
    ): List<BufferedImage> {
        val teamDataList = TeamData.allFromLeague(league)
        return teamDataList.map { teamData ->
            logger.info { "Generating team graphic for ${teamData.teamOwner ?: "Unknown Owner"}" }
            generate(teamData, style)
        }
    }

    suspend fun generateAndSendForLeague(league: League, style: TeamGraphicStyle, channel: MessageChannel) {
        coroutineScope {
            TeamGraphicChannelDB.set(league.leaguename, channel.idLong)
            for ((idx, image) in generateForLeague(league, style).withIndex()) {
                launch {
                    val id = channel.send("<@${league.table[idx]}>", files = image.toFileUpload().into()).await().idLong
                    TeamGraphicMessageDB.set(league.leaguename, idx, id)
                }
            }
        }
    }

    suspend fun editTeamGraphicForLeague(league: League, idx: Int) {
        val style = league.config.teamgraphics?.style ?: return
        val teamData = TeamData.singleFromLeague(league, idx)
        val tcid = TeamGraphicChannelDB.getChannelId(league.leaguename) ?: return
        val msgid = TeamGraphicMessageDB.getMessageId(league.leaguename, idx) ?: return
        jda.getTextChannelById(tcid)!!.editMessage(
            id = msgid.toString(),
            attachments = generate(teamData, style).toFileUpload().into()
        ).queue()
    }

    suspend fun generate(
        teamData: TeamData, style: TeamGraphicStyle
    ): BufferedImage {
        val monData = dbTransaction {
            PokemonCropDB.selectAll()
                .where { PokemonCropDB.GUILD eq style.guild and (PokemonCropDB.OFFICIAL.inList(teamData.englishNames.values)) }
                .associate { it[PokemonCropDB.OFFICIAL] to it.toDrawData() }
        }
        return createOneTeamGraphic(
            teamData.teamOwner,
            teamData.teamName,
            teamData.logo,
            teamData.englishNames.mapValues { (_, name) ->
                (monData[name] ?: error("MonData for $name not found"))
            }.toMap(),
            style
        )
    }

    private fun ResultRow.toDrawData(): DrawData {
        return DrawData(
            name = this[PokemonCropDB.OFFICIAL],
            x = this[PokemonCropDB.X],
            y = this[PokemonCropDB.Y],
            size = this[PokemonCropDB.SIZE],
            flipped = this[PokemonCropDB.FLIPPED]
        )
    }

    private suspend fun createOneTeamGraphic(
        teamOwner: String?,
        teamName: String?,
        logo: BufferedImage?,
        monData: Map<Int, DrawData>,
        style: TeamGraphicStyle
    ): BufferedImage {
        val image = withContext(Dispatchers.IO) {
            ImageIO.read(File(style.backgroundPath))
        }
        val g2d = image.createGraphics()
        g2d.setRenderingHints()
        g2d.drawOptionalText(teamOwner?.let(style::transformUsername), style.playerText)
        g2d.drawOptionalText(teamName, style.teamnameText)
        g2d.drawMons(monData, style)
        style.overlayPath?.let {
            g2d.drawImage(withContext(Dispatchers.IO) {
                ImageIO.read(File(it))
            }, 0, 0, null)
        }
        g2d.drawLogo(
            logo ?: style.logoProperties?.defaultLogoPath?.let { ImageIO.read(File(it)) },
            style.logoProperties
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
            val blurredShadow =
                ImageUtils.createBlurredShadowImage(text, this.font, shadow.color, shadow.blurRadius)
            val padding = shadow.blurRadius * 2
            val shadowX = x + shadow.offset - padding
            val shadowY = y + shadow.offset - padding - this.font.size
            drawImage(blurredShadow, shadowX, shadowY, null)
        }

        this.drawString(text, x, y)
    }

    private suspend fun Graphics2D.drawMons(monData: Map<Int, DrawData>, style: TeamGraphicStyle) {
        for ((i, data) in monData) {
            val path = db.pokedex.get(data.name.toSDName())!!.calcSpriteName()
            val image = withContext(Dispatchers.IO) {
                ImageIO.read(File("teamgraphics/sugimori_final/$path.png"))
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

    private fun BufferedImage.flipIf(doFlip: Boolean): BufferedImage {
        if (!doFlip) return this
        val tx = AffineTransform.getScaleInstance(
            -1.0,
            1.0
        )

        // Move the image back into the viewport
        val translateX = -width.toDouble()
        val translateY = 0.0
        tx.translate(translateX, translateY)

        val op = AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR)
        return op.filter(this, null)
    }

    private fun BufferedImage.cropShape(x: Int, y: Int, shape: Shape): BufferedImage {
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

    private fun Graphics2D.setRenderingHints() {
        setRenderingHint(
            RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON
        )
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

    data class TeamData(
        val teamOwner: String?, val teamName: String?, val logo: BufferedImage?, val englishNames: Map<Int, String>
    ) {
        companion object {
            suspend fun allFromLeague(league: League): List<TeamData> {
                val userNameProvider = JDALeagueUserNameProvider(league = league)
                return (league.table.indices).map {
                    logger.info { "Generating TeamData for team index $it" }
                    singleFromLeague(league, it, userNameProvider)
                }
            }

            suspend fun singleFromLeague(
                league: League,
                idx: Int,
                userNameProvider: UserNameProvider = JDADirectUserNameProvider.default,
                overridePicks: Map<Int, String>? = null
            ): TeamData {
                val englishNames =
                    overridePicks ?: league.picks(idx).inDocOrder(league)
                        .mapValues {
                            NameConventionsDB.getSDTranslation(
                                it.value.name,
                                league.guild,
                                english = true
                            )!!.official
                        }
                val lsData = db.signups.get(league.guild)!!
                val uid = league.table[idx]
                val userData = lsData.getDataByUser(uid)!!
                val teamOwner = userNameProvider.getUserName(uid)
                val teamName = userData.data[SignUpInput.TEAMNAME_ID]
                val logo = userData.downloadLogo()?.let { ImageUtils.cropToContent(it) }
                return TeamData(
                    teamOwner = teamOwner, teamName = teamName, logo = logo, englishNames = englishNames
                )
            }
        }
    }

    private fun List<DraftPokemon>.inDocOrder(league: League) =
        league.tierlist.withTierBasedPriceManager { it.getPicksWithInsertOrder(league, this@inDocOrder) } ?: sortedWith(
            league.tierorderingComparator
        ).mapIndexed { index, pokemon -> index to pokemon }.toMap()


    private val logger = KotlinLogging.logger {}

}

fun BufferedImage.toFileUpload(fileName: String = "teamgraphic") = FileUpload.fromData(ByteArrayOutputStream().also {
    ImageIO.write(
        this, "png", it
    )
}.toByteArray(), "$fileName.png")

interface UserNameProvider {
    suspend fun getUserName(userId: Long): String
}

data class JDADirectUserNameProvider(val jda: JDA = de.tectoast.emolga.bot.jda) : UserNameProvider {
    val cache = SizeLimitedMap<Long, String>()
    override suspend fun getUserName(userId: Long): String {
        return cache.getOrPut(userId) { jda.retrieveUserById(userId).await().effectiveName }
    }

    companion object {
        val default = JDADirectUserNameProvider()
    }
}

data class JDALeagueUserNameProvider(val jda: JDA = de.tectoast.emolga.bot.jda, val league: League) : UserNameProvider {
    private val cache = OneTimeCache {
        val guild = jda.getGuildById(league.guild)!!
        val members = guild.retrieveMembersByIds(league.table).await()
        members.associate { it.idLong to it.user.effectiveName }
    }

    override suspend fun getUserName(userId: Long): String {
        return cache()[userId] ?: "Unknown User"
    }
}
