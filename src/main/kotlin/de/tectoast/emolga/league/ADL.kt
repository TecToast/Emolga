package de.tectoast.emolga.league

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("ADL")
class ADL : League() {
    override val teamsize = 10
}