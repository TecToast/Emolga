package de.tectoast.emolga.commands

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class PrivateCommandEvent : GenericCommandEvent {
    constructor(e: SlashCommandInteractionEvent) : super(e)
    constructor(e: Message) : super(e)
}
