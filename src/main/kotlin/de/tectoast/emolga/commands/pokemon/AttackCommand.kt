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
        for ((s, data) in djson.entries) {
            if (s.endsWith("totem")) continue
            if (data.num <= 0) continue
            if (s !in ljson) continue
            val learnset = ljson[s]?.learnset ?: continue
            if (learnset.keys.contains(toSDName(atk))) {
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
