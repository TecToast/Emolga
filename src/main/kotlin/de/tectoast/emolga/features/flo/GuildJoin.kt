package de.tectoast.emolga.features.flo

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.ButtonFeature
import de.tectoast.emolga.features.ButtonSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.Constants
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle

object GuildJoin : ButtonFeature<GuildJoin.Args>(::Args, ButtonSpec("guildinvite")) {
    override val buttonStyle = ButtonStyle.PRIMARY
    override val label = "Invite"
    override val emoji = Emoji.fromUnicode("✉️")

    class Args : Arguments() {
        var gid by long()
    }

    init {
        registerListener<GuildJoinEvent> { e ->
            val g = e.guild
            e.jda.openPrivateChannelById(g.ownerIdLong).flatMap {
                it.sendMessage(
                    WELCOMEMESSAGE.replace("{USERNAME}", "<@${g.ownerIdLong}>").replace("{SERVERNAME}", g.name)
                )
            }.queue()
            e.jda.openPrivateChannelById(Constants.FLOID).flatMap {
                it.send(
                    "${e.guild.name} (${e.guild.id})",
                    components = GuildJoin { gid = e.guild.idLong }.into()
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

    private val WELCOMEMESSAGE: String = """
            Hallo **{USERNAME}** und vielen Dank, dass du mich auf deinen Server **{SERVERNAME}** geholt hast!
            Vermutlich möchtest du für deinen Server hauptsächlich, dass die Ergebnisse von Showdown Replays in einen Channel geschickt werden.
            Um mich zu konfigurieren gibt es folgende Möglichkeiten:

            **1. Die Ergebnisse sollen in den gleichen Channel geschickt werden:**
            Einfach `/replaychannel add` in den jeweiligen Channel schreiben

            **2. Die Ergebnisse sollen in einen anderen Channel geschickt werden:**
            `/replaychannel add #Ergebnischannel` in den Channel schicken, wo später die Replays reingeschickt werden sollen (Der #Ergebnischannel ist logischerweise der Channel, wo später die Ergebnisse reingeschickt werden sollen)       
            
            Wenn die Channel eingerichtet worden sind, muss man einfach /replay mit dem Replay-Link in einen Replay-Channel schicken und ich erledige den Rest.

            Falls die Ergebnisse in ||Spoilertags|| geschickt werden sollen, schick irgendwo auf dem Server den Command `/spoilertags` rein. Dies gilt dann serverweit.

            Falls du weitere Fragen oder Probleme hast, schreibe ${Constants.MYTAG} eine PN oder komme auf den Support-Server, dessen Link in meinem Profil steht :)
            
            _This message is written in German, because that is the only language I support at the moment. In the future, Emolga may be expanded to other languages._
        """.trimIndent()
}
