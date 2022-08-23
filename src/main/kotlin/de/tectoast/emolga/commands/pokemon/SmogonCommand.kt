package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.selectmenus.selectmenusaves.SmogonSet
import de.tectoast.jsolf.JSONObject
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.requests.restaction.MessageAction
import org.jsoup.Jsoup
import java.io.IOException

class SmogonCommand : Command("smogon", "Zeigt die vorgeschlagenen Smogon-Sets für Gen 8", CommandCategory.Pokemon) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "form", "Form", "Optionale alternative Form", ArgumentManagerTemplate.Text.of(
                    SubCommand.of("Alola"), SubCommand.of("Galar"), SubCommand.of("Mega")
                ), true
            )
            .addEngl("mon", "Pokemon", "Das Pokemon... lol", Translation.Type.POKEMON)
            .setExample("!smogon Primarene")
            .build()
    }

    @Throws(IOException::class)
    override suspend fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        val args = e.arguments
        val name = args.getTranslation("mon").translation
        val form = if (args.has("form")) "-" + args.getText("form").lowercase() else ""
        var d =
            Jsoup.connect("https://www.smogon.com/dex/ss/pokemon/" + name.lowercase() + form + "/")
                .get()
        var obj =
            JSONObject(d.select("script")[1].data().trim().substring("dexSettings = ".length)).getJSONArray(
                "injectRpcs"
            ).getJSONArray(2).getJSONObject(1)
        if (obj.getJSONArray("strategies").length() == 0) {
            try {
                d =
                    Jsoup.connect("https://www.smogon.com/dex/sm/pokemon/" + name.lowercase() + form + "/")
                        .get()
                obj = JSONObject(
                    d.select("script")[1].data().trim()
                        .substring("dexSettings = ".length)
                ).getJSONArray("injectRpcs").getJSONArray(2).getJSONObject(1)
                tco.sendMessage("Gen 7:").queue()
            } catch (ex: Exception) {
                tco.sendMessage("Es gibt kein aktuelles Moveset für dieses Pokemon!").queue()
                return
            }
        }
        val arr = obj.getJSONArray("strategies")
        val smogon = SmogonSet(arr)
        e.reply(
            smogon.buildMessage(),
            { ma: MessageAction -> ma.setActionRows(smogon.buildActionRows()) },
            null,
            { mes: Message -> smogonMenu[mes.idLong] = smogon },
            null
        )
    }

    companion object {

        val statnames =
            mapOf("hp" to "HP", "atk" to "Atk", "def" to "Def", "spa" to "SpA", "spd" to "SpD", "spe" to "Spe")
    }
}