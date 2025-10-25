package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.features.*
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.config.QueuePicksUserData
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.QueuePicks
import de.tectoast.emolga.utils.draft.TierlistMode
import de.tectoast.emolga.utils.json.db
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.emoji.Emoji

object QueuePicks {
    context(iData: InteractionData)
    fun League.queueNotEnabled(): Boolean {
        if (!config.triggers.queuePicks) {
            iData.reply("Das Queuen von Picks ist in dieser Liga deaktiviert!")
            return true
        } else return false
    }

    object Command : CommandFeature<NoArgs>(
        NoArgs(), CommandSpec(
            "queuepicks", "Verwalte deine gequeueten Picks"
        )
    ) {

        object Manage : CommandFeature<NoArgs>(NoArgs(), CommandSpec("manage", "Verwalte deine gequeueten Picks")) {
            context(iData: InteractionData)
            override suspend fun exec(e: NoArgs) {
                iData.ephemeralDefault()
                val league = db.leagueByCommand() ?: return iData.reply("Du bist in keiner Liga auf diesem Server!")
                if (league.queueNotEnabled()) return
                val currentData =
                    league.persistentData.queuePicks.queuedPicks.getOrPut(league(iData.user)) { QueuePicksUserData() }
                val currentState = currentData.queued
                if (currentState.isEmpty()) return iData.reply(
                    "Du hast zurzeit keine Picks in der Queue! Füge welche über /queuepicks add hinzu!",
                    ephemeral = true
                )
                QueuePicks(
                    iData.user, league.leaguename, currentData
                ).process {
                    init()
                }
            }
        }

        object Add : CommandFeature<Add.Args>(::Args, CommandSpec("add", "Füge einen Pick hinzu")) {
            class Args : Arguments() {
                var mon by draftPokemon("Pokemon", "Das Pokemon, das du hinzufügen möchtest")
                var oldmon by draftPokemon(
                    "Altes Mon",
                    "Das Pokemon, was rausgeschmissen werden soll",
                    autocomplete = { s, event ->
                        val league = db.leagueByGuild(event.guild?.idLong ?: -1, event.user.idLong)
                            ?: return@draftPokemon listOf("Du nimmt an keiner Liga auf diesem Server teil!")
                        monOfTeam(s, league, league(event.user.idLong))
                    }).nullable()
            }

            context(iData: InteractionData)
            override suspend fun exec(e: Args) {
                iData.ephemeralDefault()
                iData.deferReply()
                League.executeOnFreshLock(
                    { db.leagueByCommand() },
                    { iData.reply("Du bist in keiner Liga auf diesem Server!") }) l@{
                    if (queueNotEnabled()) return@l
                    val oldmon = e.oldmon
                    val idx = this(iData.user)
                    if (oldmon == null && !isRunning && picks.isNotEmpty() && !config.triggers.allowPickDuringSwitch) {
                        return@l iData.reply("Im kommenden Draft können nur Switches gemacht werden, dementsprechend musst du ein altes Pokemon angeben!")
                    }
                    if (oldmon != null && picks[idx]?.any { it.name == oldmon.official } != true) {
                        return@l iData.reply("Du besitzt `${oldmon.tlName}` nicht!")
                    }
                    if (isPicked(e.mon.official)) {
                        return@l iData.reply("`${e.mon.tlName}` wurde bereits gepickt!")
                    }
                    val data = persistentData.queuePicks.queuedPicks.getOrPut(idx) { QueuePicksUserData() }
                    val mon = e.mon
                    for (q in data.queued) {
                        if (q.g == mon) return@l iData.reply("Du hast `${mon.tlName}` bereits in deiner Queue!")
                        if (oldmon != null && q.y == oldmon) return@l iData.reply("Du planst bereits, `${oldmon.tlName}` rauszuwerfen!")
                    }
                    tierlist.getTierOf(mon.tlName) ?: return@l iData.reply("`${mon.tlName}` ist nicht in der Tierlist!")
                    oldmon?.let {
                        tierlist.getTierOf(it.tlName)
                            ?: return@l iData.reply("`${it.tlName}` ist nicht in der Tierlist!")
                    }
                    val queuedAction = QueuedAction(mon, oldmon)
                    val newlist = data.queued.toMutableList().apply { add(queuedAction) }
                    if (isIllegal(idx, newlist)) return@l
                    StateStore.processIgnoreMissing<QueuePicks> {
                        addNewMon(queuedAction)
                    }
                    data.queued = newlist
                    data.enabled = false
                    save()
                    iData.reply(
                        "`${mon.tlName}` wurde zu deinen gequeueten Picks hinzugefügt! Außerdem wurde das System für dich deaktiviert, damit du das Pokemon noch an die richtige Stelle schieben kannst :)".notNullPrepend(
                            oldmon
                        ) { "`${it.tlName}` -> " })
                }

            }
        }

        context(iData: InteractionData)
        suspend fun changeActivation(enable: Boolean) {
            League.executeOnFreshLock(
                { db.leagueByCommand() },
                { iData.reply("Du bist in keiner Liga auf diesem Server!") }) l@{
                if (queueNotEnabled()) return@l
                val idx = this(iData.user)
                val data = persistentData.queuePicks.queuedPicks.getOrPut(idx) { QueuePicksUserData() }
                if (isIllegal(idx, data.queued)) return@l
                data.enabled = enable
                save()
            }
            iData.reply("Das System wurde für dich ${if (enable) "" else "de"}aktiviert!", ephemeral = true)
        }

        object Enable : CommandFeature<NoArgs>(NoArgs(), CommandSpec("enable", "Aktiviert das System für dich")) {
            context(iData: InteractionData) override suspend fun exec(e: NoArgs) {
                changeActivation(true)
            }
        }

        object Disable : CommandFeature<NoArgs>(NoArgs(), CommandSpec("disable", "Deaktiviert das System für dich")) {
            context(iData: InteractionData) override suspend fun exec(e: NoArgs) {
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
        override val label = "Neue Pokemon laden"
        override val emoji = Emoji.fromUnicode("\uD83D\uDD04")

        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            StateStore.process<QueuePicks> {
                reload()
            }
        }
    }

    object SetLocationModal : ModalFeature<SetLocationModal.Args>(::Args, ModalSpec("queuepickslocation")) {
        override val title = "Pokemon verschieben"

        class Args : Arguments() {
            var mon by string().compIdOnly()
            var location by string<Int>("Position") {
                validate { it.toIntOrNull() }
                modal {
                    placeholder = "Die Position, an die das Pokemon verschoben werden soll"
                }
            }
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            StateStore.process<QueuePicks> {
                setLocation(e.mon, e.location)
            }
        }
    }

    context(league: League, iData: InteractionData)
    suspend fun isIllegal(idx: Int, currentState: List<QueuedAction>): Boolean {
        return when (league.tierlist.mode) {
            TierlistMode.POINTS -> {
                var gPoints = 0
                var yPoints = 0
                for (data in currentState) {
                    gPoints += league.tierlist.getPointsNeeded(league.tierlist.getTierOf(data.g.tlName)!!)
                    yPoints += data.y?.let { league.tierlist.getPointsNeeded(league.tierlist.getTierOf(it.tlName)!!) }
                        ?: 0
                }
                val result = league.points[idx] - gPoints + yPoints < league.minimumNeededPointsForTeamCompletion(
                    (league.picks[idx]?.size ?: 0) + currentState.size
                )
                if (result) iData.reply("Mit dieser Queue könnte dein Team nicht mehr vervollständigt werden!")
                result
            }

            TierlistMode.TIERS -> {
                val tiers = league.getPossibleTiers(idx)
                for (map in tiers) {
                    currentState.forEach {
                        map.add(league.tierlist.getTierOf(it.g.tlName)!!, -1)
                        it.y?.let { y -> map.add(league.tierlist.getTierOf(y.tlName)!!, 1) }
                    }
                }
                val result = tiers.mapNotNull { map ->
                    map.entries.firstOrNull { it.value < 0 }
                }
                val isIllegal = result.size == tiers.size
                if (isIllegal) {
                    iData.reply(
                        "Mit dieser Queue hättest du zu viele Pokemon im `${
                            result.distinct().joinToString("/")
                        }`-Tier!"
                    )
                }
                isIllegal
            }

            TierlistMode.TIERS_WITH_FREE -> {
                iData.reply("Das Queuen von Picks ist in Drafts mit Freepicks derzeit noch nicht möglich.")
                true
            }
        }

    }
}
