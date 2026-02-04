package de.tectoast.emolga.utils.teamgraphics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.awt.*
import java.awt.geom.Ellipse2D
import java.io.File
import javax.imageio.ImageIO

@Serializable
sealed interface TeamGraphicStyle {
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

data class IndexDataStyle(val xInFinal: Int, val yInFinal: Int, val shape: Shape)

@Serializable
@SerialName("GDL")
data class GDLStyle(val conference: String) : TeamGraphicStyle {
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

    @Transient
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

    @Transient
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

data class DrawData(val name: String, val x: Int, val y: Int, val size: Int, val flipped: Boolean)

@Serializable
@SerialName("ABL")
data object ABLStyle : TeamGraphicStyle {
    private val coordMap = mapOf(
        0 to Pair(938, 169),
        1 to Pair(1159, 118),
        2 to Pair(1376, 118),
        3 to Pair(1595, 168),
        4 to Pair(1101, 352),
        5 to Pair(1443, 351),
        6 to Pair(1098, 570),
        7 to Pair(1444, 570),
        8 to Pair(938, 791),
        9 to Pair(1270, 781),
        10 to Pair(1601, 793)
    )
    override fun getDataForIndex(
        index: Int,
        data: DrawData
    ): IndexDataStyle {
        val (x, y) = coordMap[index] ?: error("No coordinates for index $index")
        val scaled = shape.scaled(data.size.toDouble() / sizeOfShape)
        return IndexDataStyle(x, y, scaled)
    }

    val shape by lazy {
        val shape = Polygon()
        shape.addPoint(94, 0)
        shape.addPoint(0, 67)
        shape.addPoint(36, 172)
        shape.addPoint(153, 172)
        shape.addPoint(190, 67)
        shape
    }

    override val backgroundPath = "teamgraphics/layers/ABL_Grafiken.png"
    override val overlayPath = null
    override val playerText =
        TeamGraphicStyle.TextProperties(
            fontPath = "BasementGrotesque-Black_v1.202.otf",
            fontColor = Color.WHITE,
            fontSize = 32f,
            xCoord = 1480,
            yCoord = 31,
            orientation = TeamGraphicStyle.TextAlignment.RIGHT,
            maxSize = null,
            shadow = null
        )
    override val teamnameText =
        TeamGraphicStyle.TextProperties(
            fontPath = "BasementGrotesque-Black_v1.202.otf",
            fontColor = Color.WHITE,
            fontSize = 32f,
            xCoord = 425,
            yCoord = 1057,
            orientation = TeamGraphicStyle.TextAlignment.LEFT,
            maxSize = null,
            shadow = null
        )
    override val sizeOfShape: Int = 192
    override val logoProperties =
        TeamGraphicStyle.LogoProperties(192, 217, 580, 580, "/home/florian/Pictures/sans.png")
    override val guild: Long = 977969587448602654

    fun Shape.scaled(scale: Double): Shape {
        val bounds = this.bounds2D
        val scaleX = scale * bounds.width / this.bounds.width
        val scaleY = scale * bounds.height / this.bounds.height

        val transform = java.awt.geom.AffineTransform.getScaleInstance(scaleX, scaleY)
        return transform.createTransformedShape(this)
    }
}
