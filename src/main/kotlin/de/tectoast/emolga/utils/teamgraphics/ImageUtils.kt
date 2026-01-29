package de.tectoast.emolga.utils.teamgraphics

import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.image.BufferedImage
import java.awt.image.ConvolveOp
import java.awt.image.Kernel
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.sqrt

object ImageUtils {
    /**
     * Creates a Gaussian Blur filter.
     * @param radius Controls the blur intensity. Higher is blurrier.
     */
    fun getGaussianBlurFilter(radius: Int): ConvolveOp {
        if (radius < 1) {
            throw IllegalArgumentException("Radius must be >= 1")
        }

        val size = radius * 2 + 1
        val data = FloatArray(size * size)

        val sigma = radius / 3.0f
        val twoSigmaSquare = 2.0f * sigma * sigma
        val sigmaRoot = (sqrt(twoSigmaSquare * Math.PI)).toFloat()
        var total = 0.0f

        for (y in -radius..radius) {
            for (x in -radius..radius) {
                val distance = (x * x + y * y).toFloat()
                val value = exp((-distance / twoSigmaSquare).toDouble()).toFloat() / sigmaRoot
                data[(y + radius) * size + (x + radius)] = value
                total += value
            }
        }

        // Normalize the kernel so it doesn't brighten or darken the image
        for (i in data.indices) {
            data[i] /= total
        }

        return ConvolveOp(Kernel(size, size, data), ConvolveOp.EDGE_NO_OP, null)
    }

    /**
     * Creates a new BufferedImage containing just the blurred shadow of the text.
     */
    fun createBlurredShadowImage(
        text: String,
        font: Font,
        shadowColor: Color,
        blurRadius: Int
    ): BufferedImage {
        // 1. Calculate dimensions needed for the text + padding for the blur
        val frc = FontRenderContext(null, true, true)
        val bounds = font.getStringBounds(text, frc)
        val padding = blurRadius * 2 // Extra space for the blur to spread
        val width = ceil(bounds.width).toInt() + padding * 2
        val height = ceil(bounds.height).toInt() + padding * 2

        // 2. Create a temporary image to draw the sharp shadow text
        val shadowImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g2d = shadowImage.createGraphics()

        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2d.font = font
        g2d.color = shadowColor
        // Draw text centered in the padded image
        g2d.drawString(text, padding, padding + abs(bounds.y).toInt())
        g2d.dispose()

        // 3. Apply the blur filter
        val blurOp = getGaussianBlurFilter(blurRadius)
        // The filter needs a destination image of the same size
        val blurredImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        return blurOp.filter(shadowImage, blurredImage)
    }

    fun drawTextWithSoftShadow(
        g2d: Graphics2D,
        text: String,
        centerX: Int,
        centerY: Int,
        font: Font, // Must pass the font object explicitly
        fontColor: Color,
        shadowColor: Color = Color(0, 0, 0, 180), // Semi-transparent black
        shadowOffset: Int = 5,
        blurRadius: Int = 6
    ) {
        // 1. Create the blurred shadow image
        val blurredShadow = createBlurredShadowImage(text, font, shadowColor, blurRadius)
        g2d.font = font
        val metrics = g2d.fontMetrics

        // 3. Calculate X: Target Center - Half Width
        val textWidth = metrics.stringWidth(text)
        val x = centerX - (textWidth / 2)

        // 4. Calculate Y: Target Center + Half Height Adjustment
        // Using (ascent - descent) / 2 aligns the "visual center" of the text (e.g. caps height)
        val y = centerY + ((metrics.ascent - metrics.descent) / 2)

        // 2. Calculate position to draw shadow (adjusting for padding from the blur step)
        // We subtract padding to align the shadow's "core" with the text position
        val padding = blurRadius * 2
        val shadowX = x + shadowOffset - padding
        val shadowY = y + shadowOffset - padding - font.size // Adjust y to account for baseline

        // 3. Draw the blurred image
        g2d.drawImage(blurredShadow, shadowX, shadowY, null)

        // 4. Draw the main text on top
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2d.font = font
        g2d.color = fontColor
        g2d.drawString(text, x, y)
    }

    fun cropToContent(source: BufferedImage): BufferedImage {
        val width = source.width
        val height = source.height

        var top = 0
        var bottom = height - 1
        var left = 0
        var right = width - 1

        // 1. Find Top
        top@ for (y in 0 until height) {
            for (x in 0 until width) {
                if ((source.getRGB(x, y) shr 24) != 0) {
                    top = y
                    break@top
                }
            }
            // If we reach the bottom without finding content, return empty
            if (y == height - 1) return BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        }

        // 2. Find Bottom
        bottom@ for (y in height - 1 downTo top) {
            for (x in 0 until width) {
                if ((source.getRGB(x, y) shr 24) != 0) {
                    bottom = y
                    break@bottom
                }
            }
        }

        // 3. Find Left (scan within top/bottom bounds only)
        left@ for (x in 0 until width) {
            for (y in top..bottom) {
                if ((source.getRGB(x, y) shr 24) != 0) {
                    left = x
                    break@left
                }
            }
        }

        // 4. Find Right (scan within top/bottom bounds only)
        right@ for (x in width - 1 downTo left) {
            for (y in top..bottom) {
                if ((source.getRGB(x, y) shr 24) != 0) {
                    right = x
                    break@right
                }
            }
        }

        return source.getSubimage(left, top, right - left + 1, bottom - top + 1)
    }
}