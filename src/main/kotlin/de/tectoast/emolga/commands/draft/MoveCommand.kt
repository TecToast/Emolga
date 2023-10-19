package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.emolga.draft.League
import dev.minn.jda.ktx.coroutines.await
import mu.KotlinLogging

object MoveCommand : Command(
    "move",
    "Verschiebt deinen Pick",
    CommandCategory.Draft,
    Constants.G.ASL,
    Constants.G.CULT,
    821350264152784896L,
    Constants.G.COMMUNITY
) {
    init {
        aliases.add("verschieben")
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(true, Constants.G.ASL, Constants.G.COMMUNITY)
    }

    private val logger = KotlinLogging.logger {}
    override suspend fun process(e: GuildCommandEvent) {
        logger.info("MoveCommand by ${e.author.name}")
        League.byCommand(e)?.let {
            if (it.isSwitchDraft) {
                return e.reply("Dieser Draft ist ein Switch-Draft, daher wird /move nicht unterstützt!")
            }
            if (it.pseudoEnd) {
                return e.reply("Der Draft ist quasi schon vorbei, du kannst jetzt nicht mehr moven!")
            }
            it.triggerMove()
            e.slashCommandEvent!!.reply("Du hast deinen Pick verschoben!").await()
            it.afterPickOfficial()
        }
        //ndsdoc(tierlist, pokemon, d, mem, tier, round);
    }
}
