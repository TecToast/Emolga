package de.tectoast.emolga.features.league.draft.queue

import de.tectoast.emolga.domain.league.config.model.LeagueConfig
import de.tectoast.emolga.domain.league.queue.repository.QueuedPicksRepository
import de.tectoast.emolga.domain.league.queue.service.QueuePicksService
import de.tectoast.emolga.domain.league.tierlist.repository.TierlistRepository
import de.tectoast.emolga.domain.league.util.autocomplete.TierAutocompleteService
import de.tectoast.emolga.domain.league.util.service.LeagueQueryService
import de.tectoast.emolga.domain.statestore.model.QueuePicksComponents
import de.tectoast.emolga.domain.statestore.model.QueuePicksState
import de.tectoast.emolga.domain.statestore.service.QueuePicksStateStoreHandler
import de.tectoast.emolga.domain.statestore.service.StateStoreDispatcher
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.league.draft.K18n_Pick
import de.tectoast.emolga.features.league.draft.K18n_QueuePicks
import de.tectoast.emolga.features.league.draft.generic.K18n_NoLeagueForGuildFound
import de.tectoast.emolga.features.league.draft.generic.K18n_NoTierlist
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.NoArgs
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.ErrorOrNull
import de.tectoast.emolga.utils.msg
import org.koin.core.annotation.Single
import org.koin.core.component.get
import org.koin.core.component.inject

@Single(binds = [ListenerProvider::class])
class QueuePicksCommand(
    manage: Manage, add: Add, enable: Enable, disable: Disable
) : CommandFeature<NoArgs>(
    NoArgs(), CommandSpec(
        "queuepicks", K18n_QueuePicks.Help
    )
) {
    override val children = listOf(manage, add, enable, disable)

    @Single(binds = [ListenerProvider::class])
    class Manage(
        private val stateStore: StateStoreDispatcher,
        private val leagueQueryService: LeagueQueryService,
        private val queuedPicksRepository: QueuedPicksRepository,
        private val tierlistRepository: TierlistRepository,
    ) :
        CommandFeature<NoArgs>(NoArgs(), CommandSpec("manage", K18n_QueuePicks.Help)) {
        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            iData.ephemeralDefault()
            val (leagueName, config, idx) = leagueQueryService.byCommand()
                ?: return iData.reply(K18n_NoLeagueForGuildFound)
            config.checkQueueEnabled()?.let { return iData.reply(it) }
            val currentData = queuedPicksRepository.getSingle(leagueName, idx)
            val currentState = currentData.queued
            if (currentState.isEmpty()) return iData.reply(
                K18n_QueuePicks.NoPicksInQueue, ephemeral = true
            )
            val guild = iData.gid
            val tl = tierlistRepository.getMeta(guild, config.tlIdentifier) ?: return iData.reply(K18n_NoTierlist)
            stateStore.process<_, QueuePicksStateStoreHandler, _>(
                QueuePicksState(leagueName, guild, tl.language, idx, currentData), iData.user
            ) {
                with(get<QueuePicksComponents>()) {
                    init()
                }
            }
        }

        private fun LeagueConfig.checkQueueEnabled(): ErrorOrNull {
            return if (!triggers.queuePicks) {
                K18n_QueuePicks.Disabled
            } else null
        }
    }

    @Single(binds = [ListenerProvider::class])
    class Add(
        private val leagueQueryService: LeagueQueryService,
        private val queuePicksService: QueuePicksService,
    ) :
        CommandFeature<Add.Args>(::Args, CommandSpec("add", K18n_QueuePicks.AddHelp)) {
        class Args : Arguments() {

            private val tierAutocompleteService: TierAutocompleteService by inject()

            var mon by draftPokemon("Pokemon", K18n_QueuePicks.AddArgPokemon)
            var oldmon by draftPokemon(
                "Old Mon", K18n_QueuePicks.AddArgOldMon, autocomplete = { query, event ->
                    val gid = event.guild?.idLong ?: return@draftPokemon null
                    monOfTeam(query, gid, event.channelIdLong, event.user.idLong)
                }).nullable()

            var tier by string("Tier", K18n_Pick.ArgTier) {
                slashCommand { query, event ->
                    tierAutocompleteService.autocompleteTier(query, event.channelIdLong, event.user.idLong)
                }
            }.nullable()
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            iData.ephemeralDefault()
            iData.deferReply()
            val (leagueName, config, idx) = leagueQueryService.byCommand() ?: return iData.reply(
                K18n_NoLeagueForGuildFound
            )
            val result = queuePicksService.addAction(
                iData.gid,
                leagueName,
                config,
                idx,
                iData.user,
                e.mon,
                e.oldmon,
                e.tier,
            )
            iData.reply(result.msg())
        }
    }

    @Single(binds = [ListenerProvider::class])
    class Enable(private val helper: QueueActivationHelper) :
        CommandFeature<NoArgs>(NoArgs(), CommandSpec("enable", K18n_QueuePicks.EnableHelp)) {
        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            helper.changeActivation(true)
        }
    }

    @Single(binds = [ListenerProvider::class])
    class Disable(private val helper: QueueActivationHelper) :
        CommandFeature<NoArgs>(NoArgs(), CommandSpec("disable", K18n_QueuePicks.DisableHelp)) {
        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            helper.changeActivation(false)
        }
    }

    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        // do nothing
    }
}