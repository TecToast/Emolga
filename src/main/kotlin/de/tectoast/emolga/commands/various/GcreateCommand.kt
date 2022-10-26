package de.tectoast.emolga.commands.various

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants.G
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal

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
        e.slashCommandEvent!!.replyModal(
            Modal.create("gcreate", "Giveaway-Erstellung")
                .apply {
                    addActionRow(
                        TextInput.create("time", "Dauer des Giveaways", TextInputStyle.SHORT)
                            .setPlaceholder("5h")
                            .setRequired(true)
                            .setRequiredRange(1, 20)
                            .build()
                    )
                    addActionRow(
                        TextInput.create("winners", "Anzahl der Gewinner", TextInputStyle.SHORT)
                            .setPlaceholder("1")
                            .setRequired(true)
                            .setRequiredRange(1, 2)
                            .build()
                    )
                    addActionRow(
                        TextInput.create("prize", "Preis", TextInputStyle.PARAGRAPH)
                            .setPlaceholder("Kekse")
                            .setRequired(true)
                            .setRequiredRange(1, 100)
                            .build()
                    )
                }.build()
        )
    }
}
