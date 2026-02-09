package de.tectoast.emolga.features.draft

import de.tectoast.emolga.features.*
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.SwitchTimer
import de.tectoast.emolga.utils.TimeUtils
import de.tectoast.emolga.utils.TimerInfo
import net.dv8tion.jda.api.components.buttons.ButtonStyle

object SwitchTimer {
    object Create : CommandFeature<Create.Args>(
        ::Args,
        CommandSpec(
            "switchtimercreate",
            K18n_SwitchTimer.Help,
        )
    ) {
        class Args : Arguments() {
            var league by string("Liga", K18n_SwitchTimer.ArgLeague)
            var settings by list("Timer %s", K18n_SwitchTimer.ArgSettings, 5, 1)
            var stallSeconds by int("Stall-Sekunden", K18n_SwitchTimer.ArgStallSeconds) {
                default = 0
            }
            var from by int("Startstunde", K18n_SwitchTimer.ArgFrom) {
                default = 0
            }
            var to by int("Endstunde", K18n_SwitchTimer.ArgTo) {
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
                        ?: return@l iData.reply(K18n_SwitchTimer.InvalidTime(it))) / 60).set(e.from, e.to)
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
                    iData.edit(contentK18n = null, embeds = messageEmbeds, components = actionRows)
                    iData.reply(K18n_SwitchTimer.Success(e.switchTo), ephemeral = true)
                } ?: iData.reply(K18n_SwitchTimer.NoSwitchTimer(leaguename), ephemeral = true)
            }
        }
    }
}
