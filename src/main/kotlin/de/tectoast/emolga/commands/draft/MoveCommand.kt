package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.emolga.draft.League
import dev.minn.jda.ktx.coroutines.await

object MoveCommand : Command(
    "move",
    "Verschiebt deinen Pick",
    CommandCategory.Draft,
    Constants.G.ASL,
    Constants.G.CULT,
    821350264152784896L
) {
    init {
        aliases.add("verschieben")
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(true, Constants.G.ASL)
    }

    override suspend fun process(e: GuildCommandEvent) {
        League.byCommand(e)?.let {
            if (it.isSwitchDraft) {
                e.reply("Dieser Draft ist ein Switch-Draft, daher wird /move nicht unterstützt!")
                return
            }
            if (it.isLastRound) {
                e.reply("Der Draft befindet sich bereits in Runde ${it.round}, somit kann der Pick nicht mehr verschoben werden!")
                return
            }
            it.triggerMove()
            e.slashCommandEvent!!.reply("Du hast deinen Pick verschoben!").await()
            it.nextPlayer()
        }
        //ndsdoc(tierlist, pokemon, d, mem, tier, round);
    }
}
