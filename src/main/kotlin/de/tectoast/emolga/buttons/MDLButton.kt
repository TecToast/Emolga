package de.tectoast.emolga.buttons

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.add
import de.tectoast.emolga.commands.draft.PickMDLCommand
import de.tectoast.emolga.commands.invoke
import de.tectoast.emolga.commands.randomWithCondition
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.MDLTierlist
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.MDL
import de.tectoast.emolga.utils.json.emolga.draft.MDLPick
import de.tectoast.emolga.utils.json.emolga.draft.PickData
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.danger
import dev.minn.jda.ktx.interactions.components.success
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class MDLButton : ButtonListener("mdlpick") {
    override suspend fun process(e: ButtonInteractionEvent, name: String) {
        val d = League.onlyChannel(e.channel.idLong)
            ?: return e.reply("Dieser Button funktioniert nicht mehr! Wenn du denkst, dass dies ein Fehler ist, melde dich bei ${Constants.MYTAG}.")
                .setEphemeral(true).queue()
        if (e.user.idLong != d.current && e.user.idLong != Constants.FLOID && d.allowed[d.current]?.any { it.u == e.user.idLong } != true) return e.reply(
            "Du bist nicht dran!"
        ).setEphemeral(true).queue()
        if (d !is MDL) return e.reply("Dieser Button funktioniert nur im MDL Draft!").setEphemeral(true).queue()
        d.currentMon ?: return e.reply("Es gibt zurzeit keinen Pick!").setEphemeral(true).queue()
        val (official, mon, tier, type) = d.currentMon!!
        if (name == "accept") {
            e.reply("**${d.currentMon?.tlName} (${d.currentMon?.tier})** wurde akzeptiert!").await()
            val picks = d.picks(d.current)
            d.savePick(picks, mon, tier, false)
            val round = d.getPickRoundOfficial()
            with(d) {
                builder().let { b ->
                    b.pickDoc(
                        PickData(
                            league = d,
                            pokemon = mon,
                            tier = tier,
                            mem = d.current,
                            indexInRound = indexInRound(round),
                            changedIndex = picks.indexOfFirst { it.name == official },
                            picks = picks,
                            round = round,
                            memIndex = table.indexOf(d.current),
                            freePick = false
                        )
                    ).let { b }
                }.execute()
            }
            d.afterPickOfficial()
        } else {
            val mem = d.current
            if (d.jokers[mem]!! <= 0) return e.reply("Du hast keine Joker mehr!").queue()
            d.jokers.add(mem, -1)
            val picks = d.picks[mem]!!
            var newtier = ""
            var newmon = ""
            val usedTiers = mutableSetOf<String>()
            for (i in 0 until 100) {
                val temptier = PickMDLCommand.tiers.toMutableList().apply { removeAll { it.first in usedTiers } }
                    .randomWithCondition { it.second > picks.count { mon -> mon.tier == it.first } }?.first
                    ?: return e.reply("Es gibt kein $type-Pokemon mehr, welches in deinen Kader passt!").queue()

                val tempmon = MDLTierlist.get[type]!![temptier]!!.randomWithCondition { !d.isPicked(it) }
                if (tempmon != null) {
                    newtier = temptier
                    newmon = tempmon
                    break
                }
                usedTiers += temptier
            }
            if (newmon.isEmpty() || newtier.isEmpty()) {
                Command.sendToMe("ERROR PICKMDL COMMAND CONSOLE: $mem $type")
                return e.reply("Es ist ein unbekannter Fehler aufgetreten!").queue()
            }
            val newofficial = NameConventionsDB.getDiscordTranslation(newmon, d.guild, false)!!.official
            e.reply("Reroll: **$newmon ($newtier)**!").addActionRow(
                success("mdlpick;accept", "Akzeptieren"),
                danger("mdlpick;reroll", "Joker einlösen (noch ${d.jokers[mem]} übrig)")
            ).queue()
            d.currentMon = MDLPick(newofficial, newmon, newtier, type)
        }
    }
}
