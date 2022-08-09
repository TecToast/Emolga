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
        slash(true, Constants.ASLID)
    }

    override suspend fun process(e: GuildCommandEvent) {
        League.byChannel(e)?.let {
            if (it.isLastRound) {
                e.reply("Der Draft befindet sich bereits in Runde ${it.round}, somit kann der Pick nicht mehr verschoben werden!")
                return
            }
            it.nextPlayer()
        }
        //ndsdoc(tierlist, pokemon, d, mem, tier, round);
    }
}