package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import net.dv8tion.jda.api.EmbedBuilder
import java.awt.Color
import java.util.*

class AbilityCommand :
    Command("ability", "Zeigt, welche Mons eine gewisse Fähigkeit haben können", CommandCategory.Pokemon) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .addEngl(
                "abi",
                "Fähigkeit",
                "Name der Fähigkeit, nach der geschaut werden soll",
                Translation.Type.ABILITY,
                false
            )
            .setExample("!ability Intimidate")
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        val json = dataJSON
        val mons = ArrayList<String>()
        val abi = e.arguments!!.getTranslation("abi").translation
        for (s in json.keySet()) {
            val data = json.getJSONObject(s)
            if (data.getInt("num") < 0) continue
            if (data.getJSONObject("abilities").keySet().stream()
                    .map { ability: String? -> data.getJSONObject("abilities").getString(ability) }
                    .anyMatch { ability: String -> ability.equals(abi, ignoreCase = true) }
            ) {
                mons.add(
                    if (s == "nidoranf") "Nidoran-F" else if (s == "nidoranm") "Nidoran-M" else {
                        val gerName = getGerName(s)
                        if (gerName.isSuccess) gerName.translation else {
                            getGerNameNoCheck(data.getString("baseSpecies")) + "-" + data.getString("forme")
                        }
                    }
                )
            }
        }
        mons.removeIf { obj: String? -> Objects.isNull(obj) }
        mons.sort()
        var s = StringBuilder(2 shl 11)
        for (mon in mons) {
            s.append(mon).append("\n")
            if (s.length > 1900) {
                e.reply(
                    EmbedBuilder().setColor(Color.CYAN)
                        .setTitle(e.arguments!!.getTranslation("abi").otherLang + " haben:").setDescription(s).build()
                )
                s = StringBuilder(2 shl 11)
            }
        }
        e.reply(
            EmbedBuilder().setColor(Color.CYAN).setTitle(e.arguments!!.getTranslation("abi").otherLang + " haben:")
                .setDescription(s).build()
        )
        /*
        try {
            tco.sendMessage(new EmbedBuilder().setColor(Color.CYAN).setTitle(eachWordUpperCase(msg.substring(9)) + " haben:").setDescription(String.join("\n", Jsoup.connect("https://www.pokewiki.de/" + msg.substring(9)).get().select("span[style=\"padding-left: 0.2em;\"]").stream().map(Element::text).collect(Collectors.toCollection(ArrayList::new)))).build()).queue();
        } catch (IOException e) {
            e.printStackTrace();
            tco.sendMessage("Es ist ein Fehler aufgetreten!").queue();
        }*/
    }
}