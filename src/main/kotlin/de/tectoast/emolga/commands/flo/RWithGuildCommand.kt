package de.tectoast.emolga.commands.flo

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal

class RWithGuildCommand : Command("rwithguild", "Replay mit Guild", CommandCategory.Flo) {

    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            add("id", "ID", "Die ID", ArgumentManagerTemplate.DiscordType.ID)
            add("url", "Replay-Link", "Der Replay-Link", ArgumentManagerTemplate.Text.any())
            setExample("/rwithguild ${Constants.G.MY} https://replay.pokemonshowdown.com/oumonotype-82345404")
        }
        slash(true)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val url = e.arguments.getText("url")
        if (url == "-") {
            e.slashCommandEvent!!.replyModal(
                Modal.create("rwithguild", "Replays mit Guild")
                    .addComponents(
                        ActionRow.of(TextInput.create("id", "ID", TextInputStyle.SHORT).build()),
                        ActionRow.of(TextInput.create("urls", "Replay-Links", TextInputStyle.PARAGRAPH).build())
                    ).build()
            ).queue()
            return
        }
        analyseReplay(
            url,
            resultchannelParam = e.textChannel,
            customGuild = e.arguments.getID("id"),
            fromAnalyseCommand = e.run { deferReply(); slashCommandEvent?.hook })
    }

}
