package de.tectoast.emolga.utils.json.emolga

import kotlinx.serialization.Serializable

@Serializable
class Soullink(val order: MutableList<String>, val mons: MutableMap<String, MutableMap<String, String>>)