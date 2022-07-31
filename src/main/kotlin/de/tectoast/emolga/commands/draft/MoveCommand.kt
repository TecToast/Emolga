package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.emolga.draft.League

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
        e.textChannel
        /*if(d.round == tierlist.rounds) {
            e.reply("Der Draft befindet sich bereits in Runde " + d.round + ", somit kann der Pick nicht mehr verschoben werden!");
            return;
        }*/
        League.byChannel(e.textChannel, member, DraftEvent(e))?.nextPlayer()
        //ndsdoc(tierlist, pokemon, d, mem, tier, round);
    }
}