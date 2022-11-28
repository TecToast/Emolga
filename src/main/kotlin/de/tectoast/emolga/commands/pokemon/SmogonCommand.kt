package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.*
import de.tectoast.emolga.selectmenus.selectmenusaves.SmogonSet
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException

class SmogonCommand : Command("smogon", "Zeigt die vorgeschlagenen Smogon-Sets für Gen 8", CommandCategory.Pokemon) {
    init {
        argumentTemplate =
            ArgumentManagerTemplate.builder().addEngl("mon", "Pokemon", "Das Pokemon... lol", Translation.Type.POKEMON)
                .add(
                    "form", "Form", "Optionale alternative Form", ArgumentManagerTemplate.Text.of(
                        SubCommand.of("Alola"), SubCommand.of("Galar"), SubCommand.of("Mega")
                    ), true
                ).setExample("!smogon Primarene").build()
        slash()
    }

    @Throws(IOException::class)
    override suspend fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        val args = e.arguments
        val name = args.getTranslation("mon").translation
        val form = if (args.has("form")) "-" + args.getText("form").lowercase() else ""
        val fullname = name.lowercase() + form
        var d = Jsoup.connect("https://www.smogon.com/dex/ss/pokemon/$fullname/").get()
        var obj = extractFromHTML(d)
        if (obj["strategies"]?.jsonArray?.isEmpty() == true) {
            try {
                d = Jsoup.connect("https://www.smogon.com/dex/sm/pokemon/$fullname/").get()
                obj = extractFromHTML(d)
                tco.sendMessage("Gen 7:").queue()
            } catch (ex: Exception) {
                tco.sendMessage("Es gibt kein aktuelles Moveset für dieses Pokemon!").queue()
                return
            }
        }
        val smogon = SmogonSet(otherJSON.decodeFromJsonElement(obj["strategies"]!!.jsonArray))
        val id = fullname + System.currentTimeMillis()
        smogonMenu[id] = smogon
        e.reply(smogon.buildMessage(), ra = {
            it.setComponents(smogon.buildActionRows(id))
        })
    }

    companion object {

        private fun extractFromHTML(d: Document): JsonObject = otherJSON.parseToJsonElement(
            d.select("script")[1].data().trim().substring("dexSettings = ".length)
        ).jsonObject["injectRpcs"]!!.jsonArray[2].jsonArray[1].jsonObject
    }
}
