package de.tectoast.emolga.commands.draft.during

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.commands.NoCommandArgs
import de.tectoast.emolga.commands.TestableCommand
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.NextPlayerData
import de.tectoast.emolga.utils.json.emolga.draft.SkipReason

object MoveCommand : TestableCommand<NoCommandArgs>(
    "move",
    "Verschiebt deinen Pick",
) {
    init {
        aliases.add("verschieben")
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(true, Constants.G.ASL, Constants.G.COMMUNITY)
    }

    context (InteractionData)
    override suspend fun exec(e: NoCommandArgs) {
        League.byCommand()?.first?.let {
            if (it.isSwitchDraft) {
                return reply("Dieser Draft ist ein Switch-Draft, daher wird /move nicht unterstützt!")
            }
            if (it.pseudoEnd) {
                return reply("Der Draft ist quasi schon vorbei, du kannst jetzt nicht mehr moven!")
            }
            it.triggerMove()
            replyAwait("Du hast deinen Pick verschoben!")
            it.afterPickOfficial(NextPlayerData.Moved(SkipReason.SKIP))
        }
        //ndsdoc(tierlist, pokemon, d, mem, tier, round);
    }

    override fun fromGuildCommandEvent(e: GuildCommandEvent) = NoCommandArgs
}
