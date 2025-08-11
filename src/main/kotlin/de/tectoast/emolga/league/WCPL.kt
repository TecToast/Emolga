package de.tectoast.emolga.league

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("WCPL")
class WCPL : League() {
    override val teamsize = 6
}