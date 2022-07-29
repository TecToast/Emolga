package de.tectoast.emolga.utils.json

import de.tectoast.emolga.utils.json.emolga.Soullink
import de.tectoast.emolga.utils.json.emolga.customcommand.CCData
import de.tectoast.emolga.utils.json.emolga.draft.League
import kotlinx.serialization.Serializable

@Suppress("unused")
@Serializable
class Emolga(
    val soullink: Soullink,
    val drafts: Map<String, League>,
    val emolgachannel: MutableMap<String, MutableList<Long>>,
    val cooldowns: MutableMap<String, MutableMap<String, Long>>,
    val moderatorRoles: Map<String, Long>,
    val movefilter: Map<String, List<String>>,
    val mutedRoles: Map<String, Long>,
    val customCommands: Map<String, CCData>,
    val configuration: MutableMap<String, MutableMap<String, MutableMap<String, Long>>>
) {
    companion object {
        lateinit var get: Emolga
    }
}