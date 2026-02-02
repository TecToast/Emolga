package de.tectoast.emolga.utils.teamgraphics

import java.awt.*
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

    companion object {
        fun fromLeagueName(leagueName: String): TeamGraphicStyle {
            if (leagueName.startsWith("GDL")) {
                val conference = Regex("GDLS\\d+(.*)").matchEntire(leagueName)!!.groupValues[1]
                return GDLStyle(conference)
            }
            error("No TeamGraphicStyle found for league name: $leagueName")
        }
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

data object ABLStyle : TeamGraphicStyle {
    override fun getDataForIndex(
        index: Int,
        data: DrawData
    ): IndexDataStyle {
        var x: Int
        var y: Int
        when {
            index % 8 in 1..2 -> {
                val modX = index % 8 - 1
                val modY = index / 8
                x = 1159 + modX * 217
                y = 118 + modY * 670
                if (index == 9) {
                    x += 2
                    y += 1
                }
                if (index == 10) {
                    x += 6
                    y += 1
                }
            }

            setOf(0, 3, 8, 11).contains(index) -> {
                val modX = index % 8 / 3
                val modY = index / 8
                x = 938 + modX * 658
                y = 169 + modY * 600
                if (index == 3) {
                    x -= 1
                    y -= 2
                }
                if (index == 8) {
                    x += 2
                    y -= 2
                }
                if (index == 11) {
                    x += 3
                    y -= 7
                }
            }

            else -> {
                val modX = index % 2
                val modY = (index - 4) / 2
                x = 1101 + modX * 342
                y = 352 + modY * 220
                if (index in 4..5) {
                    y -= 1
                }
                if (index == 6) {
                    x -= 3
                    y -= 2
                }
                if (index == 7) {
                    x += 1
                    y -= 3
                }
            }
        }
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
