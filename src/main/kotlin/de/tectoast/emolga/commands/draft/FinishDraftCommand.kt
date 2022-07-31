package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.emolga.draft.League

class FinishDraftCommand :
    Command("finishdraft", "Beendet für dich den Draft", CommandCategory.Draft, Constants.NDSID, Constants.ASLID) {
    init {
        aliases.add("finish")
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        val memberr = e.member
        val member = memberr.idLong
        val ev = DraftEvent(e)
        val d = League.byChannel(tco, member, ev) ?: return
        val mem = d.current
        if (e.guild.idLong == Constants.NDSID && d.picks[mem]!!.filter { it.name != "???" }.size < 15) {
            ev.reply("Du hast noch keine 15 Pokemon!")
            return
        }
        ev.reply("Du hast den Draft für dich beendet!")
        d.order.values.forEach { it.removeIf(mem::equals) }
        d.finished.add(mem)
        d.nextPlayer()
        saveEmolgaJSON()
    }
}