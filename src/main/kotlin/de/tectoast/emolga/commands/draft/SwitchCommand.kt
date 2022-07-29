package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.draft.Draft
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.Tierlist
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

@Suppress("unused")
class SwitchCommand : Command("switch", "Switcht ein Pokemon", CommandCategory.Draft) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "oldmon",
                "Altes Mon",
                "Das Pokemon, was rausgeschmissen werden soll",
                ArgumentManagerTemplate.draftPokemon(),
                false,
                "Das, was du rauswerfen möchtest, ist kein Pokemon!"
            )
            .add(
                "newmon",
                "Neues Mon",
                "Das Pokemon, was stattdessen reinkommen soll",
                ArgumentManagerTemplate.draftPokemon(),
                false,
                "Das, was du haben möchtest, ist kein Pokemon!"
            )
            .setExample("!switch Gufa Emolga").build()
    }

    override fun process(e: GuildCommandEvent) {
        val msg = e.msg
        val tco = e.textChannel
        val memberr = e.member
        val member = memberr.idLong
        val d = Draft.getDraftByMember(member, tco)
        if (d == null) {
            tco.sendMessage(memberr.asMention + " Du bist in keinem Draft drin!").queue()
            return
        }
        if (d.tc.id != tco.id) return
        if (!d.isSwitchDraft) {
            e.reply("Dieser Draft ist kein Switch-Draft, daher wird !switch nicht unterstützt!")
            return
        }
        if (msg == "!switch") {
            e.reply("Willst du vielleicht noch zwei Pokemon dahinter schreiben? xD")
            return
        }
        if (d.isNotCurrent(member)) {
            tco.sendMessage(d.getMention(member) + " Du bist nicht dran!").queue()
            return
        }
        val mem = d.current
        val json = emolgaJSON
        val league = json.getJSONObject("drafts").getJSONObject(d.name)
        //JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject("ZBSL2");
        /*if (asl.has("allowed")) {
                JSONObject allowed = asl.getJSONObject("allowed");
                if (allowed.has(member.getId())) {
                    mem = d.tc.getGuild().retrieveMemberById(allowed.getString(member.getId())).complete();
                } else mem = member;
            } else mem = member;*/
        val args = e.arguments
        val oldmon = args.getText("oldmon")
        val newmon = args.getText("newmon")
        val tierlist = d.tierlist
        if (!d.isPickedBy(oldmon, mem)) {
            e.reply(memberr.asMention + " " + oldmon + " befindet sich nicht in deinem Kader!")
            return
        }
        if (d.isPicked(newmon)) {
            e.reply(memberr.asMention + " " + newmon + " wurde bereits gepickt!")
            return
        }
        val pointsBack = tierlist.getPointsNeeded(oldmon)
        if (pointsBack == -1) {
            e.reply("Das, was du rauswerfen möchtest, steht nicht in der Tierliste!")
            return
        }
        logger.info("oldmon = $oldmon")
        logger.info("newmon = $newmon")
        val newpoints = tierlist.getPointsNeeded(newmon)
        if (newpoints == -1) {
            e.reply("Das, was du haben möchtest, steht nicht in der Tierliste!")
            return
        }
        val tier = tierlist.getTierOf(newmon)
        if (d.isPointBased) {
            d.points[mem] = d.points[mem]!! + pointsBack
            if (d.points[mem]!! - newpoints < 0) {
                d.points[mem] = d.points[mem]!! - pointsBack
                e.reply(memberr.asMention + " Du kannst dir " + newmon + " nicht leisten!")
                return
            }
            d.points[mem] = d.points[mem]!! - newpoints
        } else {
            if ((d.getPossibleTiers(mem)[tier]!! < 0 || d.getPossibleTiers(mem)[tier] == 0) && tierlist.getTierOf(oldmon) != tier) {
                e.reply(memberr.asMention + " Du kannst dir kein " + tier + "-Tier mehr holen!")
                return
            }
        }
        val oldindex = AtomicInteger(-1)
        val draftPokemons = d.picks[mem]!!
        val dp =
            draftPokemons.firstOrNull { dp: DraftPokemon -> dp.name.equals(oldmon, ignoreCase = true) }
        if (dp == null) {
            logger.error("DRP NULL LINE 116 " + oldindex.get())
            return
        }
        dp.name = newmon
        dp.tier = tierlist.getTierOf(newmon)

        //m.delete().queue();
        //aslNoCoachDoc(tierlist, newmon, d, mem, tier, pointsBack - newpoints, oldMon, oldindex.get(), d.originalOrder.get(d.round).indexOf(mem));
        ndss3Doc(newmon, d, mem, oldmon)
        league.getJSONObject("picks").put(d.current, d.getTeamAsArray(d.current))
        if (newmon == "Emolga") {
            tco.sendMessage("<:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991>")
                .queue()
        }
        d.nextPlayer(tco, tierlist, league)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SwitchCommand::class.java)
        private fun aslNoCoachDoc(
            tierlist: Tierlist,
            pokemon: String,
            d: Draft,
            mem: Long,
            newtier: String,
            diff: Int,
            removed: DraftPokemon,
            monindex: Int,
            userindex: Int
        ) {
            val league = emolgaJSON.getJSONObject("drafts").getJSONObject(d.name)
            val sid = league.getString("sid")
            val b = RequestBuilder(sid)
            val oldmon = removed.name
            val cng = tierlist.getLocation(pokemon, 1, 5)
            val cog = tierlist.getLocation(oldmon, 1, 5)
            val cne = Tierlist.getLocation(pokemon, 1, 5, tierlist.tiercolumnsEngl)
            val coe = Tierlist.getLocation(oldmon, 1, 5, tierlist.tiercolumnsEngl)
            logger.info(MarkerFactory.getMarker("important"), "{} {} {} {}", cng, cog, cne, coe)
            if (cng.valid) b.addBGColorChange(
                league.getInt("tierlist"),
                cng.x shl 1,
                cng.y,
                convertColor(0xFF0000)
            )
            if (cog.valid) b.addBGColorChange(
                league.getInt("tierlist"),
                cog.x shl 1,
                cog.y,
                convertColor(0x93c47d)
            )
            if (cne.valid) b.addBGColorChange(
                league.getInt("tierlistengl"),
                cne.x shl 1,
                cne.y,
                convertColor(0xFF0000)
            )
            if (coe.valid) b.addBGColorChange(
                league.getInt("tierlistengl"),
                coe.x shl 1,
                coe.y,
                convertColor(0x93c47d)
            )
            b.addRow(
                "Teamseite RR!B${league.getLongList("table").indexOf(mem) * 15 + 4 + monindex}",
                listOf<Any>(
                    newtier,
                    pokemon,
                    dataJSON.getJSONObject(getSDName(pokemon)).getJSONObject("baseStats").getInt("spe")
                )
            )
            val round = d.round
            b.addRow(
                "Zwischendraft!${getAsXCoord(round * 5 - 2)}${userindex + 4}",
                listOf<Any>(oldmon, pokemon, diff)
            )
            b.execute()
        }

        private fun aslCoachDoc(
            tierlist: Tierlist,
            pokemon: String,
            d: Draft,
            mem: Long,
            needed: Int,
            removed: DraftPokemon
        ) {
            val asl = emolgaJSON.getJSONObject("drafts").getJSONObject("ASLS9")
            val league = asl.getJSONObject(d.name)
            val sid = asl.getString("sid")
            var x = 1
            var y = 5
            var found = false
            for (s in tierlist.tiercolumns) {
                if (s.equals(pokemon, ignoreCase = true)) {
                    found = true
                    break
                }
                //logger.info(s + " " + y);
                if (s == "NEXT") {
                    x++
                    y = 5
                } else y++
            }
            val b = RequestBuilder(sid)
            if (found) {
                b.addBGColorChange(league.getInt("tierlist"), x shl 1, y, convertColor(0xFF0000))
            }
            x = 1
            y = 5
            found = false
            for (s in tierlist.tiercolumns) {
                if (s.equals(removed.name, ignoreCase = true)) {
                    found = true
                    break
                }
                //logger.info(s + " " + y);
                if (s == "NEXT") {
                    x++
                    y = 5
                } else y++
            }
            if (found) b.addBGColorChange(league.getInt("tierlist"), x shl 1, y, convertColor(0x93c47d))

            //logger.info(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
            val team = asl.getStringList("teams")[Draft.getIndex(mem)]
            val picks = d.picks[mem]!!
            val index = picks.indexOfFirst {
                it.name == pokemon
            }
            val yc = Draft.getLevel(mem) * 20 + index + 1
            val list: MutableList<Int> = LinkedList()
            for (i in 0..9) {
                list.add(i * 5 + 10)
            }
            b.addRow(
                "$team!B$yc", listOf<Any>(getGen5Sprite(pokemon),
                    pokemon,
                    needed,
                    "=SUMME(" + list.joinToString(";") {
                        getAsXCoord(it) + yc
                    } + ")",
                    "=SUMME(" + list.joinToString(";") { i: Int -> getAsXCoord(i + 1) + yc } + ")",
                    "=E$yc - F$yc",
                    dataJSON.getJSONObject(getSDName(pokemon)).getJSONObject("baseStats").getInt("spe")))
            b.execute()
        }

        private fun fpldoc(
            tierlist: Tierlist,
            pokemon: String,
            d: Draft,
            mem: Long,
            tier: String,
            num: Int,
            removed: String
        ) {
            val league = emolgaJSON.getJSONObject("drafts").getJSONObject(d.name)
            if (league.has("sid")) {
                val doc = league.getString("sid")
                logger.info("num = $num")
                val b = RequestBuilder(doc)
                val ncoords = tierlist.getLocation(pokemon, 1, 3)
                b.addStrikethroughChange(league.getInt("tierlist"), ncoords.x shl 1, ncoords.y, true)
                val ocoords = tierlist.getLocation(removed, 1, 3)
                b.addStrikethroughChange(league.getInt("tierlist"), ocoords.x shl 1, ocoords.y, false)
                //logger.info(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
                b.addStrikethroughChange(league.getInt("draftorder"), d.round + 1, num + 6, true)
                val user = league.getLongList("table").indexOf(mem)
                val range = "Kader ${d.name.substring(5)}!${getAsXCoord(user / 4 * 22 + 2)}${
                    user % 4 * 20 + 8 + d.picks[mem]!!.indexOfFirst { dp: DraftPokemon -> dp.name == pokemon }
                }"
                logger.info("range = $range")
                b.addRow(
                    range,
                    listOf<Any>(
                        tier,
                        "",
                        pokemon,
                        "",
                        dataJSON.getJSONObject(getSDName(pokemon)).getJSONObject("baseStats").getInt("spe")
                    )
                )
                logger.info("d.members.size() = " + d.members.size)
                logger.info("d.order.size() = " + d.order[d.round]!!.size)
                logger.info("d.members.size() - d.order.size() = " + (d.members.size - d.order[d.round]!!.size))
                //if (d.members.size() - d.order.get(d.round).size() != 1 && isEnabled)
                b.execute()
            }
        }

        private fun ndss3Doc(pokemon: String, d: Draft, mem: Long, removed: String) {
            val league = emolgaJSON.getJSONObject("drafts").getJSONObject(d.name)

            //logger.info(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
            val b = RequestBuilder(league.getString("sid"))
            val teamname = league.getJSONObject("teamnames").getString(mem)
            val sdName = getSDName(pokemon)
            dataJSON.getJSONObject(sdName)
            val picks = d.picks.getValue(mem)
            val y = league.getStringList("table").indexOf(teamname) * 17 + 2 +
                    picks.indexOfFirst { it.name == pokemon }
            b.addSingle("Data!B$y", pokemon)
            b.addSingle("Data!AF$y", 2)
            b.addSingle("Data!Q$y", "=SUMME(L$y:P$y)")
            b.addSingle("Data!AC$y", "=SUMME(X$y:AB$y)")
            val tiers = listOf("S", "A", "B")
            b.addColumn("Data!F${league.getStringList("table").indexOf(teamname) * 17 + 2}", picks
                .asSequence()
                .sortedWith(compareBy({ tiers.indexOf(it.tier) }, { it.name }))
                .map { it.name }
                .toList())
            val numInRound = d.originalOrder[d.round]!!.indexOf(mem) + 1
            b.addSingle("Draft!${getAsXCoord(d.round * 5 - 1)}${numInRound * 5 + 2}", pokemon)
            b.addSingle("Draft!${getAsXCoord(d.round * 5 - 3)}${numInRound * 5 + 1}", removed)
            logger.info("d.members.size() = " + d.members.size)
            logger.info("d.order.size() = " + d.order[d.round]!!.size)
            logger.info("d.members.size() - d.order.size() = $numInRound")
            //if (d.members.size() - d.order.get(d.round).size() != 1 && isEnabled)
            b.execute()
        }
    }
}