package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

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

    override fun process(e: GuildCommandEvent) {
        val league = emolgaJSON.getJSONObject("drafts").getJSONObject(e.arguments.getText("draft"))
        val picksObj = league.getJSONObject("picks")
        val b = StringBuilder()
        val ids = ArrayList(picksObj.keySet())
        val names = HashMap<String, String>()
        EmolgaMain.emolgajda.getGuildById(league.getString("guild"))!!.retrieveMembersByIds(*ids.toTypedArray()).get()
            .forEach { names[it.id] = it.effectiveName }
        for (id in ids) {
            val picksArr = picksObj.getJSONArray(id)
            b.append("=== [gen8nationaldexag-box] ").append(e.getArg(0)).append("/").append(names[id])
                .append(" ===\n\n")
            picksArr.toJSONList().asSequence().map { it.getString("name") }
                .sortedByDescending { dataJSON.getJSONObject(getDataName(it)).getJSONObject("baseStats").getInt("spe") }
                .map { str: String ->
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
                                dataJSON.getJSONObject(getDataName(str)).getJSONObject("abilities").getString("0")
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