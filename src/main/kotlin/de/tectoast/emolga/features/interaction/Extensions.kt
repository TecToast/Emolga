package de.tectoast.emolga.features.interaction

import de.tectoast.emolga.discord.K18nMessageSender
import de.tectoast.emolga.discord.MessageSender
import de.tectoast.emolga.domain.league.draft.repository.DraftAdminRepository
import de.tectoast.emolga.utils.Constants

fun InteractionData.toMessageSender(ephemeral: Boolean) = MessageSender { replyAwait(ephemeral = ephemeral, it) }
fun InteractionData.toK18nMessageSender(ephemeral: Boolean) =
    K18nMessageSender { replyAwait(it, ephemeral = ephemeral) }

val InteractionData.validationCompleteCallback
    get() = suspend {
        replyRaw(Constants.CHECKMARK, ephemeral = true)
    }

suspend fun InteractionData.isDraftAdmin(repo: DraftAdminRepository) = repo.isAdmin(gid, user, data.memberRoles)
