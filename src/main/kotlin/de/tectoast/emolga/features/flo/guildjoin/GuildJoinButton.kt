package de.tectoast.emolga.features.flo.guildjoin

import de.tectoast.emolga.discord.GuildInviteCreator
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.ButtonSpec
import de.tectoast.emolga.features.system.types.ButtonFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.k18n
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.emoji.Emoji
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class GuildJoinButton(private val adminSupportRepo: GuildInviteCreator) :
    ButtonFeature<GuildJoinButton.Args>(::Args, ButtonSpec("guildinvite")) {
    override val buttonStyle = ButtonStyle.PRIMARY
    override val label = "Invite".k18n
    override val emoji = Emoji.fromUnicode("✉️")

    class Args : Arguments() {
        var gid by long()
    }


    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.replyRaw(adminSupportRepo.createOneTimeInvite(e.gid) ?: "Invite failed")
    }
}