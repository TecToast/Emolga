package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.randomWithCondition
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.MDLTierlist
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.MDL
import de.tectoast.emolga.utils.json.emolga.draft.MDLPick
import dev.minn.jda.ktx.interactions.components.danger
import dev.minn.jda.ktx.interactions.components.success
import mu.KotlinLogging

class PickMDLCommand : Command("pickmdl", "Gamblen :)", CommandCategory.Draft) {

    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            add(
                "type",
                "Typ",
                "Der Typ, der gewählt werden soll",
                ArgumentManagerTemplate.Text.of(
                    PickTypeCommand.typeList.map { SubCommand(it, it) }
                ))
        }
        slash(true, Constants.G.VIP)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val d =
            League.byCommand(e) ?: return e.reply("Es läuft zurzeit kein Draft in diesem Channel!", ephemeral = true)
        if (d !is MDL) return e.reply("Dieser Command funktioniert nur im MDL Draft!")
        val mem = d.current
        val picks = d.picks[mem]!!
        val type = e.arguments.getText("type")
        var tier = ""
        var mon = ""
        val usedTiers = mutableSetOf<String>()
        for (i in 0 until 100) {
            val temptier = tiers.toMutableList().apply { removeAll { it.first in usedTiers } }
                .randomWithCondition { it.second > picks.count { mon -> mon.tier == it.first } }?.first
                ?: return e.reply("Es gibt kein $type-Pokemon mehr, welches in deinen Kader passt!")

            val tempmon = MDLTierlist.get[type]!![temptier]!!.randomWithCondition { !d.isPicked(it) }
            if (tempmon != null) {
                tier = temptier
                mon = tempmon
                break
            }
            usedTiers += temptier
        }
        if (mon.isEmpty() || tier.isEmpty()) {
            logger.error("No pokemon found without error message: $mem $type")
            sendToMe("ERROR PICKMDL COMMAND CONSOLE: $mem $type")
            return e.reply("Es ist ein unbekannter Fehler aufgetreten!")
        }
        val official = NameConventionsDB.getDiscordTranslation(mon, d.guild, false)!!.official
        d.replyGeneral(e, "gegambled: **$mon ($tier)**!") {
            it.addActionRow(
                success("mdlpick;accept", "Akzeptieren"),
                danger("mdlpick;reroll", "Joker einlösen (noch ${d.jokers[mem]} übrig)")
            )
        }
        d.currentMon = MDLPick(official, mon, tier, type)
    }

    companion object {
        val tiers = mapOf("S" to 1, "A" to 2, "B" to 3, "C" to 3, "D" to 2).toList()
        private val logger = KotlinLogging.logger {}
    }
}