package de.tectoast.emolga.features.flo

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.Constants
import dev.minn.jda.ktx.messages.Mentions
import dev.minn.jda.ktx.messages.SendDefaults
import dev.minn.jda.ktx.messages.send
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.utils.FileUpload

object SendFeatures {
    class Args : Arguments() {
        var id by long("id", "id")
        var msg by string("msg", "msg")
    }

    private fun String.convertForSend() = this.replace("\\n", "\n")

    object SendPNCommand : CommandFeature<Args>(::Args, CommandSpec("sendpn", "Sendet eine PN an einen User")) {
        init {
            restrict(flo)
        }

        context(iData: InteractionData) override suspend fun exec(e: Args) {
            jda.openPrivateChannelById(e.id).flatMap { it.sendMessage(e.msg.convertForSend()) }.queue()
            iData.done(true)
        }

    }

    object SendTCCommand : CommandFeature<Args>(::Args, CommandSpec("sendtc", "Sendet eine Nachricht in einen TC")) {
        init {
            restrict(flo)
        }

        context(iData: InteractionData) override suspend fun exec(e: Args) {
            jda.getTextChannelById(e.id)!!.sendMessage(e.msg.convertForSend()).queue()
            iData.done(true)
        }

    }

    fun sendToMe(msg: String) {
        if (msg.isNotBlank()) {
            sendToUser(Constants.FLOID, msg)
        }
    }

    fun sendToUser(
        id: Long,
        content: String = SendDefaults.content,
        embeds: Collection<MessageEmbed> = SendDefaults.embeds,
        components: Collection<MessageTopLevelComponent> = SendDefaults.components,
        files: Collection<FileUpload> = emptyList(),
        tts: Boolean = false,
        mentions: Mentions = Mentions.default(),
    ) {
        val jda = jda
        jda.openPrivateChannelById(id).flatMap { pc ->
            pc.send(
                content = content.take(2000),
                embeds = embeds,
                components = components,
                files = files,
                tts = tts,
                mentions = mentions
            )
        }.queue()
    }
}
