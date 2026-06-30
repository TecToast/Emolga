package de.tectoast.emolga.domain.league.teamgraphic.model

import kotlinx.serialization.Serializable
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.io.File

@Serializable
sealed interface TeamGraphicStyle {
    fun getDataForIndex(index: Int, data: DrawData): IndexDataStyle
    val sizeOfShape: Int
    val playerText: TextProperties?
    val teamnameText: TextProperties?
    val logoProperties: LogoProperties?
    val guild: Long
    val individualBackgrounds: Boolean get() = false
    fun backgroundPath(league: String, idx: Int): String
    fun overlayPath(league: String, idx: Int): String?

    fun transformUsername(username: String) = username


    data class LogoProperties(
        val startX: Int,
        val startY: Int,
        val width: Int,
        val height: Int,
        val defaultLogoPath: String?
    )

    data class TextProperties(
        val fontPath: String,
        val fontColor: Color,
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

    data class TextShadowProperties(
        val color: Color,
        val offset: Int,
        val blurRadius: Int
    )

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