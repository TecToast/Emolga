package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import net.dv8tion.jda.api.EmbedBuilder
import java.awt.Color
import java.util.*

class AttackCommand : Command("attack", "Zeigt, welche Mons eine Attacke erlernen können", CommandCategory.Pokemon) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .addEngl("move", "Attacke", "Die Attacke, die angeschaut werden soll", Translation.Type.MOVE)
            .setExample("!attack Tarnsteine")
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        val ljson = learnsetJSON
        val djson = dataJSON
        val mons = ArrayList<String>()
        val atk = e.arguments!!.getTranslation("move").translation
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
                var name: String = if (s == "nidoranf") "Nidoran-F" else if (s == "nidoranm") "Nidoran-M" else {
                    val gerName = getGerName(s)
                    if (gerName.isSuccess) gerName.translation else {
                        getGerNameNoCheck(data.getString("baseSpecies")) + "-" + data.getString("forme")
                    }
                }
                //}
                /*String[] split = name.split("-");
                logger.info("name = " + name);
                if (split.length > 1) mons.add(getGerNameNoCheck(split[0]) + "-" + split[1]);
                else mons.add(getGerNameNoCheck(name));*/mons.add(name)
            }
        }
        mons.removeIf { obj: String? -> Objects.isNull(obj) }
        mons.sort()
        var s = StringBuilder()
        for (mon in mons) {
            s.append(mon).append("\n")
            if (s.length > 1900) {
                e.reply(
                    EmbedBuilder().setColor(Color.CYAN).setTitle(getGerNameNoCheck(atk) + " können:").setDescription(s)
                        .build()
                )
                s = StringBuilder()
            }
        }
        e.reply(
            EmbedBuilder().setColor(Color.CYAN).setTitle(getGerNameNoCheck(atk) + " können:").setDescription(s).build()
        )
    }
}