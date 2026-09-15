package de.tectoast.emolga.domain.league.teamgraphic.model

import de.tectoast.emolga.utils.serializer.ColorSerializer
import kotlinx.serialization.Serializable
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.io.File


@Serializable
data class TeamGraphicStyle(
    val guild: Long,
    val shapeInfo: TeamGraphicShapeInfo,
    val backgroundPathTemplate: String,
    val overlayPathTemplate: String? = null,
    val dataForIndex: Map<Int, CanvasCoordinate>,
    val individualBackgrounds: Boolean,
    val playerText: TextProperties? = null,
    val teamnameText: TextProperties? = null,
    val logoProperties: LogoProperties? = null,
    val userNameSettings: TeamGraphicUserNameSettings = TeamGraphicUserNameSettings()
) {

    fun backgroundPath(leagueName: String? = null, idx: Int? = null): String {
        return backgroundPathTemplate.applyTemplate(leagueName, idx)
    }

    fun overlayPath(leagueName: String? = null, idx: Int? = null): String? {
        return overlayPathTemplate?.applyTemplate(leagueName, idx)
    }

    private fun String.applyTemplate(leagueName: String? = null, idx: Int? = null): String {
        return replace("{leagueName}", leagueName ?: "")
            .replace("{idx}", idx?.toString() ?: "")
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
        @Serializable(with = ColorSerializer::class) val fontColor: Color,
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