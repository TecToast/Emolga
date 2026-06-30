package de.tectoast.emolga.features.system.types

import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.MessageContextArgs
import de.tectoast.emolga.features.system.MessageContextSpec
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent

abstract class MessageContextFeature(spec: MessageContextSpec) :
    Feature<MessageContextSpec, MessageContextInteractionEvent, MessageContextArgs>(
        ::MessageContextArgs, spec, MessageContextInteractionEvent::class, eventToName
    ) {
    override suspend fun populateArgs(
        data: InteractionData, e: MessageContextInteractionEvent, args: MessageContextArgs
    ) {
        args.message = e.target
    }

    companion object {
        val eventToName: (MessageContextInteractionEvent) -> String = { it.interaction.name }
    }
}