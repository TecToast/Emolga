package de.tectoast.emolga.features.interaction

import de.tectoast.emolga.discord.K18nMessageSender
import de.tectoast.emolga.discord.MessageSender
import de.tectoast.emolga.utils.Constants

fun InteractionData.toMessageSender(ephemeral: Boolean) = MessageSender { reply(ephemeral = ephemeral, it) }
fun InteractionData.toK18nMessageSender(ephemeral: Boolean) = K18nMessageSender { reply(it, ephemeral = ephemeral) }

val InteractionData.validationCompleteCallback
    get() = suspend {
        replyRaw(Constants.CHECKMARK, ephemeral = true)
    }
