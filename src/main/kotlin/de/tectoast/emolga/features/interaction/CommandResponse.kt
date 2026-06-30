package de.tectoast.emolga.features.interaction

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageEditData


class CommandResponse(
    val ephemeral: Boolean = false,
    val msgCreateData: MessageCreateData? = null,
    val msgEditData: MessageEditData? = null
) {

    val msg: String? get() = msgCreateData?.content

    fun sendInto(callback: IReplyCallback) {
        callback.reply(msgCreateData!!).setEphemeral(ephemeral).queue()
    }

    fun sendInto(hook: InteractionHook) {
        hook.sendMessage(msgCreateData!!).setEphemeral(ephemeral).queue()
    }

    suspend fun sendIntoAwait(callback: IReplyCallback) {
        callback.reply(msgCreateData!!).setEphemeral(ephemeral).await()
    }

    suspend fun sendIntoAwait(hook: InteractionHook) {
        hook.sendMessage(msgCreateData!!).setEphemeral(ephemeral).await()
    }

    fun editInto(callback: IMessageEditCallback) {
        callback.editMessage(msgEditData!!).queue()
    }

    fun editInto(hook: InteractionHook) {
        hook.editOriginal(msgEditData!!).queue()
    }

}