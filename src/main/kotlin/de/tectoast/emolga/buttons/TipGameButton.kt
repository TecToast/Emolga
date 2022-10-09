package de.tectoast.emolga.buttons

import de.tectoast.emolga.commands.saveEmolgaJSON
import de.tectoast.emolga.utils.json.Emolga
import de.tectoast.emolga.utils.json.emolga.draft.GamedayData
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class TipGameButton : ButtonListener("tipgame") {
    override suspend fun process(e: ButtonInteractionEvent, name: String) {
        //primary("tipgame;${league.name}:$index:${u1.indexedBy(table)}", names[u1]),
        val split = name.split(":")
        val (gameday, index, userindex) = split.drop(1).map { it.toInt() }
        val league = Emolga.get.drafts[split[0]] ?: run {
            e.reply("Dieses Tippspiel existiert nicht mehr!").setEphemeral(true).queue()
            return
        }
        val tipgame = league.tipgame!!
        val usermap =
            tipgame.tips.getOrPut(gameday) { GamedayData() }.userdata.getOrPut(e.user.idLong) { mutableMapOf() }
        usermap[index] = userindex
        e.reply("Dein Tipp wurde gespeichert!").setEphemeral(true).queue()
        saveEmolgaJSON()
    }
}