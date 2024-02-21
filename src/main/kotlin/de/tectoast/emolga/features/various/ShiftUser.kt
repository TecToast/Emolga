package de.tectoast.emolga.features.various

import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.indexedBy
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.editMessage
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send

object ShiftUser {
    object Button : ButtonFeature<Button.Args>(::Args, ButtonSpec("shiftuser")) {
        class Args : Arguments() {
            var uid by long()
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            val gid = PrivateCommands.guildForTLSetup ?: gid
            val uid = e.uid
            with(
                db.signups.get(gid)
                    ?: return reply("Diese Anmeldung ist bereits geschlossen!", ephemeral = true)
            ) {
                if (extended) {
                    return reply(
                        embeds = Embed(title = "Shift", description = "<@$uid>").into(),
                        components = conferenceSelectMenus(uid, false).into()
                    )
                }
                deferEdit()
                val tc = jda.getTextChannelById(shiftChannel!!)!!
                val user = users[uid]!!
                val currConf = user.conference!!
                user.conference = conferences[(conferences.indexOf(currConf) + 1) % conferences.size]
                PrivateCommands.generateOrderingMessages(this).forEach { (index, pair) ->
                    tc.editMessage(
                        shiftMessageIds[index].toString(),
                        embeds = pair.first.into(),
                        components = pair.second
                    )
                        .queue()
                }
                save()
            }
        }
    }

    object SelectMenu : SelectMenuFeature<SelectMenu.Args>(::Args, SelectMenuSpec("cselect")) {
        class Args : Arguments() {
            var mode by enumBasic<Mode>().compIdOnly()
            var uid by long().compIdOnly()
            var selection by singleOption()
        }

        enum class Mode {
            INITIAL, UPDATE;

            companion object {
                fun fromBoolean(initial: Boolean) = if (initial) INITIAL else UPDATE
            }
        }

        context(InteractionData) override suspend fun exec(e: Args) {
            val mode = e.mode
            val uid = e.uid
            val gid = PrivateCommands.guildForTLSetup ?: gid
            val data = db.signups.get(gid) ?: return
            val user = data.users[uid]!!
            val oldConf = user.conference.indexedBy(data.conferences)
            user.conference = e.selection
            val newConf = user.conference.indexedBy(data.conferences)
            data.save()
            when (mode) {
                Mode.INITIAL -> {
                    val nextuser = data.users.entries.firstOrNull { it.value.conference == null }?.key ?: run {
                        val tc = jda.getTextChannelById(data.shiftChannel!!)!!
                        data.shiftMessageIds = PrivateCommands.generateOrderingMessages(data).values.map {
                            tc.send(embeds = it.first.into(), components = it.second).await().idLong
                        }
                        return
                    }
                    edit(
                        embeds = Embed(title = "Einteilung", description = "<@$nextuser>").into(),
                        components = data.conferenceSelectMenus(nextuser, true).into()
                    )
                }

                Mode.UPDATE -> {
                    deferEdit()
                    hook.deleteOriginal().queue()
                    PrivateCommands.generateOrderingMessages(data, oldConf, newConf).forEach {
                        textChannel.editMessage(
                            data.shiftMessageIds[it.key].toString(),
                            embeds = it.value.first.into(),
                            components = it.value.second
                        ).queue()
                    }
                }
            }
        }
    }
}
