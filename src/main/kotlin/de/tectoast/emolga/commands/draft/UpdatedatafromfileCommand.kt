package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.draft.Draft
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.jsolf.JSONObject
import java.util.concurrent.TimeUnit

class UpdatedatafromfileCommand :
    Command("updatedatafromfile", "Aktualisiert die Daten auf Basis der Datei", CommandCategory.Flo) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("name", "Draftname", "Der Name des Drafts", ArgumentManagerTemplate.draft())
            .setExample("!updatedatafromfile Emolga-Conference")
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        val name = e.arguments!!.getText("name")
        val op = Draft.drafts.stream().filter { d: Draft -> d.name == name }.findFirst()
        if (op.isEmpty) {
            tco.sendMessage("Dieser Draft existiert nicht!").queue()
            return
        }
        val d = op.get()
        val league = emolgaJSON.getJSONObject("drafts").getJSONObject(name)
        val lround = league.getInt("round")
        if (d.round != lround) {
            d.tc.sendMessage("Runde $lround!").queue()
        }
        d.round = lround
        d.current = league.getLong("current")
        var x = 0
        for (mem in d.order[d.round]!!) {
            x++
            if (d.current == mem) break
        }
        if (x > 0) {
            d.order[d.round]!!.subList(0, x).clear()
        }
        val pick = league.getJSONObject("picks")
        for (mem in d.members) {
            if (pick.has(mem)) {
                val arr = pick.getJSONArray(mem)
                val monlist = ArrayList<DraftPokemon>()
                for (ob in arr) {
                    val obj = ob as JSONObject
                    monlist.add(DraftPokemon(obj.getString("name"), obj.getString("tier")))
                }
                d.picks[mem] = monlist
                d.update(mem)
            } else {
                d.picks[mem] = ArrayList()
            }
            if (d.isPointBased) {
                d.points[mem] = d.tierlist!!.points
                for (mon in d.picks[mem]!!) {
                    d.points[mem] = d.points[mem]!! - d.tierlist!!.prices[mon.tier]!!
                }
            }
        }
        d.cooldown!!.cancel(false)
        d.cooldown = d.scheduler.schedule({ d.timer() }, calculateDraftTimer(), TimeUnit.MILLISECONDS)
        e.done()
    }
}