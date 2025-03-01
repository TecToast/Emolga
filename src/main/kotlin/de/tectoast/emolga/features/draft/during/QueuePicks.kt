package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.features.*
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.config.QueuePicksUserData
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.QueuePicks
import de.tectoast.emolga.utils.draft.TierlistMode
import de.tectoast.emolga.utils.json.db
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle

object QueuePicks {
    object Command : CommandFeature<NoArgs>(
        NoArgs(), CommandSpec(
            "queuepicks", "Verwalte deine gequeueten Picks", Constants.G.ASL, Constants.G.NDS,
            Constants.G.EPP, Constants.G.LOEWE
        )
    ) {

        object Manage : CommandFeature<NoArgs>(NoArgs(), CommandSpec("manage", "Verwalte deine gequeueten Picks")) {
            context(InteractionData)
            override suspend fun exec(e: NoArgs) {
                ephemeralDefault()
                val league = db.leagueByCommand() ?: return reply("Du bist in keiner Liga auf diesem Server!")
                val currentData =
                    league.persistentData.queuePicks.queuedPicks.getOrPut(league.index(user)) { QueuePicksUserData() }
                val currentState = currentData.queued
                if (currentState.isEmpty()) return reply(
                    "Du hast zurzeit keine Picks in der Queue! Füge welche über /queuepicks add hinzu!",
                    ephemeral = true
                )
                QueuePicks(
                    user, league.leaguename, currentData
                ).process {
                    init()
                }
            }
        }

        object Add : CommandFeature<Add.Args>(::Args, CommandSpec("add", "Füge einen Pick hinzu")) {
            class Args : Arguments() {
                var mon by draftPokemon("Pokemon", "Das Pokemon, das du hinzufügen möchtest")
                var oldmon by draftPokemon("Altes Mon",
                    "Das Pokemon, was rausgeschmissen werden soll",
                    autocomplete = { s, event ->
                        val league = db.leagueByGuild(event.guild?.idLong ?: -1, event.user.idLong)
                            ?: return@draftPokemon listOf("Du nimmt an keiner Liga auf diesem Server teil!")
                        monOfTeam(s, league, league(event.user.idLong))
                    }).nullable()
            }

            context(InteractionData)
            override suspend fun exec(e: Args) {
                ephemeralDefault()
                deferReply()
                League.executeOnFreshLock({ db.leagueByCommand() },
                    { return reply("Du bist in keiner Liga auf diesem Server!") }) {
                    val oldmon = e.oldmon
                    val idx = this(user)
                    if (oldmon == null && !isRunning && picks.isNotEmpty() && !config.allowPickDuringSwitch.enabled) {
                        return reply("Im kommenden Draft können nur Switches gemacht werden, dementsprechend musst du ein altes Pokemon angeben!")
                    }
                    if (oldmon != null && picks[idx]?.any { it.name == oldmon.official } != true) {
                        return reply("Du besitzt `${oldmon.tlName}` nicht!")
                    }
                    if (isPicked(e.mon.official)) {
                        return reply("`${e.mon.tlName}` wurde bereits gepickt!")
                    }
                    val data = persistentData.queuePicks.queuedPicks.getOrPut(idx) { QueuePicksUserData() }
                    val mon = e.mon
                    for (q in data.queued) {
                        if (q.g == mon) return reply("Du hast `${mon.tlName}` bereits in deiner Queue!")
                        if (oldmon != null && q.y == oldmon) return reply("Du planst bereits, `${oldmon.tlName}` rauszuwerfen!")
                    }
                    tierlist.getTierOf(mon.tlName) ?: return reply("`${mon.tlName}` ist nicht in der Tierlist!")
                    oldmon?.let {
                        tierlist.getTierOf(it.tlName) ?: return reply("`${it.tlName}` ist nicht in der Tierlist!")
                    }
                    val queuedAction = QueuedAction(mon, oldmon)
                    val newlist = data.queued.toMutableList().apply { add(queuedAction) }
                    if (isIllegal(idx, newlist)) return
                    StateStore.processIgnoreMissing<QueuePicks> {
                        addNewMon(queuedAction)
                    }
                    data.queued = newlist
                    data.enabled = false
                    save("QueuePickAdd")
                    reply("`${mon.tlName}` wurde zu deinen gequeueten Picks hinzugefügt! Außerdem wurde das System für dich deaktiviert, damit du das Pokemon noch an die richtige Stelle schieben kannst :)".notNullPrepend(
                        oldmon
                    ) { "`${it.tlName}` -> " })
                }

            }
        }

        context(InteractionData)
        suspend fun changeActivation(enable: Boolean) {
            League.executeOnFreshLock({ db.leagueByCommand() },
                { return reply("Du bist in keiner Liga auf diesem Server!") }) {
                val idx = this(user)
                val data = persistentData.queuePicks.queuedPicks.getOrPut(idx) { QueuePicksUserData() }
                if (isIllegal(idx, data.queued)) return
                data.enabled = enable
                save("QueuePickActivation")
            }
            reply("Das System wurde für dich ${if (enable) "" else "de"}aktiviert!", ephemeral = true)
        }

        object Enable : CommandFeature<NoArgs>(NoArgs(), CommandSpec("enable", "Aktiviert das System für dich")) {
            context(InteractionData) override suspend fun exec(e: NoArgs) {
                changeActivation(true)
            }
        }

        object Disable : CommandFeature<NoArgs>(NoArgs(), CommandSpec("disable", "Deaktiviert das System für dich")) {
            context(InteractionData) override suspend fun exec(e: NoArgs) {
                changeActivation(false)
            }
        }

        context(InteractionData)
        override suspend fun exec(e: NoArgs) {
            // do nothing
        }
    }

    object Menu : SelectMenuFeature<Menu.Args>(::Args, SelectMenuSpec("queuepicks")) {
        class Args : Arguments() {
            var mon by singleOption()
        }

        context(InteractionData)
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

        context(InteractionData)
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

        context(InteractionData)
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

        context(InteractionData)
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

        context(InteractionData)
        override suspend fun exec(e: Args) {
            StateStore.process<QueuePicks> {
                setLocation(e.mon, e.location)
            }
        }
    }

    context(League, InteractionData)
    suspend fun isIllegal(idx: Int, currentState: List<QueuedAction>): Boolean {
        return when (tierlist.mode) {
            TierlistMode.POINTS -> {
                var gPoints = 0
                var yPoints = 0
                for (data in currentState) {
                    gPoints += tierlist.getPointsNeeded(tierlist.getTierOf(data.g.tlName)!!)
                    yPoints += data.y?.let { tierlist.getPointsNeeded(tierlist.getTierOf(it.tlName)!!) } ?: 0
                }
                val result = points[idx] - gPoints + yPoints < minimumNeededPointsForTeamCompletion(
                    (picks[idx]?.size ?: 0) + currentState.size
                )
                if (result) reply("Mit dieser Queue könnte dein Team nicht mehr vervollständigt werden!")
                result
            }

            TierlistMode.TIERS -> {
                val tiers = getPossibleTiers(idx)
                currentState.forEach {
                    tiers.add(tierlist.getTierOf(it.g.tlName)!!, -1)
                    it.y?.let { y -> tiers.add(tierlist.getTierOf(y.tlName)!!, 1) }
                }
                tiers.entries.firstOrNull { it.value < 0 }?.let {
                    reply("Mit dieser Queue hättest du zu viele Pokemon im `${it.key}`-Tier!")
                    true
                } == true
            }

            TierlistMode.TIERS_WITH_FREE -> {
                reply("Das Queuen von Picks ist in Drafts mit Freepicks derzeit noch nicht möglich.")
                true
            }
        }

    }
}
