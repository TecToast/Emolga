package de.tectoast.emolga.commands.various

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants.G
import dev.minn.jda.ktx.interactions.components.Modal

class GcreateCommand : Command(
    "gcreate",
    "Startet ein Giveaway",
    CommandCategory.Various,
    G.ASL,
    G.GENSHINEMPIRES,
    G.CULT,
    G.FPL,
    G.FLP,
    G.NDS
) {

    //GuildMessageReceivedEvent event, TextChannel tchan, int seconds, int winners
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slashInAllowedGuilds(true)
    }

    override suspend fun process(e: GuildCommandEvent) {
        e.slashCommandEvent!!.replyModal(Modal("gcreate", "Giveaway-Erstellung") {
            short(
                id = "time",
                label = "Dauer des Giveaways",
                required = true,
                placeholder = "5h",
                requiredLength = 1..20
            )
            short(id = "winners", label = "Anzahl der Gewinner", required = true, value = "1", requiredLength = 1..2)
            paragraph(id = "prize", label = "Preis", required = true, placeholder = "Kekse", requiredLength = 1..100)
        }).queue()
    }
}