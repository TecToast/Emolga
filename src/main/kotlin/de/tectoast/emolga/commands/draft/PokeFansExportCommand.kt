package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.commands.*
import de.tectoast.emolga.utils.json.db
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import java.util.regex.Pattern

object PokeFansExportCommand : Command("pokefansexport", "Macht Pokefans Export lol", CommandCategory.Draft) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder().add(
            "draft",
            "Draft-Name",
            "Der Name der Liga, für die der Export gemacht werden soll",
            ArgumentManagerTemplate.draft()
        ).setExample("!pokefansexport Emolga-Conference").build()
        setCustomPermissions(PermissionPreset.CULT)
        slash()
    }

    override suspend fun process(e: GuildCommandEvent) {
        e.done(ephemeral = true)
        val tc = e.textChannel
        val league = db.league(e.arguments.getText("draft"))
        val picksObj = league.picks
        val tosend = mutableListOf<JsonElement>()
        val ids = ArrayList(picksObj.keys)
        val names = mutableMapOf<Long, String>()
        EmolgaMain.emolgajda.getGuildById(league.guild)!!.retrieveMembersByIds(ids).get()
            .forEach { names[it.idLong] = it.effectiveName }
        for (id in ids) {
            val picksArr = picksObj[id].names().map { it to getDataObject(it).speed }
            val oneUser = buildJsonArray {
                add(JUST_CHARS_AND_WHITESPACES.matcher(names[id]!!).replaceAll(""))
                add(e.getArg(0))
                val mons = buildJsonArray {
                    picksArr.asSequence().sortedByDescending {
                        it.second
                    }.map {
                        it.first.replace("-Therian", " Tiergeistform")
                            .replace("Wolwerock-Midnight", "Wolwerock Nachtform")
                            .replace("Wolwerock-Dusk", "Wolwerock Zwielichtform").replace("Shaymin", "Shaymin Landform")
                            .replace("Durengard", "Durengard Schildform").replace("Pumpdjinn", "Pumpdjinn XL")
                            .let { str ->
                                var tmp = str
                                for (suffix in listOf("Mega", "Alola", "Galar", "Hisui", "Paldea")) {
                                    if (tmp.endsWith("-$suffix")) tmp = suffix + "-" + tmp.substringBeforeLast("-")
                                }
                                tmp
                            }
                    }.forEach { add(it) }
                }
                add(mons)
            }
            tosend.add(oneUser)
        }
        tc.sendMessage("[").queue()
        tosend.chunked(2).forEach {
            tc.sendMessage(myJSON.encodeToString(JsonArray(it)).removeSurrounding("[", "]") + ",").queue()
        }
        tc.sendMessage("]").queue()
    }


    private val JUST_CHARS_AND_WHITESPACES = Pattern.compile("[^A-Za-z\\s]")

}
