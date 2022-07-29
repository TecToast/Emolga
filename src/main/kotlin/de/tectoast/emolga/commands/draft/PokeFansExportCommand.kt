package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.draft.Draft
import de.tectoast.jsolf.JSONArray
import java.util.regex.Pattern

class PokeFansExportCommand : Command("pokefansexport", "Macht Pokefans Export lol", CommandCategory.Draft) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder().add(
            "draft",
            "Draft-Name",
            "Der Name der Liga, für die der Export gemacht werden soll",
            ArgumentManagerTemplate.draft()
        ).setExample("!pokefansexport Emolga-Conference").build()
        setCustomPermissions(PermissionPreset.CULT)
    }

    override fun process(e: GuildCommandEvent) {
        val league = Draft.getLeagueStatic(e.arguments.getText("draft"))
        val picksObj = league.getJSONObject("picks")
        val tosend = JSONArray()
        val ids = ArrayList(picksObj.keySet())
        val names = HashMap<String, String>()
        EmolgaMain.emolgajda.getGuildById(league.getString("guild"))!!.retrieveMembersByIds(*ids.toTypedArray()).get()
            .forEach { names[it.id] = it.effectiveName }
        for (id in ids) {
            val picksArr = picksObj.getJSONArray(id)
            val oneUser = JSONArray()
            oneUser.put(JUST_CHARS_AND_WHITESPACES.matcher(names[id]!!).replaceAll(""))
            oneUser.put(e.getArg(0))
            val mons = JSONArray()
            picksArr.toJSONList().asSequence().map { it.getString("name") }.sortedWith(compareByDescending {
                dataJSON.getJSONObject(getDataName(it)).getJSONObject("baseStats").getInt("spe")
            }).map {
                it.replace("Boreos-T", "Boreos Tiergeistform").replace("Voltolos-T", "Voltolos Tiergeistform")
                    .replace("Demeteros-T", "Demeteros Tiergeistform")
                    .replace("Boreos-I", "Boreos Inkarnationsform")
                    .replace("Voltolos-I", "Voltolos Inkarnationsform")
                    .replace("Demeteros-I", "Demeteros Inkarnationsform")
                    .replace("Wolwerock-Tag", "Wolwerock Tagform").replace("Wolwerock-Nacht", "Wolwerock Nachtform")
                    .replace("Wolwerock-Zw", "Wolwerock Zwielichtform").replace("Shaymin", "Shaymin Landform")
                    .replace("Durengard", "Durengard Schildform").replace("Pumpdjinn", "Pumpdjinn XL")
                    .replace("M-", "Mega-").replace("A-", "Alola-").replace("G-", "Galar-")
            }.forEach { mons.put(it) }
            oneUser.put(mons)
            tosend.put(oneUser)
            if (tosend.toString().length > 1500) {
                e.reply(tosend.toString())
                tosend.clear()
            }
        }
        e.reply(tosend.toString())
    }

    companion object {
        private val JUST_CHARS_AND_WHITESPACES = Pattern.compile("[^A-Za-z\\s]")
    }
}