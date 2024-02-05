package de.tectoast.emolga.features.flo

import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.ButtonFeature
import de.tectoast.emolga.features.ButtonSpec
import de.tectoast.emolga.utils.Constants
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle

object GuildInviteButton : ButtonFeature<GuildInviteButton.Args>(::Args, ButtonSpec("guildinvite")) {
    override val buttonStyle = ButtonStyle.PRIMARY
    override val label = "Invite"
    override val emoji = Emoji.fromUnicode("✉️")

    class Args : Arguments() {
        var gid by long()
    }

    init {
        registerListener<GuildJoinEvent> { e ->
            e.jda.openPrivateChannelById(Constants.FLOID).flatMap {
                it.send(
                    "${e.guild.name} (${e.guild.id})",
                    components = GuildInviteButton { gid = e.guild.idLong }.into()
                )
            }.queue()
        }
    }

    context(InteractionData)
    override suspend fun exec(e: Args) {
        jda.getGuildById(e.gid)?.let { g ->
            reply(g.defaultChannel!!.createInvite().setMaxUses(1).await().url)
        } ?: reply("Invite failed, server not found")
    }
}
