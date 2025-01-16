package de.tectoast.emolga.utils.json.emolga.draft

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Wooloo")
class Wooloo : League() {
    override val teamsize = 12
}