package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.draft.Draft
import de.tectoast.jsolf.JSONArray
import net.dv8tion.jda.api.entities.Member
import java.util.function.Consumer
import java.util.regex.Pattern

class PokeFansExportCommand : Command("pokefansexport", "Macht Pokefans Export lol", CommandCategory.Draft) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder().add(
            "draft",
            "Draft-Name",
            "Der Name der Liga, für die der Export gemacht werden soll",
            ArgumentManagerTemplate.draft()
        )
            .setExample("!pokefansexport Emolga-Conference")
            .build()
        setCustomPermissions(PermissionPreset.CULT)
    }

    override fun process(e: GuildCommandEvent) {
        val league = Draft.getLeagueStatic(e.arguments!!.getText("draft"))
        val picksObj = league.getJSONObject("picks")
        val tosend = JSONArray()
        val ids = ArrayList(picksObj.keySet())
        val names = HashMap<String, String>()
        EmolgaMain.emolgajda.getGuildById(league.getString("guild"))!!.retrieveMembersByIds(*ids.toTypedArray()).get()
            .forEach(
                Consumer { mem: Member -> names[mem.id] = mem.effectiveName })
        for (id in ids) {
            val picksArr = picksObj.getJSONArray(id)
            val oneUser = JSONArray()
            oneUser.put(JUST_CHARS_AND_WHITESPACES.matcher(names[id]).replaceAll(""))
            oneUser.put(e.getArg(0))
            val mons = JSONArray()
            picksArr.toJSONList().stream().map { it.getString("name") }
                .sorted(Comparator.comparing { str: String ->
                    dataJSON.getJSONObject(getDataName(str)).getJSONObject("baseStats").getInt("spe")
                }
                    .reversed())
                .map { str: String ->
                    str
                        .replace("Boreos-T", "Boreos Tiergeistform")
                        .replace("Voltolos-T", "Voltolos Tiergeistform")
                        .replace("Demeteros-T", "Demeteros Tiergeistform")
                        .replace("Boreos-I", "Boreos Inkarnationsform")
                        .replace("Voltolos-I", "Voltolos Inkarnationsform")
                        .replace("Demeteros-I", "Demeteros Inkarnationsform")
                        .replace("Wolwerock-Tag", "Wolwerock Tagform")
                        .replace("Wolwerock-Nacht", "Wolwerock Nachtform")
                        .replace("Wolwerock-Zw", "Wolwerock Zwielichtform")
                        .replace("Shaymin", "Shaymin Landform")
                        .replace("Durengard", "Durengard Schildform")
                        .replace("Pumpdjinn", "Pumpdjinn XL")
                        .replace("M-", "Mega-")
                        .replace("A-", "Alola-")
                        .replace("G-", "Galar-")
                }.forEach { value: String? -> mons.put(value) }
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