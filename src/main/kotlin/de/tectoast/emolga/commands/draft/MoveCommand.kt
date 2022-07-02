package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.draft.Draft
import de.tectoast.jsolf.JSONObject

class MoveCommand : Command(
    "move",
    "Verschiebt deinen Pick",
    CommandCategory.Draft,
    Constants.ASLID,
    Constants.CULTID,
    821350264152784896L
) {
    init {
        aliases.add("verschieben")
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        val memberr = e.member
        val member = memberr.idLong
        val tco = e.textChannel
        val d = Draft.getDraftByMember(member, tco)
        if (d == null) {
            tco.sendMessage(memberr.asMention + " Du bist in keinem Draft drin!").queue()
            return
        }
        if (d.tc.id != tco.id) return
        if (d.isNotCurrent(member)) {
            tco.sendMessage(d.getMention(member) + " Du bist nicht dran!").queue()
            return
        }
        val mem = d.current
        val tierlist = d.tierlist
        /*if(d.round == tierlist.rounds) {
            e.reply("Der Draft befindet sich bereits in Runde " + d.round + ", somit kann der Pick nicht mehr verschoben werden!");
            return;
        }*/
        val league = d.league
        if (!league.has("skippedturns")) league.put("skippedturns", JSONObject())
        val st = league.getJSONObject("skippedturns")
        st.put(mem, st.createOrGetArray(mem).put(d.round))
        d.nextPlayer(tco, tierlist!!, league)
        //ndsdoc(tierlist, pokemon, d, mem, tier, round);
    }
}