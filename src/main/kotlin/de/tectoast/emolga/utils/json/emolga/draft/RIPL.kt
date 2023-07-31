package de.tectoast.emolga.utils.json.emolga.draft

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("RIPL")
class RIPL : League() {
    override val teamsize = 12
    override fun checkUpdraft(specifiedTier: String, officialTier: String): String? {
        if (specifiedTier.startsWith("Mega") && officialTier != specifiedTier) return "Mega-Entwicklungen können nicht hochgedraftet werden!"
        return null
    }
}
