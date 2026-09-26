package de.tectoast.emolga.domain.league.teamgraphic.model

import de.tectoast.emolga.utils.serializer.ColorSerializer
import kotlinx.serialization.Serializable
import java.awt.Color

@Serializable
sealed interface FontColorProvider {

    @Serializable
    data class Fixed(@Serializable(with = ColorSerializer::class) val color: Color) : FontColorProvider

    @Serializable
    data class FromOverlay(val xCoord: Int, val yCoord: Int) : FontColorProvider
}