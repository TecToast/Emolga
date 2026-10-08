package de.tectoast.emolga.features.league.draft

import de.tectoast.emolga.domain.league.draft.model.permission.DraftMention
import de.tectoast.emolga.domain.league.draft.repository.DraftAdminRepository
import de.tectoast.emolga.domain.league.draft.service.permission.DraftPermissionManagementService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.interaction.isDraftAdmin
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.NoArgs
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.isError
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class SubstituteDrafterCommand(set: Set, remove: Remove) :
    CommandFeature<NoArgs>(NoArgs(), CommandSpec("substitutedrafter", K18n_SubstituteDrafter.Help)) {

    override val children = listOf(set, remove)

    @Single
    class Set(
        private val service: DraftPermissionManagementService,
        private val draftAdminRepo: DraftAdminRepository,
    ) :
        CommandFeature<Set.Args>(::Args, CommandSpec("set", K18n_SubstituteDrafter.SetHelp)) {
        class Args : Arguments() {
            var user by member("User", K18n_SubstituteDrafter.SetArgUser)
            var substitute by member("Substitute", K18n_SubstituteDrafter.SetArgSubstitute)
        }

        init {
            restrict { admin(this) || isDraftAdmin(draftAdminRepo) }
        }


        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            val mem = e.substitute
            if (mem.user.isBot) return iData.reply(K18n_DraftPermission.NoBotsAllowed)
            val result = service.addUser(iData.gid, e.user.idLong, mem.idLong, DraftMention.OTHER)
            if (result.isError()) {
                return iData.reply(result.message, ephemeral = true)
            }
            return iData.replyRaw(embeds = result.value.toEmbed(K18n_SubstituteDrafter.EmbedTitle), ephemeral = true)
        }
    }

    @Single
    class Remove(
        private val service: DraftPermissionManagementService,
        private val draftAdminRepo: DraftAdminRepository,
    ) :
        CommandFeature<Remove.Args>(::Args, CommandSpec("remove", K18n_SubstituteDrafter.RemoveHelp)) {
        class Args : Arguments() {
            var user by member("User", K18n_SubstituteDrafter.RemoveArgUser)
            var substitute by member("Substitute", K18n_SubstituteDrafter.RemoveArgSubstitute)
        }

        init {
            restrict { admin(this) || isDraftAdmin(draftAdminRepo) }
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            val result = service.removeUser(iData.gid, e.user.idLong, e.substitute.idLong)
            if (result.isError()) {
                return iData.reply(result.message, ephemeral = true)
            }
            iData.replyRaw(
                embeds = result.value.toEmbed(K18n_SubstituteDrafter.EmbedTitle), ephemeral = true
            )
        }
    }


    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
    }
}



