package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.draft.PickCommand.Companion.exec
import de.tectoast.emolga.utils.draft.Draft
import java.util.function.Predicate

class RandomPickCommand : Command("randompick", "Well... nen Random-Pick halt", CommandCategory.Draft) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("tier", "Tier", "Das Tier, in dem gepickt werden soll", ArgumentManagerTemplate.Text.any())
            .addEngl("type", "Typ", "Der Typ, von dem random gepickt werden soll", Translation.Type.TYPE, true)
            .setExample("!randompick A")
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        val memberr = e.member
        val member = memberr.idLong
        val tco = e.textChannel
        val d = Draft.getDraftByMember(member, tco)
        if (d == null) {
            e.textChannel.sendMessage("Du Kek der Command funktioniert nur in einem Draft xD").queue()
            return
        }
        val tierlist = d.tierlist!!
        val args = e.arguments!!
        val tier = tierlist.order.stream().filter { s: String? -> args.getText("tier").equals(s, ignoreCase = true) }
            .findFirst().orElse("")
        if (tier.isEmpty()) {
            tco.sendMessage("Das ist kein Tier!").queue()
            return
        }
        if (d.tc.id != tco.id) return
        if (d.isNotCurrent(member)) {
            tco.sendMessage(d.getMention(member) + " Du bist nicht dran!").queue()
            return
        }
        val mem = d.current
        val list: MutableList<String> = tierlist.tierlist[tier]!!.toMutableList()
        list.shuffle()
        val typecheck: Predicate<String> = if (args.has("type")) {
            val type = args.getTranslation("type")
            Predicate { str: String ->
                dataJSON.getJSONObject(getSDName(str)).getJSONArray("types").toList().contains(type.translation)
            }
        } else {
            Predicate { true }
        }
        exec(
            tco,
            "!pick " + list.stream().filter { str: String ->
                !d.isPicked(str) && !d.hasInAnotherForm(
                    mem,
                    str
                ) && (!d.hasMega(mem) || !str.startsWith("M-")) && typecheck.test(str)
            }
                .map { obj: String? -> obj!!.trim() }.findFirst().orElse(""),
            memberr,
            true
        )
    }
}