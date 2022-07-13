package de.tectoast.emolga.commands.flegmon

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.PepeCommand

class OddsCommand : PepeCommand("odds", "Bin zu faul Help Nachrichten zu schreiben") {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        val uid = e.author.idLong
        if (uid == 322755315953172485L) {
            e.reply("hm, joa, das sind zu viele Nullen nach dem Komma zum zählen :c")
        } else {
            e.reply("Die Chance beträgt 1/3.")
        }
    }
}