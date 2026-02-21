package de.tectoast.emolga.features.league.draft

import de.tectoast.emolga.features.*
import de.tectoast.emolga.features.league.draft.generic.K18n_NoLeagueForGuildFound
import de.tectoast.emolga.features.league.draft.generic.K18n_NotInTierlist
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.config.QueuePicksUserData
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.QueuePicks
import de.tectoast.emolga.utils.json.ErrorOrNull
import de.tectoast.emolga.utils.json.mdb
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.emoji.Emoji

object QueuePicks {
    context(iData: InteractionData)
    fun League.queueNotEnabled(): Boolean {
        if (!config.triggers.queuePicks) {
            iData.reply(K18n_QueuePicks.Disabled)
            return true
        } else return false
    }

    object Command : CommandFeature<NoArgs>(
        NoArgs(), CommandSpec(
            "queuepicks", K18n_QueuePicks.Help
        )
    ) {

        object Manage : CommandFeature<NoArgs>(NoArgs(), CommandSpec("manage", K18n_QueuePicks.Help)) {
            context(iData: InteractionData)
            override suspend fun exec(e: NoArgs) {
                iData.ephemeralDefault()
                val league = mdb.leagueByCommand() ?: return iData.reply(K18n_NoLeagueForGuildFound)
                if (league.queueNotEnabled()) return
                val currentData =
                    league.persistentData.queuePicks.queuedPicks.getOrPut(league(iData.user)) { QueuePicksUserData() }
                val currentState = currentData.queued
                if (currentState.isEmpty()) return iData.reply(
                    K18n_QueuePicks.NoPicksInQueue,
                    ephemeral = true
                )
                QueuePicks(
                    iData.user, league.leaguename, currentData
                ).process {
                    init()
                }
            }
        }

        object Add : CommandFeature<Add.Args>(::Args, CommandSpec("add", K18n_QueuePicks.AddHelp)) {
            class Args : Arguments() {
                var mon by draftPokemon("Pokemon", K18n_QueuePicks.AddArgPokemon)
                var oldmon by draftPokemon(
                    "Old Mon",
                    K18n_QueuePicks.AddArgOldMon,
                    autocomplete = { s, event ->
                        val gid = event.guild?.idLong
                        val league = mdb.leagueByGuild(gid ?: -1, event.user.idLong)
                            ?: return@draftPokemon listOf(K18n_NoLeagueForGuildFound.translateToGuildLanguage(gid))
                        monOfTeam(s, league, league(event.user.idLong))
                    }).nullable()
            }

            context(iData: InteractionData)
            override suspend fun exec(e: Args) {
                iData.ephemeralDefault()
                iData.deferReply()
                League.executeOnFreshLock(
                    { mdb.leagueByCommand() },
                    { iData.reply(K18n_NoLeagueForGuildFound) }) l@{
                    if (queueNotEnabled()) return@l
                    val oldmon = e.oldmon
                    val idx = this(iData.user)
                    if (oldmon == null && !isRunning && picks.isNotEmpty() && !config.triggers.allowPickDuringSwitch) {
                        return@l iData.reply(K18n_QueuePicks.OnlyWithSwitchAllowed)
                    }
                    if (oldmon != null && picks[idx]?.any { it.name == oldmon.official } != true) {
                        return@l iData.reply(K18n_QueuePicks.PokemonNotInTeam(oldmon.tlName))
                    }
                    if (isPicked(e.mon.official)) {
                        return@l iData.reply(K18n_QueuePicks.PokemonAlreadyPicked(e.mon.tlName))
                    }
                    val data = persistentData.queuePicks.queuedPicks.getOrPut(idx) { QueuePicksUserData() }
                    val mon = e.mon
                    for (q in data.queued) {
                        if (q.g == mon) return@l iData.reply(K18n_QueuePicks.PokemonInYourQueue(mon.tlName))
                        if (oldmon != null && q.y == oldmon) return@l iData.reply(
                            K18n_QueuePicks.OldMonInYourQueue(
                                oldmon.tlName
                            )
                        )
                    }
                    tierlist.getTierOf(mon.tlName) ?: return@l iData.reply(K18n_NotInTierlist(mon.tlName))
                    oldmon?.let {
                        tierlist.getTierOf(it.tlName)
                            ?: return@l iData.reply(K18n_NotInTierlist(it.tlName))
                    }
                    val queuedAction = QueuedAction(mon, oldmon)
                    val newlist = data.queued.toMutableList().apply { add(queuedAction) }
                    isIllegal(idx, newlist)?.let { return@l iData.reply(it) }
                    StateStore.processIgnoreMissing<QueuePicks> {
                        addNewMon(queuedAction)
                    }
                    data.queued = newlist
                    data.enabled = false
                    save()
                    iData.reply(
                        K18n_QueuePicks.AddSuccess("`${mon.tlName}`".notNullPrepend(oldmon) { "`${it.tlName}` -> " })
                    )
                }

            }
        }

        context(iData: InteractionData)
        suspend fun changeActivation(enable: Boolean) {
            League.executeOnFreshLock(
                { mdb.leagueByCommand() },
                { iData.reply(K18n_NoLeagueForGuildFound) }) l@{
                if (queueNotEnabled()) return@l
                val idx = this(iData.user)
                val data = persistentData.queuePicks.queuedPicks.getOrPut(idx) { QueuePicksUserData() }
                isIllegal(idx, data.queued)?.let { return@l iData.reply(it) }
                data.enabled = enable
                save()
            }
            iData.reply(if (enable) K18n_QueuePicks.QueueEnabled else K18n_QueuePicks.QueueDisabled, ephemeral = true)
        }

        object Enable : CommandFeature<NoArgs>(NoArgs(), CommandSpec("enable", K18n_QueuePicks.EnableHelp)) {
            context(iData: InteractionData)
            override suspend fun exec(e: NoArgs) {
                changeActivation(true)
            }
        }

        object Disable : CommandFeature<NoArgs>(NoArgs(), CommandSpec("disable", K18n_QueuePicks.DisableHelp)) {
            context(iData: InteractionData)
            override suspend fun exec(e: NoArgs) {
                changeActivation(false)
            }
        }

        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            // do nothing
        }
    }

    object Menu : SelectMenuFeature<Menu.Args>(::Args, SelectMenuSpec("queuepicks")) {
        class Args : Arguments() {
            var mon by singleOption()
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            StateStore.process<QueuePicks> {
                handleSelect(e.mon)
            }
        }
    }


    object ControlButton : ButtonFeature<ControlButton.Args>(::Args, ButtonSpec("queuepickscontrol")) {
        enum class ControlMode {
            UP, DOWN, REMOVE, CANCEL, MODAL
        }

        class Args : Arguments() {
            var mon by string()
            var controlMode by enumBasic<ControlMode>()
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            StateStore.process<QueuePicks> {
                handleButton(e)
            }
        }
    }

    object FinishButton : ButtonFeature<FinishButton.Args>(::Args, ButtonSpec("queuepicksfinish")) {

        class Args : Arguments() {
            var enable by boolean()
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            StateStore.process<QueuePicks> {
                finish(e.enable)
            }
        }
    }

    object ReloadButton : ButtonFeature<NoArgs>(NoArgs(), ButtonSpec("queuepicksreload")) {
        override val buttonStyle = ButtonStyle.PRIMARY
        override val label = K18n_QueuePicks.ReloadLabel
        override val emoji = Emoji.fromUnicode("\uD83D\uDD04")

        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            StateStore.process<QueuePicks> {
                reload()
            }
        }
    }

    object SetLocationModal : ModalFeature<SetLocationModal.Args>(::Args, ModalSpec("queuepickslocation")) {
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
            StateStore.process<QueuePicks> {
                setLocation(e.mon, e.location)
            }
        }
    }

    context(league: League)
    suspend fun isIllegal(idx: Int, currentState: List<QueuedAction>): ErrorOrNull {
        return league.tierlist.withTL { it.checkLegalityOfQueue(idx, currentState) }
    }
}
