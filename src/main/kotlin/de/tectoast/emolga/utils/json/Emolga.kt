package de.tectoast.emolga.utils.json

import de.tectoast.emolga.utils.json.emolga.ASLS11
import de.tectoast.emolga.utils.json.emolga.Soullink
import de.tectoast.emolga.utils.json.emolga.customcommand.CCData
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.NDS
import kotlinx.serialization.Serializable

@Serializable
class Emolga(
    val asls11: ASLS11,
    val soullink: Soullink,
    val drafts: Map<String, League>,
    val emolgachannel: MutableMap<Long, MutableList<Long>>,
    val cooldowns: MutableMap<Long, MutableMap<String, Long>>,
    val moderatorroles: Map<Long, Long>,
    val movefilter: Map<String, List<String>>,
    val mutedroles: Map<Long, Long>,
    val customcommands: MutableMap<String, CCData>,
    val configuration: MutableMap<Long, MutableMap<String, MutableMap<String, Long>>>,
    val asls11nametoid: List<Long>
) {
    fun league(name: String) = drafts[name]!!
    fun nds() = drafts["NDS"]!! as NDS

    fun leagueByGuild(gid: Long) = drafts.values.firstOrNull { it.guild == gid }

    companion object {
        lateinit var get: Emolga
    }
}