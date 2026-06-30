package de.tectoast.emolga.features.interaction

import de.tectoast.emolga.utils.t
import de.tectoast.k18n.generated.K18N_DEFAULT_LANGUAGE
import de.tectoast.k18n.generated.K18nLanguage
import de.tectoast.k18n.generated.K18nMessage
import dev.minn.jda.ktx.messages.*
import kotlinx.coroutines.CompletableDeferred
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.modals.Modal
import net.dv8tion.jda.api.utils.AttachedFile
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageEditData

abstract class InteractionData(
    open val user: Long,
    open var tc: Long,
    open val gid: Long,
    open val data: AdditionalDeliveredData = AdditionalDeliveredData(),
    open val language: K18nLanguage = K18N_DEFAULT_LANGUAGE,
) {
    abstract val hook: InteractionDataHook

    val acknowledged: CompletableDeferred<Unit> = CompletableDeferred()
    val responseDeferred: CompletableDeferred<CommandResponse> = CompletableDeferred()
    var deferred = false

    val replied get() = responseDeferred.isCompleted


    private var ephemeralDefault = false

    abstract fun reply(
        ephemeral: Boolean = ephemeralDefault,
        msgCreateData: MessageCreateData
    )

    abstract suspend fun replyAwait(ephemeral: Boolean = ephemeralDefault, msgCreateData: MessageCreateData)
    abstract fun replyModal(modal: Modal)
    abstract fun edit(
        msgEditData: MessageEditData
    )

    fun done(ephemeral: Boolean = false) = replyRaw("Done!", ephemeral = ephemeral)


    fun ephemeralDefault() {
        ephemeralDefault = true
    }

    fun replyRaw(
        content: String = SendDefaults.content,
        embeds: Collection<MessageEmbed> = SendDefaults.embeds,
        components: Collection<MessageTopLevelComponent> = SendDefaults.components,
        files: Collection<FileUpload> = emptyList(),
        tts: Boolean = false,
        mentions: Mentions = Mentions.default(),
        ephemeral: Boolean = ephemeralDefault,
    ) = reply(
        ephemeral, MessageCreate(
            content = content,
            embeds = embeds,
            files = files,
            components = components,
            tts = tts,
            mentions = mentions
        )
    )

    fun reply(
        content: K18nMessage,
        embeds: Collection<MessageEmbed> = SendDefaults.embeds,
        components: Collection<MessageTopLevelComponent> = SendDefaults.components,
        files: Collection<FileUpload> = emptyList(),
        tts: Boolean = false,
        mentions: Mentions = Mentions.default(),
        ephemeral: Boolean = ephemeralDefault,
    ) = reply(
        ephemeral, MessageCreate(
            content = content.t(),
            embeds = embeds,
            files = files,
            components = components,
            tts = tts,
            mentions = mentions
        )
    )

    fun editRaw(
        content: String? = null,
        embeds: Collection<MessageEmbed>? = null,
        components: Collection<MessageTopLevelComponent>? = null,
        attachments: Collection<AttachedFile>? = null,
        replace: Boolean = MessageEditDefaults.replace,
    ) = edit(
        MessageEdit(
            content = content,
            embeds = embeds,
            files = attachments,
            components = components,
            replace = replace
        )
    )

    fun edit(
        contentK18n: K18nMessage? = null,
        embeds: Collection<MessageEmbed>? = null,
        components: Collection<MessageTopLevelComponent>? = null,
        attachments: Collection<AttachedFile>? = null,
        replace: Boolean = MessageEditDefaults.replace,
    ) = edit(
        MessageEdit(
            content = contentK18n?.t(),
            embeds = embeds,
            files = attachments,
            components = components,
            replace = replace
        )
    )

    abstract fun deferReply(ephemeral: Boolean = ephemeralDefault)
    abstract fun deferEdit()

    protected fun markAcknowledged() {
        acknowledged.complete(Unit)
    }
}