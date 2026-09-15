package de.tectoast.emolga.domain.league.teamgraphic.model

import kotlinx.serialization.Serializable
import java.awt.Shape
import java.awt.geom.Ellipse2D

@Serializable
sealed interface TeamGraphicShapeInfo {
    val size: Int
    fun provideShape(spriteSize: Int): Shape

    @Serializable
    data class Circle(override val size: Int) : TeamGraphicShapeInfo {
        override fun provideShape(spriteSize: Int): Shape {
            return Ellipse2D.Float(0f, 0f, spriteSize.toFloat(), spriteSize.toFloat())
        }
    }

    @Serializable
    data class Polygon(override val size: Int, val points: List<CanvasCoordinate>) : TeamGraphicShapeInfo {
        val polygon by lazy {
            val polygon = java.awt.Polygon()
            points.forEach { polygon.addPoint(it.x, it.y) }
            polygon
        }

        override fun provideShape(spriteSize: Int): Shape {
            return polygon.scaled(spriteSize.toDouble() / size.toDouble())
        }

        private fun Shape.scaled(scale: Double): Shape {
            val bounds = this.bounds2D
            val scaleX = scale * bounds.width / this.bounds.width
            val scaleY = scale * bounds.height / this.bounds.height

            val transform = java.awt.geom.AffineTransform.getScaleInstance(scaleX, scaleY)
            return transform.createTransformedShape(this)
        }
    }
}