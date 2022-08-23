package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.jsolf.JSONArray
import org.slf4j.LoggerFactory
import java.util.*

class ShowdownPokeFansCommand : Command(
    "showdownpokefans",
    "Nimmt ein Showdown-Paste und wandelt es in ein Pokefans-Paste um",
    CommandCategory.Draft,
    Constants.G.CULT,
    821350264152784896L
) {
    init {
        argumentTemplate =
            ArgumentManagerTemplate.builder().add("paste", "Paste", "Das Paste", ArgumentManagerTemplate.Text.any())
                .setExample("!showdownpokefans binzufaul")
                .build()
        wip()
        disable()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val tosend = JSONArray()
        //for (String id : ids) {
        val paste = e.arguments.getText("paste")
        val oneUser = JSONArray()
        oneUser.put("HierDenNamenÄndern")
        oneUser.put("HierDieLigaÄndern")
        val pmons: MutableList<String> = LinkedList()
        for (s in paste.split("\n")) {
            if (s.isBlank()) continue
            if (s.contains(":") && !s.contains("Type: Null")) continue
            logger.info("s = $s")
            pmons.add(s.trim())
        }
        val mons = JSONArray()
        logger.info("pmons = $pmons")
        pmons.asSequence()
            .sortedByDescending {
                dataJSON.getJSONObject(toSDName(it)).getJSONObject("baseStats").getInt("spe")
            }
            .map { s: String ->
                val split = s.split("-")
                getGerNameNoCheck(split[0]) + if (split.size > 1) "-" + split[1] else ""
            }
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
        //}
        e.reply(tosend.toString())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ShowdownPokeFansCommand::class.java)
    }
}