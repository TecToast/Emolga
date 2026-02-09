package de.tectoast.emolga.features.draft

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.ButtonFeature
import de.tectoast.emolga.features.ButtonSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.k18n
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.ButtonStyle

object RevealTeam {
    object RevealButton : ButtonFeature<RevealButton.Args>(::Args, ButtonSpec("revealteam")) {
        class Args : Arguments() {
            var league by string("league")
            var idx by int("index")
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            League.executeOnFreshLock(e.league) {
                val state = persistentData.teamReveal.revealState
                val picks = picks[e.idx] ?: return@executeOnFreshLock iData.reply(K18n_RevealTeam.NoPicksInTeam)
                val ustate = state[e.idx] ?: 0
                if (ustate >= picks.size) return@executeOnFreshLock iData.reply(K18n_RevealTeam.AllPicksRevealed)
                revealPick(e.idx, ustate)
                state[e.idx] = ustate + 1
                iData.edit(contentK18n = null, components = getButtons(this))
                save()
            }
        }
    }

    suspend fun getButtons(league: League): List<ActionRow> = with(league) {
        if (names.isEmpty()) generateNames()
        val state = persistentData.teamReveal.revealState
        return table.indices.map {
            val upicks = picks[it]
            val hasPicks = upicks != null
            val ustate = state[it] ?: 0
            RevealButton.withoutIData(
                label = names[it].k18n, buttonStyle = if (hasPicks) {
                    if (ustate >= upicks.size) ButtonStyle.SECONDARY
                    else ButtonStyle.PRIMARY
                } else ButtonStyle.DANGER,
                disabled = !hasPicks || ustate >= upicks.size
            ) {
                this.league = leaguename
                this.idx = it
            }
        }.chunked(5).map { row -> ActionRow.of(row) }
    }
}