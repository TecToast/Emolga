package de.tectoast.emolga.domain.league.teamgraphic.model

import de.tectoast.emolga.utils.serializer.ColorSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.io.File


@Serializable
data class TeamGraphicStyle(
    val guild: Long,
    val shapeInfo: TeamGraphicShapeInfo,
    val backgroundPathTemplates: List<String>,
    val overlayPathTemplates: List<String> = emptyList(),
    val dataForIndex: Map<Int, CanvasCoordinate>,
    val individualBackgrounds: Boolean,
    val playerText: TextProperties? = null,
    val teamnameText: TextProperties? = null,
    val logoProperties: LogoProperties? = null,
    val userNameSettings: TeamGraphicUserNameSettings = TeamGraphicUserNameSettings()
) {

    @Transient
    private val shortIdentRegex = Regex(".*S\\d+(.*)")

    fun backgroundPaths(parameters: TeamGraphicParameters): List<String> {
        return backgroundPathTemplates.map { it.applyTemplate(parameters) }
    }

    fun overlayPaths(parameters: TeamGraphicParameters): List<String> {
        return overlayPathTemplates.map { it.applyTemplate(parameters) }
    }

    private fun String.applyTemplate(parameters: TeamGraphicParameters): String {
        return replace("{leagueName}", parameters.leagueName ?: "")
            .replace("{idx}", parameters.idx?.toString() ?: "")
            .replace("{shortIdent}", parameters.leagueName?.let {
                shortIdentRegex.find(it)?.groupValues?.get(1)
            } ?: "")
    }

    @Serializable
    data class LogoProperties(
        val startX: Int,
        val startY: Int,
        val width: Int,
        val height: Int,
        val defaultLogoPath: String?
    )

    @Serializable
    data class TextProperties(
        val fontPath: String,
        val fontColor: FontColorProvider,
        val fontSize: Float,
        val xCoord: Int,
        val yCoord: Int,
        val orientation: TextAlignment,
        val maxSize: Int?,
        val shadow: TextShadowProperties?
    ) {
        val font: Font by lazy {
            val fontFile = File(fontPath)
            val rawFont = Font.createFont(Font.TRUETYPE_FONT, fontFile)
            val sizedFont = rawFont.deriveFont(fontSize)
            val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
            ge.registerFont(sizedFont)
            sizedFont
        }
    }

    @Serializable
    data class TextShadowProperties(
        @Serializable(with = ColorSerializer::class) val color: Color,
        val offset: Int,
        val blurRadius: Int
    )

    @Serializable
    enum class TextAlignment {
        CENTERED {
            override fun calculateTextCoordinates(
                g2d: Graphics2D,
                text: String,
                baseX: Int,
                baseY: Int
            ): Pair<Int, Int> {
                val metrics = g2d.fontMetrics
                val textWidth = metrics.stringWidth(text)
                val x = baseX - (textWidth / 2)
                val y = baseY + ((metrics.ascent - metrics.descent) / 2)
                return x to y
            }
        },
        LEFT {
            override fun calculateTextCoordinates(
                g2d: Graphics2D,
                text: String,
                baseX: Int,
                baseY: Int
            ): Pair<Int, Int> {
                val metrics = g2d.fontMetrics
                val y = baseY + ((metrics.ascent - metrics.descent) / 2)
                return baseX to y
            }
        },
        RIGHT {
            override fun calculateTextCoordinates(
                g2d: Graphics2D,
                text: String,
                baseX: Int,
                baseY: Int
            ): Pair<Int, Int> {
                val metrics = g2d.fontMetrics
                val textWidth = metrics.stringWidth(text)
                val x = baseX - textWidth
                val y = baseY + ((metrics.ascent - metrics.descent) / 2)
                return x to y
            }
        };

        abstract fun calculateTextCoordinates(g2d: Graphics2D, text: String, baseX: Int, baseY: Int): Pair<Int, Int>
    }
}