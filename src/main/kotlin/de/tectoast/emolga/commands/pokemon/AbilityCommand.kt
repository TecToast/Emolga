package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.embedColor
import dev.minn.jda.ktx.messages.Embed

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

    override suspend fun process(e: GuildCommandEvent) {
        val json = dataJSON
        val mons = ArrayList<String>()
        val abi = e.arguments.getTranslation("abi").translation
        for ((s, data) in json.entries) {
            if (data.num < 0) continue
            if (data.abilities.values.any { it.equals(abi, ignoreCase = true) }
            ) {
                mons.add(
                    if (s == "nidoranf") "Nidoran-F" else if (s == "nidoranm") "Nidoran-M" else {
                        val gerName = getGerName(s)
                        if (gerName.isSuccess) gerName.translation else {
                            data.baseSpeciesAndForme
                        }
                    }
                )
            }
        }
        mons.sort()
        var s = StringBuilder()
        for (mon in mons) {
            s.append(mon).append("\n")
            if (s.length > 1900) {
                e.reply(
                    Embed(
                        title = "${e.arguments.getTranslation("abi").otherLang} haben:",
                        color = embedColor,
                        description = s.toString()
                    )
                )
                s = StringBuilder()
            }
        }
        e.reply(
            Embed(
                title = "${e.arguments.getTranslation("abi").otherLang} haben:",
                color = embedColor,
                description = s.toString()
            )
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
