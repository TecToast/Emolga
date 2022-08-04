package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.draft.PickCommand.Companion.exec
import de.tectoast.emolga.utils.json.emolga.draft.League

class RandomPickCommand : Command("randompick", "Well... nen Random-Pick halt", CommandCategory.Draft) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("tier", "Tier", "Das Tier, in dem gepickt werden soll", ArgumentManagerTemplate.Text.any())
            .addEngl("type", "Typ", "Der Typ, von dem random gepickt werden soll", Translation.Type.TYPE, true)
            .setExample("!randompick A").build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val memberr = e.member
        val d = League.byChannel(e.textChannel, memberr.idLong, DraftEvent(e)) ?: return
        val tierlist = d.tierlist
        val args = e.arguments
        val tier = tierlist.order.firstOrNull { args.getText("tier").equals(it, ignoreCase = true) } ?: run {
            e.reply("Das ist kein Tier!")
            return
        }
        val list: MutableList<String> = tierlist.tierlist[tier]!!.toMutableList()
        list.shuffle()
        val typecheck: (String) -> Boolean = if (args.has("type")) {
            val type = args.getTranslation("type");
            {
                type.translation in dataJSON.getJSONObject(getSDName(it)).getStringList("types")
            }
        } else {
            { true }
        }
        exec(
            DraftEvent(e), "!pick " + (list.firstOrNull { str: String ->
                !d.isPicked(str) && typecheck(str)
            }?.trim() ?: ""), memberr, true
        )
    }
}