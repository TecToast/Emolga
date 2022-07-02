package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.draft.Draft
import de.tectoast.jsolf.JSONObject
import org.slf4j.LoggerFactory

class SkipCommand : Command(
    "skip",
    "Überspringt deinen Zug",
    CommandCategory.Draft,
    Constants.ASLID,
    Constants.FPLID,
    Constants.NDSID
) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        val memberr = e.member
        val member = memberr.idLong
        val d = Draft.getDraftByMember(member, tco)
            ?: //tco.sendMessage(member.getAsMention() + " Du bist in keinem Draft drin!").queue();
            return
        val league = emolgaJSON.getJSONObject("drafts").getJSONObject(d.name)
        if (d.tc.id != tco.id) return
        if (!d.isSwitchDraft) {
            e.reply("Dieser Draft ist kein Switch-Draft, daher wird !skip nicht unterstützt!")
            return
        }
        if (d.isNotCurrent(member)) {
            tco.sendMessage(d.getMention(member) + " Du bist nicht dran!").queue()
            return
        }
        //fplDoc(league, d);
        d.nextPlayer(tco, d.tierlist!!, league)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SkipCommand::class.java)
        private fun fplDoc(league: JSONObject, d: Draft) {
            val b = RequestBuilder(league.getString("sid"))
            b.addStrikethroughChange(
                league.getInt("draftorder"),
                d.round + 1,
                d.members.size - d.order[d.round]!!.size + 6,
                true
            )
            b.execute()
        }
    }
}