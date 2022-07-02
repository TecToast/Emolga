package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import net.dv8tion.jda.api.entities.Member
import java.util.function.Consumer

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
        val league = emolgaJSON.getJSONObject("drafts").getJSONObject(e.arguments!!.getText("draft"))
        val picksObj = league.getJSONObject("picks")
        val b = StringBuilder()
        val ids = ArrayList(picksObj.keySet())
        val names = HashMap<String, String>()
        EmolgaMain.emolgajda.getGuildById(league.getString("guild"))!!.retrieveMembersByIds(*ids.toTypedArray()).get()
            .forEach(
                Consumer { mem: Member -> names[mem.id] = mem.effectiveName })
        for (id in ids) {
            val picksArr = picksObj.getJSONArray(id)
            b.append("=== [gen8nationaldexag-box] ").append(e.getArg(0)).append("/").append(names[id])
                .append(" ===\n\n")
            picksArr.toJSONList().asSequence().map { it.getString("name") }
                .sortedByDescending { dataJSON.getJSONObject(getDataName(it)).getJSONObject("baseStats").getInt("spe") }
                .map { str: String ->
                    if (sdex.containsKey(str)) {
                        return@map """${getEnglName(getFirst(str))}${getFirstAfterUppercase(sdex.getValue(str))} 
Ability: ${dataJSON.getJSONObject(getDataName(str)).getJSONObject("abilities").getString("0")}"""
                    }
                    if (str.startsWith("A-")) {
                        return@map """
                    ${getEnglName(str.substring(2))}-Alola 
                    Ability: ${dataJSON.getJSONObject(getDataName(str)).getJSONObject("abilities").getString("0")}
                    """.trimIndent()
                    }
                    if (str.startsWith("G-")) {
                        return@map """
                    ${getEnglName(str.substring(2))}-Galar 
                    Ability: ${dataJSON.getJSONObject(getDataName(str)).getJSONObject("abilities").getString("0")}
                    """.trimIndent()
                    }
                    if (str.startsWith("M-")) {
                        return@map """
                    ${getEnglName(str.substring(2))}-Mega 
                    Ability: ${dataJSON.getJSONObject(getDataName(str)).getJSONObject("abilities").getString("0")}
                    """.trimIndent()
                    }
                    """${getEnglName(str)} 
Ability: ${dataJSON.getJSONObject(getDataName(str)).getJSONObject("abilities").getString("0")}"""
                }.forEach { str: String? -> b.append(str).append("\n\n") }
            b.append("\n")
            if (b.length > 1500) {
                e.reply(b.toString())
                b.setLength(0)
            }
        }
        e.reply(b.toString())
    }
}