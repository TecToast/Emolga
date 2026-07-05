package de.tectoast.emolga.features.league.draft

import de.tectoast.emolga.domain.league.draft.model.permission.DraftMention
import de.tectoast.emolga.domain.league.draft.service.permission.DraftPermissionManagementService
import de.tectoast.emolga.domain.league.member.model.LeagueParticipant
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.NoArgs
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.isError
import de.tectoast.emolga.utils.t
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.into
import net.dv8tion.jda.api.entities.MessageEmbed
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class DraftPermissionCommand(allow: Allow, deny: Deny) :
    CommandFeature<NoArgs>(NoArgs(), CommandSpec("draftpermission", K18n_DraftPermission.Help)) {

    override val children = listOf(allow, deny)

    @Single
    class Allow(
        private val service: DraftPermissionManagementService
    ) :
        CommandFeature<Allow.Args>(::Args, CommandSpec("allow", K18n_DraftPermission.AllowHelp)) {
        class Args : Arguments() {
            var user by member("User", K18n_DraftPermission.AllowArgUser)
            var withmention by enumAdvanced<DraftMention>("Ping", K18n_DraftPermission.AllowArgMention)
        }


        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            val mem = e.user
            if (mem.user.isBot) return iData.reply(K18n_DraftPermission.NoBotsAllowed)
            val result = service.addUser(iData.gid, iData.user, mem.idLong, e.withmention)
            if (result.isError()) {
                return iData.reply(result.message, ephemeral = true)
            }
            return iData.replyRaw(embeds = result.value.toEmbed(), ephemeral = true)
        }
    }

    @Single
    class Deny(
        private val service: DraftPermissionManagementService
    ) :
        CommandFeature<Deny.Args>(::Args, CommandSpec("deny", K18n_DraftPermission.DenyHelp)) {
        class Args : Arguments() {
            var user by member("User", K18n_DraftPermission.DenyArgUser)
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            val mem = e.user
            val result = service.removeUser(iData.gid, iData.user, mem.idLong)
            if (result.isError()) {
                return iData.reply(result.message, ephemeral = true)
            }
            iData.replyRaw(
                embeds = result.value.toEmbed(), ephemeral = true
            )
        }
    }


    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
    }
}

context(iData: InteractionData)
private fun List<LeagueParticipant>.toEmbed(): List<MessageEmbed> {
    return Embed(title = K18n_DraftPermission.EmbedTitle.t(), color = Constants.EMBED_COLOR) {
        description = joinToString("\n") {
            (if (it.shouldPing) K18n_DraftPermission.EmbedYes(it.userId) else K18n_DraftPermission.EmbedNo(it.userId)).t()
        }
    }.into()
}



