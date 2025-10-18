package de.tectoast.emolga.features.draft

import de.tectoast.emolga.features.*
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.SwitchTimer
import de.tectoast.emolga.utils.TimeUtils
import de.tectoast.emolga.utils.TimerInfo
import de.tectoast.emolga.utils.l
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle

object SwitchTimer {
    object Create : CommandFeature<Create.Args>(
        ::Args,
        CommandSpec(
            "switchtimercreate",
            "Konfiguriert den Switch-Timer für eine Liga und erstellt ein Control-Panel",
        )
    ) {
        class Args : Arguments() {
            var league by string("Liga", "Der Name der Liga, für die der Timer erstellt werden soll.")
            var settings by list("Timer %s", "Die %s. Einstellung", 5, 1)
            var stallSeconds by int("Stall-Sekunden", "Die Anzahl an Sekunden, die Spieler überziehen dürfen.") {
                default = 0
            }
            var from by int("Startstunde", "Die Stunde, zu der der Timer startet.") {
                default = 0
            }
            var to by int("Endstunde", "Die Stunde, zu der der Timer endet.") {
                default = 24
            }
        }

        init {
            restrict(admin)
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            League.executeOnFreshLock(e.league) l@{
                val settings = e.settings
                val timer = SwitchTimer(settings.associateWith {
                    TimerInfo((TimeUtils.parseShortTime(it).toInt().takeIf { n -> n >= 0 }
                        ?: return@l iData.reply("`$it` ist keine valide Zeitangabe!")) / 60).set(e.from, e.to)
                })
                timer.stallSeconds = e.stallSeconds
                config.timer = timer
                save()
                val controlPanel = timer.createControlPanel(this)
                iData.reply(ephemeral = false, embeds = controlPanel.first, components = controlPanel.second)
            }
        }
    }

    object Button : ButtonFeature<Button.Args>(::Args, ButtonSpec("switchtimer")) {
        override val buttonStyle = ButtonStyle.PRIMARY

        class Args : Arguments() {
            var league by string()
            var switchTo by string()
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            League.executeOnFreshLock(e.league) {
                (config.timer as? SwitchTimer)?.let {
                    it.switchTo(e.switchTo)
                    save()
                    iData.deferEdit()
                    val (messageEmbeds, actionRows) = it.createControlPanel(this)
                    iData.edit(embeds = messageEmbeds, components = actionRows)
                    iData.reply("Timer auf `${e.switchTo}` umgestellt!", ephemeral = true)
                } ?: iData.reply("Der Timer von `$l` ist kein Switch-Timer!", ephemeral = true)
            }
        }
    }
}
