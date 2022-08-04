package de.tectoast.emolga.commands.moderator

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class TempBanCommand : Command("tempban", "Tempbannt den User", CommandCategory.Moderator) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("user", "User", "User, der getempbannt werden soll", ArgumentManagerTemplate.DiscordType.USER)
            .add(
                "time",
                "Zeit",
                "Zeitspanne, für die der User gebannt werden soll",
                ArgumentManagerTemplate.Text.any()
            )
            .add("reason", "Grund", "Grund des Tempbanns", ArgumentManagerTemplate.Text.any())
            .setNoCheck(true)
            .setExample("!tempban @BöserUser123 1d Hat böse Sachen gemacht")
            .build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val m = e.message!!
        val raw = m.contentRaw
        val tco = e.textChannel
        val members = m.mentions.members
        if (members.size != 1) {
            //tco.sendMessage("Du musst einen Spieler taggen!").queue();
            return
        }
        val mem = members[0]
        val splitarr = raw.split(" ")
        //ArrayList<String> split = new ArrayList<>(Arrays.asList(splitarr));
        val reasonbuilder = StringBuilder()
        var time = 0
        for (i in 2 until splitarr.size) {
            if (parseShortTime(splitarr[i]) != -1) {
                time += parseShortTime(splitarr[i])
            } else reasonbuilder.append(splitarr[i]).append(" ")
        }
        val reason = reasonbuilder.toString().trim().ifEmpty { "Nicht angebeben" }
        tempBan(tco, e.member, mem, time, reason)
    }
}