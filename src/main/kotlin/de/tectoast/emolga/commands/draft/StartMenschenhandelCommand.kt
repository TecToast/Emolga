package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.db

class StartMenschenhandelCommand : Command(
    "startmenschenhandel",
    "Startet die beste Sache einer Coach-Season",
    CommandCategory.Draft,
    Constants.G.ASL
) {

    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("channel", "Channel", "Der Channel lol", ArgumentManagerTemplate.DiscordType.CHANNEL)
            .setExample("!startmenschenhandel #wasweißichdenn")
            .build()
        setCustomPermissions { it.isOwner }
    }

    override suspend fun process(e: GuildCommandEvent) {
        val tc = e.arguments.getChannel("channel")
        db.asls11.apply {
            textChannel = tc.idLong
            tc.sendMessage("Möge der Menschenhandel beginnen!").queue()
            nextCoach()
            // TODO
        }

    }

}
