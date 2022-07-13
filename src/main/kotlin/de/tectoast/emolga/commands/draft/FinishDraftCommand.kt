package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.draft.Draft
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class FinishDraftCommand :
    Command("finishdraft", "Beendet für dich den Draft", CommandCategory.Draft, Constants.NDSID, Constants.ASLID) {
    init {
        aliases.add("finish")
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        val memberr = e.member
        val member = memberr.idLong
        val d = Draft.getDraftByMember(member, tco)
            ?: //tco.sendMessage(member.getAsMention() + " Du bist in keinem Draft drin!").queue();
            return
        val league = emolgaJSON.getJSONObject("drafts").getJSONObject(d.name)
        if (d.tc.id != tco.id) return
        if (d.isNotCurrent(member)) {
            tco.sendMessage(d.getMention(member) + " Du bist nicht dran!").queue()
            return
        }
        val mem = d.current
        if (e.guild.idLong == Constants.NDSID && d.picks[mem]!!.filter { it.name != "???" }.size < 15) {
            e.reply("Du hast noch keine 15 Pokemon!")
            return
        }
        e.reply("Du hast den Draft für dich beendet!")
        d.order.values.forEach(Consumer { l: MutableList<Long> -> l.removeIf { me: Long -> me == mem } })
        league.put("finished", league.optString("finished") + mem + ",")
        d.cooldown!!.cancel(false)
        if (d.order[d.round]!!.size == 0) {
            if (d.round == d.tierlist.rounds) {
                tco.sendMessage("Der Draft ist vorbei!").queue()
                d.ended = true
                //wooloodoc(tierlist, pokemon, d, mem, needed, null, num, round);
                saveEmolgaJSON()
                Draft.drafts.remove(d)
                return
            }
            d.round++
            if (d.order[d.round]!!.size == 0) {
                e.reply("Da alle bereits ihre Drafts beendet haben, ist der Draft vorbei!")
                saveEmolgaJSON()
                return
            }
            d.tc.sendMessage("Runde " + d.round + "!").queue()
            league.put("round", d.round)
        }
        d.current = d.order[d.round]!!.removeAt(0)
        league.put("current", d.current)
        d.cooldown!!.cancel(false)
        league.getJSONObject("picks").put(d.current, d.getTeamAsArray(d.current))
        if (d.isPointBased) //tco.sendMessage(getMention(current) + " (<@&" + asl.getLongList("roleids").get(getIndex(current.getIdLong())) + ">) ist dran! (" + points.get(current.getIdLong()) + " mögliche Punkte)").queue();
            tco.sendMessage(d.getMention(d.current) + " ist dran! (" + d.points[d.current] + " mögliche Punkte)")
                .queue() else tco.sendMessage(
            d.getMention(d.current) + " ist dran! (Mögliche Tiers: " + d.getPossibleTiersAsString(
                d.current
            ) + ")"
        ).queue()
        val delay = calculateDraftTimer()
        league.put("cooldown", System.currentTimeMillis() + delay)
        d.cooldown = d.scheduler.schedule({ d.timer() }, delay, TimeUnit.MILLISECONDS)
        saveEmolgaJSON()
    }
}