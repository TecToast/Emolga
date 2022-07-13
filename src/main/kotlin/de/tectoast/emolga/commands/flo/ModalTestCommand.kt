package de.tectoast.emolga.commands.flo

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.Modal
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle

class ModalTestCommand : Command("modaltest", "Testet Modals", CommandCategory.Flo) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(true)
    }

    override fun process(e: GuildCommandEvent) {
        val email = TextInput.create("email", "Email", TextInputStyle.SHORT)
            .setPlaceholder("Enter your E-mail")
            .setMinLength(10)
            .setMaxLength(100) // or setRequiredRange(10, 100)
            .build()
        val body = TextInput.create("body", "Body", TextInputStyle.PARAGRAPH)
            .setPlaceholder("Your concerns go here")
            .setMinLength(30)
            .setMaxLength(1000)
            .build()
        val modal = Modal.create("support", "Support")
            .addActionRows(ActionRow.of(email), ActionRow.of(body))
            .build()
        e.slashCommandEvent!!.replyModal(modal).queue()
    }
}