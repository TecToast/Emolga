package de.tectoast.emolga.selectmenus

import de.tectoast.emolga.commands.PrivateCommands
import de.tectoast.emolga.commands.indexedBy
import de.tectoast.emolga.utils.json.Emolga
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.editMessage
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent

class CSelectMenu : MenuListener("cselect") {
    override suspend fun process(e: StringSelectInteractionEvent, menuname: String?) {
        val (mode, uid) = menuname!!.split(":").let { it[0] to it[1].toLong() }
        val gid = e.guild?.idLong ?: return
        val data = Emolga.get.signups[gid] ?: return
        val user = data.users[uid]!!
        val oldConf = user.conference.indexedBy(data.conferences)
        user.conference = e.values.first()
        val newConf = user.conference.indexedBy(data.conferences)
        if (mode == "initial") {
            val nextuser = data.users.entries.firstOrNull { it.value.conference == null }?.key ?: run {
                val tc = e.jda.getTextChannelById(data.shiftChannel!!)!!
                data.shiftMessageIds = PrivateCommands.generateOrderingMessages(data).values.map {
                    tc.send(embeds = it.first.into(), components = it.second).await().idLong
                }
                return
            }
            e.editMessageEmbeds(Embed(title = "Einteilung", description = "<@$nextuser>"))
                .setActionRow(data.conferenceSelectMenus(nextuser, true)).queue()
        } else {
            e.deferEdit().queue()
            e.hook.deleteOriginal().queue()
            PrivateCommands.generateOrderingMessages(data, oldConf, newConf).forEach {
                e.channel.editMessage(
                    data.shiftMessageIds[it.key].toString(),
                    embeds = it.value.first.into(),
                    components = it.value.second
                ).queue()
            }
        }
    }
}