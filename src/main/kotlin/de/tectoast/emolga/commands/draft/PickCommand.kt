package de.tectoast.emolga.commands.draft

import com.google.api.services.sheets.v4.model.*
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.draft.Draft
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.jsolf.JSONObject
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.slf4j.LoggerFactory
import java.util.*
import java.util.stream.Collectors

class PickCommand : Command("pick", "Pickt das Pokemon", CommandCategory.Draft) {
    init {
        //setArgumentTemplate(ArgumentManagerTemplate.noSpecifiedArgs("!pick <Pokemon> [Optionales Tier]", "!pick Emolga"));
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "pokemon",
                "Pokemon",
                "Das Pokemon, was du picken willst",
                ArgumentManagerTemplate.draftPokemon { s: String ->
                    val lol = Tierlist.getByGuild(Constants.FPLID)!!.tierlist["lol"]!!
                    val strings = lol.stream().filter { str: String ->
                        str.lowercase(Locale.getDefault()).startsWith(s.lowercase(Locale.getDefault()))
                    }
                        .toList()
                    if (strings.size > 25) return@draftPokemon emptyList<String>()
                    strings
                },
                false,
                "Das ist kein Pokemon!"
            ) //.add("tier", "Tier", "Das Tier", ArgumentManagerTemplate.Text.any(), true)
            .setExample("!pick Emolga")
            .build()
        slash(false, Constants.FPLID)
    }

    override fun process(e: GuildCommandEvent) {
        val msg: String
        var sc: SlashCommandInteractionEvent? = null
        if (e.isSlash) {
            sc = e.slashCommandEvent
            msg = "!pick ${sc!!.getOption("pokemon")!!.asString}"
        } else {
            msg = e.msg!!
        }
        exec(e.textChannel, msg, e.member, false, sc)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PickCommand::class.java)

        @JvmOverloads
        fun exec(
            tco: TextChannel,
            msg: String,
            memberr: Member,
            isRandom: Boolean,
            slashEvent: SlashCommandInteractionEvent? = null,
        ) {
            try {
                val member = memberr.idLong
                if (msg.trim() == "!pick") {
                    if (isRandom) {
                        tco.sendMessage("Jedes Pokemon aus dem Tier mit dem Typen ist bereits weg!").queue()
                    } else {
                        tco.sendMessage("Willst du vielleicht noch ein Pokemon dahinter schreiben? xD").queue()
                    }
                    return
                }
                val d = Draft.getDraftByMember(member, tco)
                if (d == null) {
                    tco.sendMessage(memberr.asMention + " Du bist in keinem Draft drin!").queue()
                    return
                }
                if (d.tc.id != tco.id) return
                /*if (d.isSwitchDraft) {
                tco.sendMessage("Dieser Draft ist ein Switch-Draft, daher wird !pick nicht unterstützt!").queue();
                return;
            }*/if (d.isNotCurrent(member)) {
                    tco.sendMessage(d.getMention(member) + " Du bist nicht dran!").queue()
                    return
                }
                val mem = d.current
                val league = emolgaJSON.getJSONObject("drafts").getJSONObject(d.name)
                val split = msg.substring(6).split(" ".toRegex())
                val tier: String
                val t: Translation
                var pokemon: String
                val tierlist = d.tierlist!!
                if (d.picks[mem]!!.size == 15) {
                    tco.sendMessage("Du hast bereits 15 Mons!").queue()
                    return
                }
                if (split.size == 2 && !d.isPointBased) {
                    t = getDraftGerName(split[0])
                    if (!t.isFromType(Translation.Type.POKEMON)) {
                        tco.sendMessage("Das ist kein Pokemon!").queue()
                        return
                    }
                    pokemon = t.translation
                    tier = tierlist.order.stream().filter { s: String? -> split[1].equals(s, ignoreCase = true) }
                        .findFirst().orElse("")
                } else {
                    t = getDraftGerName(msg.substring(6))
                    if (!t.isFromType(Translation.Type.POKEMON)) {
                        tco.sendMessage("Das ist kein Pokemon!").queue()
                        return
                    }
                    pokemon = t.translation
                    tier = tierlist.getTierOf(pokemon)
                }
                if (d.isPicked(pokemon)) {
                    //tco.sendMessage(member.getAsMention() + " Junge bist du scheiße oder was?! (Dieses Pokemon wurde bereits gepickt!)").queue();
                    tco.sendMessage(memberr.asMention + " Dieses Pokemon wurde bereits gepickt!").queue()
                    return
                }
                val needed = tierlist.getPointsNeeded(pokemon)
                if (d.isPointBased) {
                    if (needed == -1) {
                        tco.sendMessage(memberr.asMention + " Das Pokemon steht nicht in der Tierliste!").queue()
                        return
                    }
                } else {
                    val origtier = tierlist.getTierOf(pokemon)
                    if (origtier.isEmpty()) {
                        tco.sendMessage(memberr.asMention + " Das ist kein Pokemon!").queue()
                        return
                    }
                    if (tierlist.order.indexOf(origtier) < tierlist.order.indexOf(tier)) {
                        tco.sendMessage("Du kannst ein $origtier-Mon nicht ins $tier hochdraften!").queue()
                        return
                    }
                    val map = d.getPossibleTiers(mem)
                    if (!map.containsKey(tier)) {
                        tco.sendMessage("Das Tier `$tier` existiert nicht!").queue()
                        return
                    }
                    if (map[tier]!! <= 0) {
                        if (tierlist.prices[tier] == 0) {
                            tco.sendMessage("Ein Pokemon aus dem $tier-Tier musst du in ein anderes Tier hochdraften!")
                                .queue()
                            return
                        }
                        tco.sendMessage("Du kannst dir kein $tier-Pokemon mehr picken!").queue()
                        return
                    }
                }
                pokemon = tierlist.getNameOf(pokemon)
                /*if (d.hasMega(mem) && pokemon.startsWith("M-")) {
                tco.sendMessage(member.getAsMention() + " Du hast bereits ein Mega!").complete().getId();
                return;
            }
            / *if (d.hasInAnotherForm(mem, pokemon)) {
                tco.sendMessage(member.getAsMention() + " Damit würdest du gegen die Species Clause verstoßen!").queue();
                return;
            }*/if (d.isPointBased && d.points[mem]!! - needed < 0) {
                    tco.sendMessage(memberr.asMention + " Dafür hast du nicht genug Punkte!").queue()
                    return
                }
                /*if (d.isPointBased && (d.getTierlist().rounds - d.round) * d.getTierlist().prices.get(d.getTierlist().order.get(d.getTierlist().order.size() - 1)) > (d.points.get(mem) - needed)) {
                tco.sendMessage(memberr.getAsMention() + " Wenn du dir dieses Pokemon holen würdest, kann dein Kader nicht mehr vervollständigt werden!").queue();
                return;
            }*/if (d.isPointBased) d.points[mem] = d.points[mem]!! - needed
                d.picks[mem]!!.add(DraftPokemon(pokemon, tier))
                if (!league.has("picks")) league.put("picks", JSONObject())
                league.getJSONObject("picks").put(mem, d.getTeamAsArray(mem))
                //m.delete().queue();
                d.update(mem)
                slashEvent?.reply("${slashEvent.member!!.effectiveName} hat $pokemon gepickt!")?.queue()
                if (isRandom) {
                    tco.sendMessage("**<@$mem>** hat aus dem $tier-Tier ein **$pokemon** bekommen!").queue()
                } else if (pokemon == "Emolga") {
                    tco.sendMessage("<:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991>")
                        .queue()
                }
                //zbsdoc(tierlist, pokemon, d, mem, tier, d.members.size() - d.order.get(d.round).size(), d.round);
                //fpldoc(tierlist, pokemon, d, mem, tier, d.members.size() - d.order.get(d.round).size(), d.round);
                //woolooDoc(tierlist, pokemon, d, mem, tier, d.round);
                //int rd = d.round == tierlist.rounds && d.picks.get(mem).size() < tierlist.rounds ? (int) league.getJSONObject("skippedturns").getJSONArrayL(mem).remove(0) : d.round;
                //aslS10Doc(tierlist, pokemon, d, mem, tier, rd);
                //ndsdoc(tierlist, pokemon, d, mem, tier);
                //uplDoc(pokemon, league, d, d.members.size() - d.order.get(d.round).size(), mem);
                ndss3Doc(pokemon, d, mem)
                /*if (d.round == tierlist.rounds && d.picks.get(mem).size() < d.round) {
                if (d.isPointBased)
                    //tco.sendMessage(getMention(current) + " (<@&" + asl.getLongList("roleids").get(getIndex(current.getIdLong())) + ">) ist dran! (" + points.get(current.getIdLong()) + " mögliche Punkte)").queue();
                    tco.sendMessage(d.getMention(mem) + " ist dran! (" + d.points.get(mem) + " mögliche Punkte)").queue();
                else
                    tco.sendMessage(d.getMention(mem) + " ist dran! (Mögliche Tiers: " + d.getPossibleTiersAsString(mem) + ")").queue();
            } else {*/d.nextPlayer(tco, tierlist, league)
                //}
                //aslCoachDoc(tierlist, pokemon, d, mem, needed, round, toremove);
                //ndsdoc(tierlist, pokemon, d, mem, tier, round);
            } catch (ex: Exception) {
                tco.sendMessage("Es ist ein Fehler aufgetreten!").queue()
                slashEvent?.reply("Da ist wohl was schiefgelaufen :(")?.setEphemeral(true)?.queue()
                ex.printStackTrace()
            }
        }

        private fun ndss3Doc(pokemon: String, d: Draft, mem: Long) {
            val league = emolgaJSON.getJSONObject("drafts").getJSONObject(d.name)

            //logger.info(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
            val b = RequestBuilder(league.getString("sid"))
            val teamname = league.getJSONObject("teamnames").getString(mem)
            val picks = d.picks.getValue(mem)
            val y = league.getStringList("table").indexOf(teamname) * 17 + 1 + picks.size
            b.addSingle("Data!B$y", pokemon)
            b.addSingle("Data!AF$y", 2)
            val tiers = listOf("S", "A", "B")
            b.addColumn("Data!F${league.getStringList("table").indexOf(teamname) * 17 + 2}", picks
                .asSequence()
                .sortedWith(compareBy({ tiers.indexOf(it.tier) }, { it.name }))
                .map { it.name }
                .toList())
            val numInRound = d.originalOrder.getValue(d.round).indexOf(mem) + 1
            b.addSingle("Draft!${getAsXCoord(d.round * 5 - 1)}${numInRound * 5 + 2}", pokemon)
            logger.info("d.members.size() = ${d.members.size}")
            logger.info("d.order.size() = ${d.order.getValue(d.round).size}")
            logger.info("d.members.size() - d.order.size() = $numInRound")
            //if (d.members.size() - d.order.get(d.round).size() != 1 && isEnabled)
            b.execute()
        }

        private fun uplDoc(pokemon: String, league: JSONObject, d: Draft, num: Int, mem: Long) {
            val sid = league.getString("sid")
            val b = RequestBuilder(sid)
            val round = d.round - 1
            b.addStrikethroughChange(league.getInt("draftorder"), round % 6 + 2, round / 6 * 12 + num + 6, true)
            val table = league.getLongList("table")
            val index = table.indexOf(mem)
            b.addSingle(
                "Kader!${getAsXCoord((index % 3 shl 3) + 2)}${index / 3 * 20 + 7 + d.picks[mem]!!.size}", pokemon
            )
            b.execute()
        }

        private fun aslS10Doc(
            tierlist: Tierlist,
            pokemon: String,
            d: Draft,
            mem: Long,
            tier: String,
            effectiveRound: Int,
        ) {
            val league = emolgaJSON.getJSONObject("drafts").getJSONObject(d.name)
            val sid = league.getString("sid")
            val b = RequestBuilder(sid)
            val c = tierlist.getLocation(pokemon, 0, 0)
            logger.info("c.toString() = {}", c)
            logger.info("c.valid() = {}", c.valid)
            if (c.valid) b.addBGColorChange(
                league.getInt("tierlist"),
                (c.x shl 1) + 2,
                c.y + 5,
                convertColor(0xFF0000)
            )
            val cengl = Tierlist.getLocation(pokemon, 0, 0, tierlist.tiercolumnsEngl)
            if (cengl.valid) b.addBGColorChange(
                league.getInt("tierlistengl"),
                (cengl.x shl 1) + 2,
                cengl.y + 5,
                convertColor(0xFF0000)
            )
            val finalComp = Comparator.comparing { p: DraftPokemon -> tierlist.order.indexOf(p.tier) }
                .thenComparing { p: DraftPokemon -> p.name }
            b.addAll("Teamseite HR!B${league.getLongList("table").indexOf(mem) * 15 + 4}",
                d.picks[mem]!!.stream().sorted(finalComp).map { mon: DraftPokemon ->
                    listOf(
                        mon.tier, mon.name, dataJSON.getJSONObject(
                            getSDName(mon.name)
                        ).getJSONObject("baseStats").getInt("spe")
                    )
                }
                    .collect(Collectors.toList()))
            val rr = effectiveRound - 1
            logger.info("d.originalOrder = {}", d.originalOrder)
            logger.info("effectiveRound = {}", effectiveRound)
            logger.info("mem = {}", mem)
            val index = d.originalOrder[effectiveRound]!!.indexOf(mem)
            logger.info("index = {}", index)
            b.addRow(
                "Draft!${getAsXCoord((rr % 6 shl 2) + 3)}${rr / 6 * 10 + 4 + index}",
                listOf(pokemon, tierlist.prices.getValue(tier)!!)
            )
            b.execute()
        }

        private fun woolooDoc(tierlist: Tierlist, pokemon: String, d: Draft, mem: Long, tier: String) {
            val league = emolgaJSON.getJSONObject("drafts").getJSONObject("WoolooCupS4")
            val sid = league.getString("sid")
            var x = 1
            var y = 4
            var found = false
            for (s in tierlist.tiercolumns) {
                if (s.equals(pokemon, ignoreCase = true)) {
                    found = true
                    break
                }
                //logger.info(s + " " + y);
                if (s == "NEXT") {
                    x++
                    y = 4
                } else y++
            }
            val b = RequestBuilder(sid)
            if (found) {
                b.addStrikethroughChange(league.getInt("tierlist"), x shl 1, y, true)
            }
            //logger.info(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
            var lea = ""
            var num = -1
            for (i in 1..2) {
                val lists = league.getJSONArray("table$i").toLongListList()
                val `in` =
                    lists.stream().filter { l: List<Long> -> l.contains(mem) }.map { o: List<Long> -> lists.indexOf(o) }
                        .findFirst().orElse(-1)
                if (`in` == -1) continue
                lea = if (i == 1) "Sonne" else "Hagel"
                num = `in`
            }
            b.addRow(
                "Teamseite $lea!C${
                    num * 15L + 3 +
                            (tierlist.order.indexOf(tier) * 3L + d.picks[mem]!!
                                .stream().filter { p: DraftPokemon -> p.tier == tier }.count())
                }",
                listOf<Any>(
                    pokemon,
                    dataJSON.getJSONObject(getSDName(pokemon)).getJSONObject("baseStats").getInt("spe")
                )
            )
            val rr = d.round - 1
            b.addSingle(
                "Draftreihenfolge!${getAsXCoord(rr % 6 * 3 + 3)}${rr / 6 * 14 + 2 + (12 - d.order[d.round]!!.size)}",
                pokemon
            )
            b.execute()
        }

        private fun aslCoachDoc(
            tierlist: Tierlist,
            pokemon: String,
            d: Draft,
            mem: Member,
            needed: Int,
            removed: DraftPokemon?,
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
                b.addBGColorChange(league.getInt("tierlist"), x shl 1, y, convertColor(0xff0000))
            }
            x = 1
            y = 5
            if (removed != null) {
                for (s in tierlist.tiercolumns) {
                    if (s.equals(removed.name, ignoreCase = true)) {
                        break
                    }
                    //logger.info(s + " " + y);
                    if (s == "NEXT") {
                        x++
                        y = 5
                    } else y++
                }
                /*Request request = new Request();
            request.setUpdateCells(new UpdateCellsRequest().setRows(Collections.singletonList(new RowData()
                            .setValues(Collections.singletonList(new CellData()
                                    .setUserEnteredFormat(new CellFormat()
                                            .setBackgroundColor(new Color()
                                                    .setRed((float) 0.5764706).setGreen((float) 0.76862746).setBlue((float) 0.49019608)))))))
                    .setFields("userEnteredFormat.backgroundColor").setRange(new GridRange().setSheetId(league.getInt("tierlist")).setStartRowIndex(y).setEndRowIndex(y + 1).setStartColumnIndex(x * 2 - 1).setEndColumnIndex(x * 2)));
            b.addBatch(request);*/b.addBGColorChange(league.getInt("tierlist"), x shl 1, y, convertColor(0x93c47d))
            }
            //logger.info(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
            val team = asl.getStringList("teams")[Draft.getIndex(mem.idLong)]
            val yc = Draft.getLevel(mem.idLong) * 20 + d.picks[mem.idLong]!!.size
            val list: MutableList<Int> = LinkedList()
            for (i in 0..8) {
                list.add((i shl 2) + 10)
            }
            b.addRow(
                "$team!B$yc", listOf<Any>(getGen5Sprite(pokemon),
                    pokemon,
                    needed,
                    "=" + list.stream().map { i: Int? ->
                        getAsXCoord(
                            i!!
                        ) + yc
                    }.collect(Collectors.joining(" + ")),
                    "=" + list.stream().map { i: Int -> getAsXCoord(i + 1) + yc }
                        .collect(Collectors.joining(" + ")),
                    "=E$yc - F$yc",
                    dataJSON.getJSONObject(getSDName(pokemon)).getJSONObject("baseStats").getInt("spe")))
            b.execute()
        }

        fun ndsdoc(tierlist: Tierlist, pokemon: String, d: Draft, mem: Long) {
            val league = emolgaJSON.getJSONObject("drafts").getJSONObject(d.name)

            //logger.info(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
            val b = RequestBuilder(league.getString("sid"))
            val teamname = league.getJSONObject("teamnames").getString(mem)
            val sdName = getSDName(pokemon)
            val o = dataJSON.getJSONObject(sdName)
            val i = d.picks[mem]!!.size + 14
            val tl = tierlist.getLocation(pokemon, 0, 0)
            val gen5Sprite = getGen5Sprite(o)
            b
                .addSingle("$teamname!B$i", gen5Sprite)
                .addSingle("$teamname!D$i", pokemon)
                .addSingle("Tierliste!" + getAsXCoord(tl.x * 6 + 6) + (tl.y + 4), "='$teamname'!B2")
            val t: MutableList<Any> =
                o.getStringList("types").stream().map { s: String? -> typeIcons.getString(s) }
                    .collect(
                        Collectors.toCollection { LinkedList() }
                    )
            if (t.size == 1) t.add("/")
            b.addRow("$teamname!F$i", t)
            b.addSingle("$teamname!H$i", o.getJSONObject("baseStats").getInt("spe"))
            val pointsNeeded = tierlist.getPointsNeeded(pokemon)
            b.addSingle("$teamname!I$i", pointsNeeded)
            b.addSingle("$teamname!J$i", "2")
            b.addRow(
                "$teamname!L$i",
                listOf<Any>(
                    canLearnNDS(sdName, "stealthrock"),
                    canLearnNDS(sdName, "defog"),
                    canLearnNDS(sdName, "rapidspin"),
                    canLearnNDS(sdName, "voltswitch", "uturn", "flipturn", "batonpass", "teleport")
                )
            )
            val numInRound = d.originalOrder[d.round]!!.indexOf(mem) + 1
            b.addSingle("Draft!${getAsXCoord(d.round * 5 - 3)}${numInRound * 5 + 2}", "《《《《")
                .addSingle("Draft!${getAsXCoord(d.round * 5 - 1)}${numInRound * 5 + 2}", pokemon)
                .addSingle("Draft!${getAsXCoord(d.round * 5)}${numInRound * 5 + 1}", gen5Sprite)
                .addSingle("Draft!${getAsXCoord(d.round * 5)}${numInRound * 5 + 3}", pointsNeeded)
            logger.info("d.members.size() = " + d.members.size)
            logger.info("d.order.size() = " + d.order[d.round]!!.size)
            logger.info("d.members.size() - d.order.size() = $numInRound")
            //if (d.members.size() - d.order.get(d.round).size() != 1 && isEnabled)
            b.execute()
        }

        private fun fpldoc(tierlist: Tierlist, pokemon: String, d: Draft, mem: Member, tier: String, num: Int) {
            val league = emolgaJSON.getJSONObject("drafts").getJSONObject(d.name)
            if (league.has("sid")) {
                val doc = league.getString("sid")
                var x = 1
                var y = 3
                for (s in tierlist.tiercolumns) {
                    if (s.equals(pokemon, ignoreCase = true)) break
                    //logger.info(s + " " + y);
                    if (s == "NEXT") {
                        x++
                        y = 3
                    } else y++
                }
                logger.info("num = $num")
                val b = RequestBuilder(doc)
                b.addStrikethroughChange(league.getInt("tierlist"), x shl 1, y, true)
                //logger.info(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
                b.addStrikethroughChange(league.getInt("draftorder"), d.round + 1, num + 6, true)
                val user = league.getLongList("table").indexOf(mem.idLong)
                val range =
                    "Kader ${d.name.substring(5)}!${getAsXCoord(user / 4 * 22 + 2)}${user % 4 * 20 + 7 + d.picks[mem.idLong]!!.size}"
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

        private fun zbsdoc(tierlist: Tierlist, pokemon: String, d: Draft, mem: Member, tier: String, num: Int) {
            val league = emolgaJSON.getJSONObject("drafts").getJSONObject(d.name)
            if (league.has("sid")) {
                val doc = league.getString("sid")
                var x = 1
                var y = 2
                for (s in tierlist.tiercolumns) {
                    if (s.equals(pokemon, ignoreCase = true)) break
                    //logger.info(s + " " + y);
                    if (s == "NEXT") {
                        x++
                        y = 2
                    } else y++
                }
                logger.info("num = $num")
                val b = RequestBuilder(doc)
                b.addStrikethroughChange(910228334, (x shl 1) + 1, y, true)
                //logger.info(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
                b.addStrikethroughChange(856868721, d.round + 2, num + 2, true)
                val user = league.getLongList("table").indexOf(mem.idLong)
                val range = "Liga 2!" + getAsXCoord((if (tier == "S") 12 + d.picks[mem.idLong]!!
                    .stream().filter { p: DraftPokemon -> p.tier == "S" }
                    .count() else tierlist.order.indexOf(tier) * 3 + 11 + d.picks[mem.idLong]!!
                    .stream().filter { p: DraftPokemon -> p.tier == tier }.count()).toInt()
                ) + (user + 3)
                logger.info("range = $range")
                b.addSingle(range, getGen5Sprite(pokemon))
                logger.info("d.members.size() = " + d.members.size)
                logger.info("d.order.size() = " + d.order[d.round]!!.size)
                logger.info("d.members.size() - d.order.size() = " + (d.members.size - d.order[d.round]!!.size))
                //if (d.members.size() - d.order.get(d.round).size() != 1 && isEnabled)
                b.execute()
            }
        }

        /*public void doc(Tierlist tierlist, String pokemon, Draft d, Member mem, String tier, JSONObject league, int pk) {
        zbsdoc(tierlist, pokemon, d, mem, tier, num);
        //aslnocoachdoc(tierlist, pokemon, d, mem, tier, league, pk);
    }*/
        private fun woolooolddoc(
            tierlist: Tierlist,
            pokemon: String,
            d: Draft,
            mem: Member,
            removed: DraftPokemon?,
            num: Int,
            round: Int,
        ) {
            val league = emolgaJSON.getJSONObject("drafts").getJSONObject(d.name)
            val sid = league.getString("sid")
            var x = 1
            var y = 2
            var found = false
            for (s in tierlist.tiercolumns) {
                if (s.equals(pokemon, ignoreCase = true)) {
                    found = true
                    break
                }
                //logger.info(s + " " + y);
                if (s == "NEXT") {
                    x++
                    y = 2
                } else y++
            }
            val b = RequestBuilder(sid)
            if (found) {
                val request = Request()
                request.updateCells = UpdateCellsRequest().setRows(
                    listOf(
                        RowData()
                            .setValues(
                                listOf(
                                    CellData()
                                        .setUserEnteredFormat(
                                            CellFormat()
                                                .setBackgroundColor(Color().setRed(1f))
                                        )
                                )
                            )
                    )
                )
                    .setFields("userEnteredFormat.backgroundColor").setRange(
                        GridRange().setSheetId(league.getInt("tierlist")).setStartRowIndex(y).setEndRowIndex(y + 1)
                            .setStartColumnIndex((x shl 1) - 1).setEndColumnIndex(x shl 1)
                    )
                b.addBatch(request)
            }
            x = 1
            y = 2
            if (removed != null) {
                for (s in tierlist.tiercolumns) {
                    if (s.equals(removed.name, ignoreCase = true)) {
                        break
                    }
                    //logger.info(s + " " + y);
                    if (s == "NEXT") {
                        x++
                        y = 2
                    } else y++
                }
                val request = Request()
                request.updateCells = UpdateCellsRequest().setRows(
                    listOf(
                        RowData()
                            .setValues(
                                listOf(
                                    CellData()
                                        .setUserEnteredFormat(
                                            CellFormat()
                                                .setBackgroundColor(
                                                    Color()
                                                        .setRed(0.5764706.toFloat()).setGreen(0.76862746.toFloat())
                                                        .setBlue(0.49019608.toFloat())
                                                )
                                        )
                                )
                            )
                    )
                )
                    .setFields("userEnteredFormat.backgroundColor").setRange(
                        GridRange().setSheetId(league.getInt("tierlist")).setStartRowIndex(y).setEndRowIndex(y + 1)
                            .setStartColumnIndex((x shl 1) - 1).setEndColumnIndex(x shl 1)
                    )
                b.addBatch(request)
            }
            val req = Request()
            //logger.info(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
            req.updateCells = UpdateCellsRequest().setRows(
                listOf(
                    RowData()
                        .setValues(
                            listOf(
                                CellData()
                                    .setUserEnteredFormat(
                                        CellFormat()
                                            .setTextFormat(TextFormat().setStrikethrough(true))
                                    )
                            )
                        )
                )
            )
                .setFields("userEnteredFormat.textFormat.strikethrough").setRange(
                    GridRange().setSheetId(1316641169).setStartRowIndex(num + 1).setEndRowIndex(num + 2)
                        .setStartColumnIndex(round).setEndColumnIndex(round + 1)
                )
            //logger.info(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
            b.addBatch(req)
            val user = listOf(*league.getString("table").split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()).indexOf(mem.id)
            val picks = d.picks[mem.idLong]!!
            for (i in 0..12) {
                val list: MutableList<Any> = ArrayList()
                if (i < picks.size) {
                    val mon = picks[i]
                    list.add(tierlist.prices[mon.tier]!!)
                    list.add(mon.name)
                } else {
                    list.add("")
                    list.add("")
                }
                b.addRow(
                    "Teamübersicht!" + getAsXCoord((if (user > 3) user - 4 else user) * 6 + 2) + ((if (user > 3) 25 else 7) + i),
                    list
                )
            }
            b.execute()
        }

        private fun aslnocoachdoc(tierlist: Tierlist, pokemon: String, d: Draft, mem: Member, removed: DraftPokemon?) {
            val league = emolgaJSON.getJSONObject("drafts").getJSONObject(d.name)
            val sid = league.getString("sid")
            var x = 1
            var y = 2
            var found = false
            for (s in tierlist.tiercolumns) {
                if (s.equals(pokemon, ignoreCase = true)) {
                    found = true
                    break
                }
                //logger.info(s + " " + y);
                if (s == "NEXT") {
                    x++
                    y = 2
                } else y++
            }
            val b = RequestBuilder(sid)
            if (found) {
                val request = Request()
                request.updateCells = UpdateCellsRequest().setRows(
                    listOf(
                        RowData()
                            .setValues(
                                listOf(
                                    CellData()
                                        .setUserEnteredFormat(
                                            CellFormat()
                                                .setBackgroundColor(Color().setRed(1f))
                                        )
                                )
                            )
                    )
                )
                    .setFields("userEnteredFormat.backgroundColor").setRange(
                        GridRange().setSheetId(league.getInt("tierlist")).setStartRowIndex(y).setEndRowIndex(y + 1)
                            .setStartColumnIndex((x shl 1) - 1).setEndColumnIndex(x shl 1)
                    )
                b.addBatch(request)
            }
            x = 1
            y = 2
            if (removed != null) {
                for (s in tierlist.tiercolumns) {
                    if (s.equals(removed.name, ignoreCase = true)) {
                        break
                    }
                    //logger.info(s + " " + y);
                    if (s == "NEXT") {
                        x++
                        y = 2
                    } else y++
                }
                val request = Request()
                request.updateCells = UpdateCellsRequest().setRows(
                    listOf(
                        RowData()
                            .setValues(
                                listOf(
                                    CellData()
                                        .setUserEnteredFormat(
                                            CellFormat()
                                                .setBackgroundColor(
                                                    Color()
                                                        .setRed(0.5764706.toFloat()).setGreen(0.76862746.toFloat())
                                                        .setBlue(0.49019608.toFloat())
                                                )
                                        )
                                )
                            )
                    )
                )
                    .setFields("userEnteredFormat.backgroundColor").setRange(
                        GridRange().setSheetId(league.getInt("tierlist")).setStartRowIndex(y).setEndRowIndex(y + 1)
                            .setStartColumnIndex((x shl 1) - 1).setEndColumnIndex(x shl 1)
                    )
                b.addBatch(request)
            }
            //logger.info(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
            val user = listOf(*league.getString("table").split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()).indexOf(mem.id)
            val picks = d.picks[mem.idLong]!!
            for (i in 0..11) {
                val list: MutableList<Any> = ArrayList()
                if (i < picks.size) {
                    val mon = picks[i]
                    list.add(mon.name)
                    list.add(tierlist.getPointsNeeded(mon.name).toString())
                } else {
                    list.add("")
                    list.add("")
                }
                b.addRow(
                    "Teams!" + getAsXCoord((if (user > 3) user - 4 else user) * 5 + 1) + ((if (user > 3) 24 else 7) + i),
                    list
                )
            }
            b.execute()
        }
    }
}