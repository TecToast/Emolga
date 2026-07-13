package de.tectoast.emolga.discord.jda.features

import de.tectoast.emolga.features.interaction.AdditionalDeliveredData
import de.tectoast.emolga.features.interaction.CommandResponse
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.debug.TestOverride
import de.tectoast.k18n.generated.K18N_DEFAULT_LANGUAGE
import de.tectoast.k18n.generated.K18nLanguage
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.callbacks.IDeferrableCallback
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.modals.Modal
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageEditData
import org.koin.mp.KoinPlatformTools

class JDAInteractionData(
    val e: GenericInteractionCreateEvent, override val language: K18nLanguage = K18N_DEFAULT_LANGUAGE
) : InteractionData(
    user = testOverride.user ?: e.user.idLong,
    tc = testOverride.tc ?: e.channel!!.idLong,
    gid = testOverride.gid ?: e.guild?.idLong ?: -1,
    data = AdditionalDeliveredData(memberRoles = e.member?.roles?.map { it.idLong } ?: emptyList(),
        messageId = (e as? GenericComponentInteractionCreateEvent)?.message?.idLong
            ?: (e as? ModalInteractionEvent)?.message?.idLong),

    ) {

    override val hook by lazy { JDAInteractionDataHook((e as IDeferrableCallback).hook) }

    override fun reply(ephemeral: Boolean, msgCreateData: MessageCreateData) {
        e as IReplyCallback
        markAcknowledged()
        val response = CommandResponse(ephemeral, msgCreateData)
        if (deferred || replied)
            response.sendInto(e.hook)
        else response.sendInto(e)
        responseDeferred.complete(response)
    }

    override fun edit(msgEditData: MessageEditData) {
        e as IMessageEditCallback
        markAcknowledged()
        val response = CommandResponse(msgEditData = msgEditData)
        if (deferred || replied)
            response.editInto(e.hook)
        else response.editInto(e)
        responseDeferred.complete(response)
    }

    override fun replyModal(modal: Modal) {
        e as IModalCallback
        markAcknowledged()
        responseDeferred.complete(CommandResponse())
        e.replyModal(modal).queue()
    }

    override fun deferReply(ephemeral: Boolean) {
        if (deferred) return
        e as IReplyCallback
        deferred = true
        markAcknowledged()
        e.deferReply(ephemeral).queue()
    }

    override fun deferEdit() {
        if (deferred) return
        e as IMessageEditCallback
        deferred = true
        markAcknowledged()
        e.deferEdit().queue()
    }

    override suspend fun replyAwait(ephemeral: Boolean, msgCreateData: MessageCreateData) {
        e as IReplyCallback
        val response = CommandResponse(ephemeral, msgCreateData)
        markAcknowledged()
        if (deferred || replied)
            response.sendIntoAwait(e.hook)
        else response.sendIntoAwait(e)
        responseDeferred.complete(response)
    }
}

private val testOverride: TestOverride by KoinPlatformTools.defaultContext().get().inject()