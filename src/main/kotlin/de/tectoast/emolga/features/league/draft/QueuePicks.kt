package de.tectoast.emolga.features.league.draft

import de.tectoast.emolga.database.exposed.*
import de.tectoast.emolga.database.league.LeagueQueryService
import de.tectoast.emolga.database.league.QueuePicksService
import de.tectoast.emolga.database.league.byCommand
import de.tectoast.emolga.database.league.checkQueueEnabled
import de.tectoast.emolga.features.*
import de.tectoast.emolga.features.league.draft.generic.K18n_NoLeagueForGuildFound
import de.tectoast.emolga.features.league.draft.generic.K18n_NotInTierlist
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.json.ErrorOrNull
import de.tectoast.emolga.utils.json.mdb
import de.tectoast.emolga.utils.json.msg
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.emoji.Emoji
import org.koin.core.annotation.Single
import org.koin.core.component.get
import kotlin.collections.any

object QueuePicks {


    @Single(binds = [ListenerProvider::class])
    class Command(
        manage: Manage, add: Add, enable: Enable, disable: Disable
    ) : CommandFeature<NoArgs>(
        NoArgs(), CommandSpec(
            "queuepicks", K18n_QueuePicks.Help
        )
    ) {
        override val children = listOf(manage, add, enable, disable)

        @Single(binds = [ListenerProvider::class])
        class Manage(
            val stateStore: StateStoreDispatcher,
            val leagueQueryService: LeagueQueryService,
            val queuedPicksRepository: QueuedPicksRepository
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
                stateStore.process<_, QueuePicksStateHandler>(
                    QueuePicksState(leagueName, currentData), iData.user
                ) {
                    with(get<QueuePicksComponents>()) {
                        init()
                    }
                }
            }
        }

        @Single(binds = [ListenerProvider::class])
        class Add(
            val leagueQueryService: LeagueQueryService,
            val queuePicksService: QueuePicksService,
        ) :
            CommandFeature<Add.Args>(::Args, CommandSpec("add", K18n_QueuePicks.AddHelp)) {
            class Args : Arguments() {
                var mon by draftPokemon("Pokemon", K18n_QueuePicks.AddArgPokemon)
                var oldmon by draftPokemon(
                    "Old Mon", K18n_QueuePicks.AddArgOldMon, autocomplete = { s, event ->
                        val gid = event.guild?.idLong
                        val league = mdb.leagueByGuild(gid ?: -1, event.user.idLong) ?: return@draftPokemon listOf(
                            K18n_NoLeagueForGuildFound.translateToGuildLanguage(gid)
                        )
                        monOfTeam(s, league, league(event.user.idLong))
                    }).nullable()
                // TODO: Autocomplete
                var tier by string("Tier", K18n_Pick.ArgTier).nullable()
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
                    e.mon.showdownId,
                    e.oldmon?.showdownId,
                    e.tier,
                )
                iData.reply(result.msg())
            }
        }

        @Single(binds = [ListenerProvider::class])
        class Enable(val helper: QueueActivationHelper) : CommandFeature<NoArgs>(NoArgs(), CommandSpec("enable", K18n_QueuePicks.EnableHelp)) {
            context(iData: InteractionData)
            override suspend fun exec(e: NoArgs) {
                helper.changeActivation(true)
            }
        }

        @Single(binds = [ListenerProvider::class])
        class Disable(val helper: QueueActivationHelper) : CommandFeature<NoArgs>(NoArgs(), CommandSpec("disable", K18n_QueuePicks.DisableHelp)) {
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

    @Single(binds = [ListenerProvider::class])
    class Menu(val stateStore: StateStoreDispatcher) :
        SelectMenuFeature<Menu.Args>(::Args, SelectMenuSpec("queuepicks")) {
        class Args : Arguments() {
            var mon by singleOption()
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            stateStore.process<_, QueuePicksStateHandler>(iData.user) {
                with(get<QueuePicksComponents>()) {
                    handleSelect(e.mon)
                }
            }
        }
    }

    @Single(binds = [ListenerProvider::class])
    class ControlButton(val stateStore: StateStoreDispatcher) :
        ButtonFeature<ControlButton.Args>(::Args, ButtonSpec("queuepickscontrol")) {
        enum class ControlMode {
            UP, DOWN, REMOVE, CANCEL, MODAL
        }

        class Args : Arguments() {
            var mon by string()
            var controlMode by enumBasic<ControlMode>()
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            stateStore.process<_, QueuePicksStateHandler>(iData.user) {
                with(get<QueuePicksComponents>()) {
                    handleButton(e)
                }
            }
        }
    }

    @Single(binds = [ListenerProvider::class])
    class FinishButton(val stateStore: StateStoreDispatcher) :
        ButtonFeature<FinishButton.Args>(::Args, ButtonSpec("queuepicksfinish")) {

        class Args : Arguments() {
            var enable by boolean()
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            stateStore.process<_, QueuePicksStateHandler>(iData.user) {
                with(get<QueuePicksComponents>()) {
                    finish(e.enable)
                }
            }
        }
    }

    @Single(binds = [ListenerProvider::class])
    class ReloadButton(val stateStore: StateStoreDispatcher) :
        ButtonFeature<NoArgs>(NoArgs(), ButtonSpec("queuepicksreload")) {
        override val buttonStyle = ButtonStyle.PRIMARY
        override val label = K18n_QueuePicks.ReloadLabel
        override val emoji = Emoji.fromUnicode("\uD83D\uDD04")

        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            stateStore.process<_, QueuePicksStateHandler>(iData.user) {
                with(get<QueuePicksComponents>()) {
                    reload()
                }
            }
        }
    }

    @Single(binds = [ListenerProvider::class])
    class SetLocationModal(val stateStore: StateStoreDispatcher) :
        ModalFeature<SetLocationModal.Args>(::Args, ModalSpec("queuepickslocation")) {
        override val title = K18n_QueuePicks.SetLocationModalTitle

        class Args : Arguments() {
            var mon by string().compIdOnly()
            var location by string<Int>("Position") {
                validate { it.toIntOrNull() }
                modal(placeholder = K18n_QueuePicks.SetLocationModalArgLocation)
            }
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            stateStore.process<_, QueuePicksStateHandler>(iData.user) {
                with(get<QueuePicksComponents>()) {
                    setLocation(e.mon, e.location)
                }
            }
        }
    }


}

class QueueActivationHelper(val leagueQueryService: LeagueQueryService, val queuePicksService: QueuePicksService) {
    context(iData: InteractionData)
    suspend fun changeActivation(enable: Boolean) {
        val (leagueName, config, idx) = leagueQueryService.byCommand() ?: return iData.reply(K18n_NoLeagueForGuildFound)
        val result = queuePicksService.changeActivation(enable, iData.gid, leagueName, idx, config)
        iData.reply(result.msg())
    }
}

@Single
class QueuePicksComponents(
    val btn: QueuePicks.ControlButton,
    val menu: QueuePicks.Menu,
    val finishBtn: QueuePicks.FinishButton,
    val reloadBtn: QueuePicks.ReloadButton,
    val locationModal: QueuePicks.SetLocationModal
)
