package de.tectoast.emolga.utils.json.emolga.customcommand

import kotlinx.serialization.Serializable

@Suppress("unused")
@Serializable
data class CCData(var image: String? = null, var text: String? = null)