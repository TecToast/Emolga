package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.commands.*
import de.tectoast.emolga.utils.json.Emolga
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
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

    override suspend fun process(e: GuildCommandEvent) {
        val league = Emolga.get.league(e.arguments.getText("draft"))
        val picksObj = league.picks
        val tosend = mutableListOf<Any>()
        val ids = ArrayList(picksObj.keys)
        val names = mutableMapOf<Long, String>()
        EmolgaMain.emolgajda.getGuildById(league.guild)!!.retrieveMembersByIds(ids).get()
            .forEach { names[it.idLong] = it.effectiveName }
        for (id in ids) {
            val picksArr = picksObj[id].names()
            val oneUser = mutableListOf<Any>()
            oneUser.add(JUST_CHARS_AND_WHITESPACES.matcher(names[id]!!).replaceAll(""))
            oneUser.add(e.getArg(0))
            val mons = buildJsonArray {
                picksArr.asSequence().sortedWith(compareByDescending {
                    getDataObject(it).speed
                }).map {
                    it.replace("-T", " Tiergeistform")
                        .replace("-I", " Inkarnationsform")
                        .replace("Wolwerock-Tag", "Wolwerock Tagform").replace("Wolwerock-Nacht", "Wolwerock Nachtform")
                        .replace("Wolwerock-Zw", "Wolwerock Zwielichtform").replace("Shaymin", "Shaymin Landform")
                        .replace("Durengard", "Durengard Schildform").replace("Pumpdjinn", "Pumpdjinn XL")
                        .replace("M-", "Mega-").replace("A-", "Alola-").replace("G-", "Galar-")
                }.forEach { add(it) }
            }
            oneUser.add(mons)
            tosend.add(oneUser)
            if (tosend.toString().length > 1500) {
                e.reply(myJSON.encodeToString(tosend))
                tosend.clear()
            }
        }
        e.reply(tosend.toString())
    }

    companion object {
        private val JUST_CHARS_AND_WHITESPACES = Pattern.compile("[^A-Za-z\\s]")
    }
}
