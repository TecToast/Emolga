package de.tectoast.emolga.utils.teamgraphics

import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.Shape
import java.awt.geom.Ellipse2D
import java.io.File
import javax.imageio.ImageIO

interface TeamGraphicStyle {
    fun getDataForIndex(index: Int, data: DrawData): IndexDataStyle
    val backgroundPath: String
    val overlayPath: String?
    val sizeOfShape: Int
    val playerText: TextProperties?
    val teamnameText: TextProperties?
    val logoProperties: LogoProperties?
    val guild: Long

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
            val ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
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
        };

        abstract fun calculateTextCoordinates(g2d: Graphics2D, text: String, baseX: Int, baseY: Int): Pair<Int, Int>
    }
}

data class IndexDataStyle(val xInFinal: Int, val yInFinal: Int, val shape: Shape)

class GDLStyle(conference: String) : TeamGraphicStyle {
    override fun getDataForIndex(
        index: Int,
        data: DrawData
    ): IndexDataStyle {
        val x: Int
        val y: Int
        if (index % 7 < 4) {
            x = STARTXTOPBOTTOM + (index % 7) * DISTANCE
            y = if (index < 4) STARTYTOP else STARTYBOTTOM
        } else {
            x = STARTXMIDDLE + (index - 4) * DISTANCE
            y = STARTYMIDDLE
        }
        return IndexDataStyle(x, y, Ellipse2D.Float(0f, 0f, data.size.toFloat(), data.size.toFloat()))
    }

    override val overlayPath = "/teamgraphics/GDL/$conference.png"
    override val backgroundPath = "/teamgraphics/GDL/Universe.png"
    override val sizeOfShape = SIZE_OF_CIRCLE
    override val playerText = TeamGraphicStyle.TextProperties(
        fontPath = "/teamgraphics/GDL/MASQUE.ttf",
        fontColor = run {
            val layer = ImageIO.read(File(overlayPath))
            Color(layer.getRGB(1, 800))
        },
        fontSize = 72f,
        xCoord = 300,
        yCoord = 884,
        orientation = TeamGraphicStyle.TextAlignment.CENTERED,
        maxSize = 560,
        shadow = TeamGraphicStyle.TextShadowProperties(Color(0, 0, 0, 255), 5, 5)
    )
    override val teamnameText = null
    override val logoProperties =
        TeamGraphicStyle.LogoProperties(10, 106, 580, 580, "/teamgraphics/GDL/defaultlogo.png")
    override val guild = 716942575852060682

    override fun transformUsername(username: String): String {
        return username.replace(Regex("\\d+$"), "").replace("0", "O").replace("|", "I").replace("/", "I")
    }

    companion object {
        const val DISTANCE = 316
        const val SIZE_OF_CIRCLE = 281
        const val STARTXTOPBOTTOM = 666
        const val STARTYTOP = 55
        const val STARTYMIDDLE = 400
        const val STARTYBOTTOM = 745
        const val STARTXMIDDLE = 832
    }
}

data class DrawData(val name: String, val x: Int, val y: Int, val size: Int)