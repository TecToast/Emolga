package de.tectoast.emolga.utils.json.emolga.draft

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Default")
class Default : League() {
    override val teamsize = 69
}
