package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.embedColor
import dev.minn.jda.ktx.messages.Embed

class AttackCommand : Command("attack", "Zeigt, welche Mons eine Attacke erlernen können", CommandCategory.Pokemon) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .addEngl("move", "Attacke", "Die Attacke, die angeschaut werden soll", Translation.Type.MOVE)
            .setExample("!attack Tarnsteine").build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val ljson = learnsetJSON
        val djson = dataJSON
        val mons = ArrayList<String>()
        val atk = e.arguments.getTranslation("move").translation
        for (s in djson.keySet()) {
            if (s.endsWith("totem")) continue
            val data = djson.getJSONObject(s)
            if (data.getInt("num") <= 0) continue
            if (!ljson.has(s)) continue
            if (!ljson.getJSONObject(s).has("learnset")) continue
            if (ljson.getJSONObject(s).getJSONObject("learnset").keySet().contains(toSDName(atk))) {
                /*if (s.endsWith("alola")) name = getGerNameNoCheck(s.substring(0, s.length() - 5)) + "-Alola";
                else if (s.endsWith("galar")) name = getGerNameNoCheck(s.substring(0, s.length() - 5)) + "-Galar";
                else if (s.endsWith("unova")) name = getGerNameNoCheck(s.substring(0, s.length() - 5)) + "-Unova";
                else {*/
                //}
                /*String[] split = name.split("-");
                logger.info("name = " + name);
                if (split.length > 1) mons.add(getGerNameNoCheck(split[0]) + "-" + split[1]);
                else mons.add(getGerNameNoCheck(name));*/mons.add(
                    if (s == "nidoranf") "Nidoran-F" else if (s == "nidoranm") "Nidoran-M" else {
                        val gerName = getGerName(s)
                        if (gerName.isSuccess) gerName.translation else {
                            getGerNameNoCheck(data.getString("baseSpecies")) + "-" + data.getString("forme")
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
                    Embed(title = "${getGerNameNoCheck(atk)} können:", description = s.toString(), color = embedColor)
                )
                s = StringBuilder()
            }
        }
        e.reply(
            Embed(title = "${getGerNameNoCheck(atk)} können:", description = s.toString(), color = embedColor)
        )
    }
}