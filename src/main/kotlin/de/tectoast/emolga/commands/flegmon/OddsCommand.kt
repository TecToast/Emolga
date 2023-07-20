package de.tectoast.emolga.commands.flegmon

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.PepeCommand
import de.tectoast.emolga.utils.Constants

class OddsCommand : PepeCommand("odds", "Bin zu faul Help Nachrichten zu schreiben") {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val uid = e.author.idLong
        if (uid == Constants.M.TARIA) {
            e.reply("hm, joa, das sind zu viele Nullen nach dem Komma zum zählen :c")
        } else {
            e.reply("Die Chance beträgt 1/3.")
        }
    }
}
