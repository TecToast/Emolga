package de.tectoast.emolga.features.league.draft


import de.tectoast.emolga.domain.league.config.repository.LeagueConfigRepository
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.draft.model.core.PickInput
import de.tectoast.emolga.domain.league.draft.service.core.DraftService
import de.tectoast.emolga.domain.league.tierlist.model.UpdraftConfig
import de.tectoast.emolga.domain.league.tierlist.model.config.FreePickTierlistConfig
import de.tectoast.emolga.domain.league.tierlist.model.config.TierBasedTierlistConfig
import de.tectoast.emolga.domain.league.tierlist.repository.TierlistRepository
import de.tectoast.emolga.domain.league.util.autocomplete.TierAutocompleteService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.interaction.validationCompleteCallback
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.model.ArgumentPresence
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.BotConstants
import de.tectoast.emolga.utils.isError
import org.koin.core.annotation.Single
import org.koin.core.component.inject

@Single(binds = [ListenerProvider::class])
class PickCommand(private val draftService: DraftService) :
    CommandFeature<PickCommand.Args>(PickCommand::Args, CommandSpec("pick", K18n_Pick.Help)) {

    class Args : Arguments() {
        val tierlistRepo: TierlistRepository by inject()
        val leagueConfigRepo: LeagueConfigRepository by inject()
        val leagueCoreRepo: LeagueCoreRepository by inject()
        private val tierAutocompleteService: TierAutocompleteService by inject()
        private val botConstants: BotConstants by inject()

        var pokemon by draftPokemon("pokemon", K18n_Pick.ArgPokemon)
        var tier by string("tier", K18n_Pick.ArgTier) {
            slashCommand(guildChecker = { gid ->
                if (gid == botConstants.botOwnerGuildId) return@slashCommand ArgumentPresence.OPTIONAL
                if (tierlistRepo.getAllMetasForGuild(gid)
                        .any { meta -> meta.config is TierBasedTierlistConfig && meta.config.updraftConfig != UpdraftConfig.Disabled }
                ) ArgumentPresence.OPTIONAL
                else ArgumentPresence.NOT_PRESENT
            }, autocomplete = { query, event ->
                tierAutocompleteService.autocompleteTier(query, event.channelIdLong, event.user.idLong)
            })
        }.nullable()
        var free by boolean("free", K18n_Pick.ArgFree) {
            default = false
            slashCommand(guildChecker = { gid ->
                if (gid == botConstants.botOwnerGuildId) ArgumentPresence.OPTIONAL
                else when (tierlistRepo.getAllMetasForGuild(gid)
                    .any { meta -> meta.config is FreePickTierlistConfig }) {
                    true -> ArgumentPresence.OPTIONAL
                    else -> ArgumentPresence.NOT_PRESENT
                }
            })
        }
        var tera by boolean("tera", K18n_Pick.ArgTera) {
            default = false
            slashCommand(guildChecker = { gid ->
                if (gid == botConstants.botOwnerGuildId) return@slashCommand ArgumentPresence.OPTIONAL
                val leagueNames = leagueCoreRepo.getLeagueNamesByGuild(gid)
                if (leagueNames.any { leagueConfigRepo.getConfig(it).teraPick != null }) ArgumentPresence.OPTIONAL
                else ArgumentPresence.NOT_PRESENT
            })
        }
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.deferReply(ephemeral = true)
        val result = draftService.handleInputRequest(
            PickInput(e.pokemon, e.tier, e.free, e.tera),
            iData.tc,
            iData.user,
            iData.data.memberRoles,
            iData.validationCompleteCallback
        )
        if (result.isError()) {
            iData.reply(result.message, ephemeral = true)
        }
    }

}
