package de.tectoast.emolga.commands.flo

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal

object MultiReplayCommand : Command("multireplay", "MultiReplay", CommandCategory.Flo) {

    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            add("replay", "ReplayChannel", "lol", ArgumentManagerTemplate.DiscordType.ID)
            add("result", "ResultChannel", "lol", ArgumentManagerTemplate.DiscordType.ID)
            setExample("/multireplayy 12345 23456")
        }
        slash(true)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val replay = e.arguments.getID("replay")
        val result = e.arguments.getID("result")
        e.slashCommandEvent!!.replyModal(
            Modal.create("multireplay;$replay:$result", "Multi-Replay")
                .addComponents(
                    ActionRow.of(TextInput.create("urls", "Replay-Links", TextInputStyle.PARAGRAPH).build())
                ).build()
        ).queue()
    }

}
