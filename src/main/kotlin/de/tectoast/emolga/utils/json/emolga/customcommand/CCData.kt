package de.tectoast.emolga.utils.json.emolga.customcommand

import kotlinx.serialization.Serializable

@Serializable
data class CCData(var image: String? = null, var text: String? = null)