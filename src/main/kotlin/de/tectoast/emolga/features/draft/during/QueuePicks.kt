package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.QueuePicks
import de.tectoast.emolga.utils.QueuedAction
import de.tectoast.emolga.utils.StateStore
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.QueuePicksData
import de.tectoast.emolga.utils.process
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle

object QueuePicks {
    object Command : CommandFeature<NoArgs>(
        NoArgs(), CommandSpec("queuepicks", "Verwalte deine gequeueten Picks", Constants.G.ASL)
    ) {

        object Manage : CommandFeature<NoArgs>(NoArgs(), CommandSpec("manage", "Verwalte deine gequeueten Picks")) {
            context(InteractionData)
            override suspend fun exec(e: NoArgs) {
                val league = db.leagueByCommand() ?: return reply("Du bist in keiner Liga auf diesem Server!")
                val currentData = league.queuedPicks.getOrPut(league.index(user)) { QueuePicksData() }
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
            }

            context(InteractionData)
            override suspend fun exec(e: Args) {
                ephemeralDefault()
                deferReply()
                League.executeOnFreshLock({ db.leagueByCommand() },
                    { return reply("Du bist in keiner Liga auf diesem Server!") }) {
                    val data = queuedPicks.getOrPut(index(user)) { QueuePicksData() }
                    val mon = e.mon
                    if (data.queued.any { it.g == mon }) return reply("Du hast dieses Pokemon bereits in deiner Queue!")
                    tierlist.getTierOf(mon.tlName) ?: return reply("Das Pokemon ist nicht in der Tierlist!")
                    val newlist = data.queued.toMutableList().apply { add(QueuedAction(mon)) }
                    if (checkIfTeamCantBeFinished(user, newlist))
                        return reply("Wenn du dieses Pokemon holen wollen würdest, könnte dein Team nicht mehr vervollständigt werden!")
                    StateStore.processIgnoreMissing<QueuePicks> {
                        addNewMon(QueuedAction(mon))
                    }
                    data.queued = newlist
                    data.enabled = false
                    save("QueuePickAdd")
                    reply("`${mon.tlName}` wurde zu deinen gequeueten Picks hinzugefügt! Außerdem wurde das System für dich deaktiviert, damit du das Pokemon noch an die richtige Stelle schieben kannst :)")
                }

            }
        }

        context(InteractionData)
        suspend fun changeActivation(enable: Boolean) {
            League.executeOnFreshLock({ db.leagueByCommand() },
                { return reply("Du bist in keiner Liga auf diesem Server!") }) {
                val data = queuedPicks.getOrPut(index(user)) { QueuePicksData() }
                if (checkIfTeamCantBeFinished(
                        user, data.queued
                    )
                ) return reply("Mit der aktuellen Queue könnte dein Kader nicht vervollständigt werden!")
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
                handleButton(e.mon, e.controlMode)
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

    context(League)
    suspend fun checkIfTeamCantBeFinished(uid: Long, currentState: List<QueuedAction>): Boolean {
        var gPoints = 0
        var yPoints = 0
        for (data in currentState) {
            gPoints += tierlist.getPointsNeeded(tierlist.getTierOf(data.g.tlName)!!)
            yPoints += data.y?.let { tierlist.getPointsNeeded(tierlist.getTierOf(it.tlName)!!) } ?: 0
        }
        return points[uid] - gPoints + yPoints < minimumNeededPointsForTeamCompletion(
            (picks[uid]?.size ?: 0) + currentState.size
        )
    }
}
