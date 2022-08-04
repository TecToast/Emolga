package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.json.emolga.draft.League
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.slf4j.LoggerFactory

@Suppress("unused")
class PickCommand : Command("pick", "Pickt das Pokemon", CommandCategory.Draft) {
    init {
        //setArgumentTemplate(ArgumentManagerTemplate.noSpecifiedArgs("!pick <Pokemon> [Optionales Tier]", "!pick Emolga"));
        argumentTemplate = ArgumentManagerTemplate.builder().add(
            "pokemon",
            "Pokemon",
            "Das Pokemon, was du picken willst",
            ArgumentManagerTemplate.draftPokemon { s: String, event: CommandAutoCompleteInteractionEvent ->
                val strings = Tierlist.getByGuild(event.guild!!.id)?.autoComplete?.filter { str: String ->
                    str.lowercase().startsWith(s.lowercase())
                }
                if (strings == null || strings.size > 25) return@draftPokemon emptyList<String>()
                strings
            },
            false,
            "Das ist kein Pokemon!"
        ) //.add("tier", "Tier", "Das Tier", ArgumentManagerTemplate.Text.any(), true)
            .setExample("!pick Emolga").build()
        slash(false, Constants.FPLID, Constants.NDSID)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val msg: String
        var sc: SlashCommandInteractionEvent? = null
        if (e.isSlash) {
            sc = e.slashCommandEvent
            msg = "!pick ${sc!!.getOption("pokemon")!!.asString}"
        } else {
            msg = e.msg!!
        }
        exec(DraftEvent(e.textChannel, sc, e.member.asMention), msg, e.member, false)
    }


    companion object {
        private val logger = LoggerFactory.getLogger(PickCommand::class.java)

        fun exec(
            e: DraftEvent, msg: String, memberr: Member, isRandom: Boolean
        ) {
            try {
                val member = memberr.idLong
                if (msg.trim() == "!pick") {
                    e.reply(if (isRandom) "Jedes Pokemon aus dem Tier mit dem Typen ist bereits weg!" else "Willst du vielleicht noch ein Pokemon dahinter schreiben? xD")
                    return
                }
                val d = League.byChannel(e.tc, member, e) ?: return
                val mem = d.current
                val split = msg.substring(6).split(" ")
                val tier: String
                val t: Translation
                val pokemon: String
                val tierlist = d.tierlist
                val picks = d.picks[mem]!!
                if (picks.filter { it.name != "???" }.size == 15) {
                    e.reply("Du hast bereits 15 Mons!")
                    return
                }
                if (split.size == 2 && !d.isPointBased) {
                    t = getDraftGerName(split[0])
                    if (!t.isFromType(Translation.Type.POKEMON)) {
                        e.reply("Das ist kein Pokemon!")
                        return
                    }
                    pokemon = t.translation
                    tier = tierlist.order.firstOrNull { split[1].equals(it, ignoreCase = true) } ?: ""
                } else {
                    t = getDraftGerName(msg.substring(6))
                    if (!t.isFromType(Translation.Type.POKEMON)) {
                        e.reply("Das ist kein Pokemon!")
                        return
                    }
                    pokemon = t.translation
                    tier = tierlist.getTierOf(pokemon)
                }
                if (d.isPicked(pokemon)) {
                    //tco.sendMessage(member.getAsMention() + " Junge bist du scheiße oder was?! (Dieses Pokemon wurde bereits gepickt!)").queue();
                    e.reply("Dieses Pokemon wurde bereits gepickt!")
                    return
                }
                val needed = tierlist.getPointsNeeded(pokemon)
                if (d.isPointBased) {
                    if (needed == -1) {
                        e.reply("Das Pokemon steht nicht in der Tierliste!")
                        return
                    }
                } else {
                    val origtier = tierlist.getTierOf(pokemon)
                    if (origtier.isEmpty()) {
                        e.reply("Das Pokemon steht nicht in der Tierliste!")
                        return
                    }
                    if (tierlist.order.indexOf(origtier) < tierlist.order.indexOf(tier)) {
                        e.reply("Du kannst ein $origtier-Mon nicht ins $tier hochdraften!")
                        return
                    }
                    val map = d.getPossibleTiers(mem)
                    if (!map.containsKey(tier)) {
                        e.reply("Das Tier `$tier` existiert nicht!")
                        return
                    }
                    if (map[tier]!! <= 0) {
                        if (tierlist.prices[tier] == 0) {
                            e.reply("Ein Pokemon aus dem $tier-Tier musst du in ein anderes Tier hochdraften!")
                            return
                        }
                        e.reply("Du kannst dir kein $tier-Pokemon mehr picken!")
                        return
                    }
                }
                if (d.handlePoints(e, needed)) return
                d.savePick(picks, pokemon, tier)
                //m.delete().queue();
                e.slashEvent?.reply("${e.mention} hat $pokemon gepickt!")?.queue()
                if (isRandom) {
                    e.reply("**<@$mem>** hat aus dem $tier-Tier ein **$pokemon** bekommen!")
                } else if (pokemon == "Emolga") {
                    e.tc.sendMessage("<:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991>")
                        .queue()
                }
                d.pickDoc(PickData(pokemon, tier, mem, d.indexInRound(), picks.indexOfFirst { it.name == pokemon }))
                d.nextPlayer()
                //zbsdoc(tierlist, pokemon, d, mem, tier, d.members.size() - d.order.get(d.round).size(), d.round);
                //fpldoc(tierlist, pokemon, d, mem, tier, d.members.size() - d.order.get(d.round).size(), d.round);
                //woolooDoc(tierlist, pokemon, d, mem, tier, d.round);
                //int rd = d.round == tierlist.rounds && d.picks.get(mem).size() < tierlist.rounds ? (int) league.getJSONObject("skippedturns").getJSONArrayL(mem).remove(0) : d.round;
                //aslS10Doc(tierlist, pokemon, d, mem, tier, rd);
                //ndsdoc(tierlist, pokemon, d, mem, tier);
                //uplDoc(pokemon, league, d, d.members.size() - d.order.get(d.round).size(), mem);
                //ndss3Doc(pokemon, d, mem, picks.indexOf(firstUnknown))
                /*if (d.round == tierlist.rounds && d.picks.get(mem).size() < d.round) {
                if (d.isPointBased)
                    //tco.sendMessage(getMention(current) + " (<@&" + asl.getLongList("roleids").get(getIndex(current.getIdLong())) + ">) ist dran! (" + points.get(current.getIdLong()) + " mögliche Punkte)").queue();
                    tco.sendMessage(d.getMention(mem) + " ist dran! (" + d.points.get(mem) + " mögliche Punkte)").queue();
                else
                    tco.sendMessage(d.getMention(mem) + " ist dran! (Mögliche Tiers: " + d.getPossibleTiersAsString(mem) + ")").queue();
            } else {*/

                //}
                //aslCoachDoc(tierlist, pokemon, d, mem, needed, round, toremove);
                //ndsdoc(tierlist, pokemon, d, mem, tier, round);
            } catch (ex: Exception) {
                e.reply("Es ist ein Fehler aufgetreten!")
                ex.printStackTrace()
            }
        }

        /*private fun ndss3Doc(pokemon: String, d: Draft, mem: Long, index: Int) {
            val league = emolgaJSON.getJSONObject("drafts").getJSONObject(d.name)

            //logger.info(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
            val b = RequestBuilder(league.getString("sid"))
            val teamname = league.getJSONObject("teamnames").getString(mem)
            val picks = d.picks.getValue(mem)
            val y = league.getStringList("table").indexOf(teamname) * 17 + 2 + index
            b.addSingle("Data!B$y", pokemon)
            b.addSingle("Data!AF$y", 2)
            val tiers = listOf("S", "A", "B")
            b.addColumn("Data!F${league.getStringList("table").indexOf(teamname) * 17 + 2}", picks
                .asSequence()
                .filter { it.name != "???" }
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
            Comparator.comparing { p: DraftPokemon -> tierlist.order.indexOf(p.tier) }
                .thenComparing { p: DraftPokemon -> p.name }
            b.addAll("Teamseite HR!B${league.getLongList("table").indexOf(mem) * 15 + 4}",
                d.picks[mem]!!.asSequence().sortedWith(compareBy({ it.tier.indexedBy(tierlist.order) }, { it.name }))
                    .map { mon: DraftPokemon ->
                        listOf(
                            mon.tier, mon.name, dataJSON.getJSONObject(
                                getSDName(mon.name)
                            ).getJSONObject("baseStats").getInt("spe")
                        )
                    }
                    .toList()
            )
            val rr = effectiveRound - 1
            logger.info("d.originalOrder = {}", d.originalOrder)
            logger.info("effectiveRound = {}", effectiveRound)
            logger.info("mem = {}", mem)
            val index = d.originalOrder[effectiveRound]!!.indexOf(mem)
            logger.info("index = {}", index)
            b.addRow(
                "Draft!${getAsXCoord((rr % 6 shl 2) + 3)}${rr / 6 * 10 + 4 + index}",
                listOf(pokemon, tierlist.prices.getValue(tier))
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
                val idk = lists.indexOfFirst { mem in it }
                if (idk == -1) continue
                lea = if (i == 1) "Sonne" else "Hagel"
                num = idk
            }
            b.addRow(
                "Teamseite $lea!C${
                    num * 15L + 3 +
                            (tierlist.order.indexOf(tier) * 3L + d.picks[mem]!!.count { it.tier == tier })
                }",
                listOf(
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
                "$team!B$yc", listOf<Any>(
                    getGen5Sprite(pokemon),
                    pokemon,
                    needed,
                    "=" + list.joinToString(" + ") {
                        getAsXCoord(
                            it
                        ) + yc
                    },
                    "=" + list.joinToString(" + ") { i: Int -> getAsXCoord(i + 1) + yc },
                    "=E$yc - F$yc",
                    dataJSON.getJSONObject(getSDName(pokemon)).getJSONObject("baseStats").getInt("spe")
                )
            )
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
                o.getStringList("types").map { s: String? -> typeIcons.getString(s) }.toMutableList()
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
                    .count { it.tier == "S" }
                else tierlist.order.indexOf(tier) * 3 + 11 + d.picks[mem.idLong]!!
                    .count { it.tier == tier }).toInt()
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
            val user = listOf(*league.getString("table").split(",").dropLastWhile { it.isEmpty() }
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
            val user = listOf(*league.getString("table").split(",").dropLastWhile { it.isEmpty() }
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
        }*/
    }
}

class DraftEvent(val tc: TextChannel, val slashEvent: SlashCommandInteractionEvent?, val mention: String) {

    constructor(e: GuildCommandEvent) : this(e.textChannel, e.slashCommandEvent, e.author.asMention)

    fun reply(msg: String) = msg.let { slashEvent?.reply(it)?.queue() ?: tc.sendMessage("$mention $it").queue() }
}

class PickData(val pokemon: String, val tier: String, val mem: Long, val indexInRound: Int, val changedIndex: Int)