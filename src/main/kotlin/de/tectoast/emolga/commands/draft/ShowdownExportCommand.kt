package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.names
import de.tectoast.emolga.utils.json.db

class ShowdownExportCommand : Command("showdownexport", "Macht Showdown Export lol", CommandCategory.Pokemon) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder().add(
            "draft",
            "Draft-Name",
            "Der Name der Liga, für die der Export gemacht werden soll",
            ArgumentManagerTemplate.draft()
        )
            .setExample("!showdownexport Emolga-Conference")
            .build()
        setCustomPermissions(PermissionPreset.CULT)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val league = db.league(e.arguments.getText("draft"))
        val picksObj = league.picks
        val b = StringBuilder()
        val ids = picksObj.keys.toList()
        val names = mutableMapOf<Long, String>()
        EmolgaMain.emolgajda.getGuildById(league.guild)!!.retrieveMembersByIds(ids).get()
            .forEach { names[it.idLong] = it.effectiveName }
        for (id in ids) {
            val picksArr = picksObj[id].names().map { it to getDataObject(it) }
            b.append("=== [gen8nationaldexag-box] ").append(e.getArg(0)).append("/").append(names[id])
                .append(" ===\n\n")
            picksArr.asSequence()
                .sortedByDescending { it.second.speed }
                .map {
                    val str = it.first
                    (if (sdex.containsKey(str)) {
                        "${getEnglName(getFirst(str))}${getFirstAfterUppercase(sdex.getValue(str))}"
                    } else if (str.startsWith("A-")) {
                        "${getEnglName(str.substring(2))}-Alola"
                    } else if (str.startsWith("G-")) {
                        "${getEnglName(str.substring(2))}-Galar"
                    } else if (str.startsWith("M-")) {
                        "${getEnglName(str.substring(2))}-Mega"
                    } else getEnglName(str)) + " \n" +
                            "Ability: ${
                                it.second.abilities["0"]
                            }"
                }.forEach { b.append(it).append("\n\n") }
            b.append("\n")
            if (b.length > 1500) {
                e.reply(b.toString())
                b.setLength(0)
            }
        }
        e.reply(b.toString())
    }
}
