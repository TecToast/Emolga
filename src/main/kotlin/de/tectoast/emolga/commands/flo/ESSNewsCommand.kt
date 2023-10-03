package de.tectoast.emolga.commands.flo

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal

object ESSNewsCommand : Command("essnews", "ESS News", CommandCategory.Flo) {

    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(true, 1030585128696680538)
        setCustomPermissions(PermissionPreset.fromRole(1030585814800932984))
    }

    override suspend fun process(e: GuildCommandEvent) {
        e.slashCommandEvent!!.replyModal(
            Modal.create("essnews", "ESS News").addComponents(
                listOf(
                    ActionRow.of(TextInput.create("title", "Titel", TextInputStyle.SHORT).apply {
                        isRequired = true
                        minLength = 1
                        maxLength = 50
                    }.build()),
                    ActionRow.of(TextInput.create("text", "Text", TextInputStyle.PARAGRAPH).apply {
                        isRequired = true
                        minLength = 1
                        maxLength = 300
                    }.build())
                )
            )
                .build()
        ).queue()
    }
}
