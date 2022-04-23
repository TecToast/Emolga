package de.tectoast.emolga.commands.draft;

import com.google.api.services.sheets.v4.model.*;
import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.RequestBuilder;
import de.tectoast.emolga.utils.draft.Draft;
import de.tectoast.emolga.utils.draft.DraftPokemon;
import de.tectoast.emolga.utils.draft.Tierlist;
import de.tectoast.emolga.utils.records.Coord;
import de.tectoast.jsolf.JSONObject;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static de.tectoast.emolga.utils.draft.Draft.getIndex;

public class PickCommand extends Command {
    private static final Logger logger = LoggerFactory.getLogger(PickCommand.class);
    public static boolean isEnabled = true;

    public PickCommand() {
        super("pick", "Pickt das Pokemon", CommandCategory.Draft);
        setArgumentTemplate(ArgumentManagerTemplate.noSpecifiedArgs("!pick <Pokemon> [Optionales Tier]", "!pick Emolga"));
    }

    public static void exec(TextChannel tco, String msg, Member memberr, boolean isRandom) {
        try {
            long member = memberr.getIdLong();
            if (msg.trim().equals("!pick")) {
                if (isRandom) {
                    tco.sendMessage("Jedes Pokemon aus dem Tier mit dem Typen ist bereits weg!").queue();
                } else {
                    tco.sendMessage("Willst du vielleicht noch ein Pokemon dahinter schreiben? xD").queue();
                }
                return;
            }
            Draft d = Draft.getDraftByMember(member, tco);
            if (d == null) {
                tco.sendMessage(memberr.getAsMention() + " Du bist in keinem Draft drin!").queue();
                return;
            }
            if (!d.tc.getId().equals(tco.getId())) return;
            /*if (d.isSwitchDraft) {
                tco.sendMessage("Dieser Draft ist ein Switch-Draft, daher wird !pick nicht unterstützt!").queue();
                return;
            }*/
            if (d.isNotCurrent(member)) {
                tco.sendMessage(d.getMention(member) + " Du bist nicht dran!").queue();
                return;
            }
            long mem = d.current;
            JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject(d.name);
            String[] split = msg.substring(6).split(" ");
            String tier;
            Translation t;
            String pokemon;
            Tierlist tierlist = d.getTierlist();
            if (d.picks.get(mem).size() == 15) {
                tco.sendMessage("Du hast bereits 15 Mons!").queue();
                return;
            }
            if (split.length == 2 && !d.isPointBased) {
                t = getDraftGerName(split[0]);
                if (!t.isFromType(Translation.Type.POKEMON)) {
                    tco.sendMessage("Das ist kein Pokemon!").queue();
                    return;
                }
                pokemon = t.getTranslation();
                tier = d.getTierlist().order.stream().filter(s -> split[1].equalsIgnoreCase(s)).findFirst().orElse("");
            } else {
                t = getDraftGerName(msg.substring(6));
                if (!t.isFromType(Translation.Type.POKEMON)) {
                    tco.sendMessage("Das ist kein Pokemon!").queue();
                    return;
                }
                pokemon = t.getTranslation();
                tier = tierlist.getTierOf(pokemon);
            }
            if (d.isPicked(pokemon)) {
                //tco.sendMessage(member.getAsMention() + " Junge bist du scheiße oder was?! (Dieses Pokemon wurde bereits gepickt!)").queue();
                tco.sendMessage(memberr.getAsMention() + " Dieses Pokemon wurde bereits gepickt!").queue();
                return;
            }
            int needed = tierlist.getPointsNeeded(pokemon);
            if (d.isPointBased) {
                if (needed == -1) {
                    tco.sendMessage(memberr.getAsMention() + " Das Pokemon steht nicht in der Tierliste!").queue();
                    return;
                }
            } else {
                String origtier = tierlist.getTierOf(pokemon);
                if (origtier.equals("")) {
                    tco.sendMessage(memberr.getAsMention() + " Das ist kein Pokemon!").queue();
                    return;
                }
                if (tierlist.order.indexOf(origtier) < tierlist.order.indexOf(tier)) {
                    tco.sendMessage("Du kannst ein " + origtier + "-Mon nicht ins " + tier + " hochdraften!").queue();
                    return;
                }
                Map<String, Integer> map = d.getPossibleTiers(mem);
                if (!map.containsKey(tier)) {
                    tco.sendMessage("Das Tier `" + tier + "` existiert nicht!").queue();
                    return;
                }
                if (map.get(tier) == 0) {
                    if (tierlist.prices.get(tier) == 0) {
                        tco.sendMessage("Ein Pokemon aus dem " + tier + "-Tier musst du in ein anderes Tier hochdraften!").queue();
                        return;
                    }
                    tco.sendMessage("Du kannst dir kein " + tier + "-Pokemon mehr picken!").queue();
                    return;
                }
            }
            pokemon = tierlist.getNameOf(pokemon);
            /*if (d.hasMega(mem) && pokemon.startsWith("M-")) {
                tco.sendMessage(member.getAsMention() + " Du hast bereits ein Mega!").complete().getId();
                return;
            }
            /*if (d.hasInAnotherForm(mem, pokemon)) {
                tco.sendMessage(member.getAsMention() + " Damit würdest du gegen die Species Clause verstoßen!").queue();
                return;
            }*/
            if (d.isPointBased && d.points.get(mem) - needed < 0) {
                tco.sendMessage(memberr.getAsMention() + " Dafür hast du nicht genug Punkte!").queue();
                return;
            }
            /*if (d.isPointBased && (d.getTierlist().rounds - d.round) * d.getTierlist().prices.get(d.getTierlist().order.get(d.getTierlist().order.size() - 1)) > (d.points.get(mem) - needed)) {
                tco.sendMessage(memberr.getAsMention() + " Wenn du dir dieses Pokemon holen würdest, kann dein Kader nicht mehr vervollständigt werden!").queue();
                return;
            }*/
            if (d.isPointBased)
                d.points.put(mem, d.points.get(mem) - needed);
            d.picks.get(mem).add(new DraftPokemon(pokemon, tier));
            if (!league.has("picks"))
                league.put("picks", new JSONObject());
            league.getJSONObject("picks").put(mem, d.getTeamAsArray(mem));
            //m.delete().queue();
            d.update(mem);
            if (isRandom) {
                tco.sendMessage("**<@" + mem + ">** hat aus dem " + tier + "-Tier ein **" + pokemon + "** bekommen!").queue();
            } else if (pokemon.equals("Emolga")) {
                tco.sendMessage("<:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991>").queue();
            }
            //zbsdoc(tierlist, pokemon, d, mem, tier, d.members.size() - d.order.get(d.round).size(), d.round);
            //fpldoc(tierlist, pokemon, d, mem, tier, d.members.size() - d.order.get(d.round).size(), d.round);
            //woolooDoc(tierlist, pokemon, d, mem, tier, d.round);
            //int rd = d.round == tierlist.rounds && d.picks.get(mem).size() < tierlist.rounds ? (int) league.getJSONObject("skippedturns").getJSONArrayL(mem).remove(0) : d.round;
            //aslS10Doc(tierlist, pokemon, d, mem, tier, rd);
            ndsdoc(tierlist, pokemon, d, mem, tier);
            /*if (d.round == tierlist.rounds && d.picks.get(mem).size() < d.round) {
                if (d.isPointBased)
                    //tco.sendMessage(getMention(current) + " (<@&" + asl.getLongList("roleids").get(getIndex(current.getIdLong())) + ">) ist dran! (" + points.get(current.getIdLong()) + " mögliche Punkte)").queue();
                    tco.sendMessage(d.getMention(mem) + " ist dran! (" + d.points.get(mem) + " mögliche Punkte)").queue();
                else
                    tco.sendMessage(d.getMention(mem) + " ist dran! (Mögliche Tiers: " + d.getPossibleTiersAsString(mem) + ")").queue();
            } else {*/
            d.nextPlayer(tco, tierlist, league);
            //}
            //aslCoachDoc(tierlist, pokemon, d, mem, needed, round, toremove);
            //ndsdoc(tierlist, pokemon, d, mem, tier, round);
        } catch (Exception ex) {
            tco.sendMessage("Es ist ein Fehler aufgetreten!").queue();
            ex.printStackTrace();
        }
    }

    private static void aslS10Doc(Tierlist tierlist, String pokemon, Draft d, long mem, String tier, int effectiveRound) {
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject(d.name);
        String sid = league.getString("sid");
        RequestBuilder b = new RequestBuilder(sid);
        Coord c = tierlist.getLocation(pokemon, 0, 0);
        logger.info("c.toString() = {}", c);
        logger.info("c.valid() = {}", c.valid());
        if (c.valid()) b.addBGColorChange(league.getInt("tierlist"), c.x() * 2 + 2, c.y() + 5, convertColor(0xFF0000));
        Coord cengl = tierlist.getLocation(pokemon, 0, 0, tierlist.tiercolumnsEngl);
        if (cengl.valid())
            b.addBGColorChange(league.getInt("tierlistengl"), cengl.x() * 2 + 2, cengl.y() + 5, convertColor(0xFF0000));
        Integer points = tierlist.prices.get(tier);
        Comparator<DraftPokemon> comparator = Comparator.comparing(p -> tierlist.order.indexOf(p.getTier()));
        Comparator<DraftPokemon> finalComp = comparator.thenComparing(p -> p.name);
        b.addAll("Teamseite HR!B%d".formatted(league.getLongList("table").indexOf(mem) * 15 + 4),
                d.picks.get(mem).stream().sorted(finalComp).map(mon ->
                        new ArrayList<Object>(Arrays.asList(mon.getTier(), mon.getName(), getDataJSON().getJSONObject(getSDName(mon.getName())).getJSONObject("baseStats").getInt("spe")))
                ).collect(Collectors.toList()));
        int rr = effectiveRound - 1;
        logger.info("d.originalOrder = {}", d.originalOrder);
        logger.info("effectiveRound = {}", effectiveRound);
        logger.info("mem = {}", mem);
        int index = d.originalOrder.get(effectiveRound).indexOf(mem);
        logger.info("index = {}", index);

        b.addRow("Draft!%s%d".formatted(getAsXCoord((rr % 6) * 4 + 3), (rr / 6) * 10 + 4 + index), Arrays.asList(pokemon, points));
        b.execute();
    }

    private static void woolooDoc(Tierlist tierlist, String pokemon, Draft d, long mem, String tier, int round) {
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject("WoolooCupS4");
        String sid = league.getString("sid");
        int x = 1;
        int y = 4;
        boolean found = false;
        for (String s : tierlist.tiercolumns) {
            if (s.equalsIgnoreCase(pokemon)) {
                found = true;
                break;
            }
            //logger.info(s + " " + y);
            if (s.equals("NEXT")) {
                x++;
                y = 4;
            } else y++;
        }
        RequestBuilder b = new RequestBuilder(sid);
        if (found) {
            b.addStrikethroughChange(league.getInt("tierlist"), x * 2, y, true);
        }
        //logger.info(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
        String lea = "";
        int num = -1;
        for (int i = 1; i <= 2; i++) {
            List<List<Long>> lists = league.getJSONArray("table" + i).toLongListList();
            Integer in = lists.stream().filter(l -> l.contains(mem)).map(lists::indexOf).findFirst().orElse(-1);
            if (in == -1) continue;
            lea = i == 1 ? "Sonne" : "Hagel";
            num = in;
        }
        b.addRow("Teamseite %s!C%d".formatted(lea, num * 15L + 3 +
                ((tierlist.order.indexOf(tier) * 3L + d.picks.get(mem).stream().filter(p -> p.tier.equals(tier)).count()))
        ), Arrays.asList(pokemon, getDataJSON().getJSONObject(getSDName(pokemon)).getJSONObject("baseStats").getInt("spe")));
        int rr = d.round - 1;
        b.addSingle("Draftreihenfolge!%s%d".formatted(getAsXCoord((rr % 6) * 3 + 3), (rr / 6) * 14 + 2 + (12 - d.order.get(d.round).size())), pokemon);
        b.execute();
    }

    private static void aslCoachDoc(Tierlist tierlist, String pokemon, Draft d, Member mem, int needed, int round, @Nullable DraftPokemon removed) {
        JSONObject asl = getEmolgaJSON().getJSONObject("drafts").getJSONObject("ASLS9");
        JSONObject league = asl.getJSONObject(d.name);
        String sid = asl.getString("sid");
        int x = 1;
        int y = 5;
        boolean found = false;
        for (String s : tierlist.tiercolumns) {
            if (s.equalsIgnoreCase(pokemon)) {
                found = true;
                break;
            }
            //logger.info(s + " " + y);
            if (s.equals("NEXT")) {
                x++;
                y = 5;
            } else y++;
        }
        RequestBuilder b = new RequestBuilder(sid);
        if (found) {
            b.addBGColorChange(league.getInt("tierlist"), x * 2, y, convertColor(0xff0000));
        }
        x = 1;
        y = 5;
        if (removed != null) {
            for (String s : tierlist.tiercolumns) {
                if (s.equalsIgnoreCase(removed.name)) {
                    break;
                }
                //logger.info(s + " " + y);
                if (s.equals("NEXT")) {
                    x++;
                    y = 5;
                } else y++;
            }
            /*Request request = new Request();
            request.setUpdateCells(new UpdateCellsRequest().setRows(Collections.singletonList(new RowData()
                            .setValues(Collections.singletonList(new CellData()
                                    .setUserEnteredFormat(new CellFormat()
                                            .setBackgroundColor(new Color()
                                                    .setRed((float) 0.5764706).setGreen((float) 0.76862746).setBlue((float) 0.49019608)))))))
                    .setFields("userEnteredFormat.backgroundColor").setRange(new GridRange().setSheetId(league.getInt("tierlist")).setStartRowIndex(y).setEndRowIndex(y + 1).setStartColumnIndex(x * 2 - 1).setEndColumnIndex(x * 2)));
            b.addBatch(request);*/
            b.addBGColorChange(league.getInt("tierlist"), x * 2, y, convertColor(0x93c47d));
        }
        //logger.info(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
        String team = asl.getStringList("teams").get(getIndex(mem.getIdLong()));
        int yc = (Draft.getLevel(mem.getIdLong()) * 20 + d.picks.get(mem.getIdLong()).size());
        List<Integer> list = new LinkedList<>();
        for (int i = 0; i < 9; i++) {
            list.add(i * 4 + 10);
        }
        b.addRow(team + "!B" + yc, Arrays.asList(getGen5Sprite(pokemon), pokemon, needed,
                "=" + list.stream().map(i -> getAsXCoord(i) + yc).collect(Collectors.joining(" + ")),
                "=" + list.stream().map(i -> getAsXCoord(i + 1) + yc).collect(Collectors.joining(" + ")),
                ("=E" + yc + " - F" + yc), getDataJSON().getJSONObject(getSDName(pokemon)).getJSONObject("baseStats").getInt("spe")));
        b.execute();
    }

    public static void ndsdoc(Tierlist tierlist, String pokemon, Draft d, long mem, String tier) {
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject(d.name);

        //logger.info(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
        RequestBuilder b = new RequestBuilder(league.getString("sid"));
        String teamname = league.getJSONObject("teamnames").getString(mem);
        String sdName = getSDName(pokemon);
        JSONObject o = getDataJSON().getJSONObject(sdName);
        int i = d.picks.get(mem).size() + 14;
        Coord tl = tierlist.getLocation(pokemon, 0, 0);
        String gen5Sprite = getGen5Sprite(o);
        b
                .addSingle(teamname + "!B" + i, gen5Sprite)
                .addSingle(teamname + "!D" + i, pokemon)
                .addSingle("Tierliste!" + getAsXCoord(tl.x() * 6 + 6) + (tl.y() + 4), "='" + teamname + "'!B2");
        List<Object> t = o.getStringList("types").stream().map(s -> getTypeIcons().getString(s)).collect(Collectors.toCollection(LinkedList::new));
        if (t.size() == 1) t.add("/");
        b.addRow(teamname + "!F" + i, t);
        b.addSingle(teamname + "!H" + i, o.getJSONObject("baseStats").getInt("spe"));
        int pointsNeeded = tierlist.getPointsNeeded(pokemon);
        b.addSingle(teamname + "!I" + i, pointsNeeded);
        b.addSingle(teamname + "!J" + i, "2");
        b.addRow(teamname + "!L" + i, Arrays.asList(canLearnNDS(sdName, "stealthrock"), canLearnNDS(sdName, "defog"), canLearnNDS(sdName, "rapidspin"), canLearnNDS(sdName, "voltswitch", "uturn", "flipturn", "batonpass", "teleport")));
        int numInRound = d.originalOrder.get(d.round).indexOf(mem) + 1;
        b.addSingle("Draft!%s%d".formatted(getAsXCoord(d.round * 5 - 3), numInRound * 5 + 2), "《《《《")
                .addSingle("Draft!%s%d".formatted(getAsXCoord(d.round * 5 - 1), numInRound * 5 + 2), pokemon)
                .addSingle("Draft!%s%d".formatted(getAsXCoord(d.round * 5), numInRound * 5 + 1), gen5Sprite)
                .addSingle("Draft!%s%d".formatted(getAsXCoord(d.round * 5), numInRound * 5 + 3), pointsNeeded);


        logger.info("d.members.size() = " + d.members.size());
        logger.info("d.order.size() = " + d.order.get(d.round).size());
        logger.info("d.members.size() - d.order.size() = " + numInRound);
        //if (d.members.size() - d.order.get(d.round).size() != 1 && isEnabled)
        b.execute();

    }

    private static void fpldoc(Tierlist tierlist, String pokemon, Draft d, Member mem, String tier, int num, int round) {
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject(d.name);
        if (league.has("sid")) {
            String doc = league.getString("sid");
            int x = 1;
            int y = 3;
            for (String s : tierlist.tiercolumns) {
                if (s.equalsIgnoreCase(pokemon)) break;
                //logger.info(s + " " + y);
                if (s.equals("NEXT")) {
                    x++;
                    y = 3;
                } else y++;
            }
            logger.info("num = " + num);
            RequestBuilder b = new RequestBuilder(doc);
            b.addStrikethroughChange(league.getInt("tierlist"), x * 2, y, true);
            //logger.info(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
            b.addStrikethroughChange(league.getInt("draftorder"), d.round + 1, num + 6, true);
            int user = league.getLongList("table").indexOf(mem.getIdLong());
            String range = "Kader %s!%s%d".formatted(d.name.substring(5), getAsXCoord((user / 4) * 22 + 2), (user % 4) * 20 + 7 + d.picks.get(mem.getIdLong()).size());
            logger.info("range = " + range);
            b.addRow(range, Arrays.asList(tier, "", pokemon, "", getDataJSON().getJSONObject(getSDName(pokemon)).getJSONObject("baseStats").getInt("spe")));
            logger.info("d.members.size() = " + d.members.size());
            logger.info("d.order.size() = " + d.order.get(d.round).size());
            logger.info("d.members.size() - d.order.size() = " + (d.members.size() - d.order.get(d.round).size()));
            //if (d.members.size() - d.order.get(d.round).size() != 1 && isEnabled)
            b.execute();
        }
    }

    private static void zbsdoc(Tierlist tierlist, String pokemon, Draft d, Member mem, String tier, int num, int round) {
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject(d.name);
        if (league.has("sid")) {
            String doc = league.getString("sid");
            int x = 1;
            int y = 2;
            for (String s : tierlist.tiercolumns) {
                if (s.equalsIgnoreCase(pokemon)) break;
                //logger.info(s + " " + y);
                if (s.equals("NEXT")) {
                    x++;
                    y = 2;
                } else y++;
            }
            logger.info("num = " + num);
            RequestBuilder b = new RequestBuilder(doc);
            b.addStrikethroughChange(910228334, x * 2 + 1, y, true);
            //logger.info(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
            b.addStrikethroughChange(856868721, d.round + 2, num + 2, true);
            int user = league.getLongList("table").indexOf(mem.getIdLong());
            String range = "Liga 2!" + getAsXCoord((int) (tier.equals("S") ? (12 + d.picks.get(mem.getIdLong()).stream().filter(p -> p.tier.equals("S")).count()) : (tierlist.order.indexOf(tier) * 3 + 11 + d.picks.get(mem.getIdLong()).stream().filter(p -> p.tier.equals(tier)).count()))) + (user + 3);
            logger.info("range = " + range);
            b.addSingle(range, getGen5Sprite(pokemon));
            logger.info("d.members.size() = " + d.members.size());
            logger.info("d.order.size() = " + d.order.get(d.round).size());
            logger.info("d.members.size() - d.order.size() = " + (d.members.size() - d.order.get(d.round).size()));
            //if (d.members.size() - d.order.get(d.round).size() != 1 && isEnabled)
            b.execute();
        }
    }

    /*public void doc(Tierlist tierlist, String pokemon, Draft d, Member mem, String tier, JSONObject league, int pk) {
        zbsdoc(tierlist, pokemon, d, mem, tier, num);
        //aslnocoachdoc(tierlist, pokemon, d, mem, tier, league, pk);
    }*/

    private static void woolooolddoc(Tierlist tierlist, String pokemon, Draft d, Member mem, int needed, @Nullable DraftPokemon removed, int num, int round) {
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject(d.name);
        String sid = league.getString("sid");
        int x = 1;
        int y = 2;
        boolean found = false;
        for (String s : tierlist.tiercolumns) {
            if (s.equalsIgnoreCase(pokemon)) {
                found = true;
                break;
            }
            //logger.info(s + " " + y);
            if (s.equals("NEXT")) {
                x++;
                y = 2;
            } else y++;
        }
        RequestBuilder b = new RequestBuilder(sid);
        if (found) {
            Request request = new Request();
            request.setUpdateCells(new UpdateCellsRequest().setRows(Collections.singletonList(new RowData()
                            .setValues(Collections.singletonList(new CellData()
                                    .setUserEnteredFormat(new CellFormat()
                                            .setBackgroundColor(new Color().setRed(1f)))))))
                    .setFields("userEnteredFormat.backgroundColor").setRange(new GridRange().setSheetId(league.getInt("tierlist")).setStartRowIndex(y).setEndRowIndex(y + 1).setStartColumnIndex(x * 2 - 1).setEndColumnIndex(x * 2)));
            b.addBatch(request);
        }
        x = 1;
        y = 2;
        if (removed != null) {
            for (String s : tierlist.tiercolumns) {
                if (s.equalsIgnoreCase(removed.name)) {
                    break;
                }
                //logger.info(s + " " + y);
                if (s.equals("NEXT")) {
                    x++;
                    y = 2;
                } else y++;
            }
            Request request = new Request();
            request.setUpdateCells(new UpdateCellsRequest().setRows(Collections.singletonList(new RowData()
                            .setValues(Collections.singletonList(new CellData()
                                    .setUserEnteredFormat(new CellFormat()
                                            .setBackgroundColor(new Color()
                                                    .setRed((float) 0.5764706).setGreen((float) 0.76862746).setBlue((float) 0.49019608)))))))
                    .setFields("userEnteredFormat.backgroundColor").setRange(new GridRange().setSheetId(league.getInt("tierlist")).setStartRowIndex(y).setEndRowIndex(y + 1).setStartColumnIndex(x * 2 - 1).setEndColumnIndex(x * 2)));
            b.addBatch(request);
        }
        Request req = new Request();
        //logger.info(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));

        req.setUpdateCells(new UpdateCellsRequest().setRows(Collections.singletonList(new RowData()
                        .setValues(Collections.singletonList(new CellData()
                                .setUserEnteredFormat(new CellFormat()
                                        .setTextFormat(new TextFormat().setStrikethrough(true)))))))
                .setFields("userEnteredFormat.textFormat.strikethrough").setRange(new GridRange().setSheetId(1316641169).setStartRowIndex(num + 1).setEndRowIndex(num + 2).setStartColumnIndex(round).setEndColumnIndex(round + 1)));
        //logger.info(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
        b.addBatch(req);


        int user = Arrays.asList(league.getString("table").split(",")).indexOf(mem.getId());
        List<DraftPokemon> picks = d.picks.get(mem.getIdLong());
        for (int i = 0; i < 13; i++) {
            List<Object> list = new ArrayList<>();
            if (i < picks.size()) {
                DraftPokemon mon = picks.get(i);
                list.add(tierlist.prices.get(mon.tier));
                list.add(mon.name);
            } else {
                list.add("");
                list.add("");
            }
            b.addRow("Teamübersicht!" + getAsXCoord((user > 3 ? user - 4 : user) * 6 + 2) + ((user > 3 ? 25 : 7) + i), list);
        }
        b.execute();
    }

    private static void aslnocoachdoc(Tierlist tierlist, String pokemon, Draft d, Member mem, int needed, @Nullable DraftPokemon removed) {
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject(d.name);
        String sid = league.getString("sid");
        int x = 1;
        int y = 2;
        boolean found = false;
        for (String s : tierlist.tiercolumns) {
            if (s.equalsIgnoreCase(pokemon)) {
                found = true;
                break;
            }
            //logger.info(s + " " + y);
            if (s.equals("NEXT")) {
                x++;
                y = 2;
            } else y++;
        }
        RequestBuilder b = new RequestBuilder(sid);
        if (found) {
            Request request = new Request();
            request.setUpdateCells(new UpdateCellsRequest().setRows(Collections.singletonList(new RowData()
                            .setValues(Collections.singletonList(new CellData()
                                    .setUserEnteredFormat(new CellFormat()
                                            .setBackgroundColor(new Color().setRed(1f)))))))
                    .setFields("userEnteredFormat.backgroundColor").setRange(new GridRange().setSheetId(league.getInt("tierlist")).setStartRowIndex(y).setEndRowIndex(y + 1).setStartColumnIndex(x * 2 - 1).setEndColumnIndex(x * 2)));
            b.addBatch(request);
        }
        x = 1;
        y = 2;
        if (removed != null) {
            for (String s : tierlist.tiercolumns) {
                if (s.equalsIgnoreCase(removed.name)) {
                    break;
                }
                //logger.info(s + " " + y);
                if (s.equals("NEXT")) {
                    x++;
                    y = 2;
                } else y++;
            }
            Request request = new Request();
            request.setUpdateCells(new UpdateCellsRequest().setRows(Collections.singletonList(new RowData()
                            .setValues(Collections.singletonList(new CellData()
                                    .setUserEnteredFormat(new CellFormat()
                                            .setBackgroundColor(new Color()
                                                    .setRed((float) 0.5764706).setGreen((float) 0.76862746).setBlue((float) 0.49019608)))))))
                    .setFields("userEnteredFormat.backgroundColor").setRange(new GridRange().setSheetId(league.getInt("tierlist")).setStartRowIndex(y).setEndRowIndex(y + 1).setStartColumnIndex(x * 2 - 1).setEndColumnIndex(x * 2)));
            b.addBatch(request);
        }
        //logger.info(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));


        int user = Arrays.asList(league.getString("table").split(",")).indexOf(mem.getId());
        List<DraftPokemon> picks = d.picks.get(mem.getIdLong());
        for (int i = 0; i < 12; i++) {
            List<Object> list = new ArrayList<>();
            if (i < picks.size()) {
                DraftPokemon mon = picks.get(i);
                list.add(mon.name);
                list.add(String.valueOf(tierlist.getPointsNeeded(mon.name)));
            } else {
                list.add("");
                list.add("");
            }
            b.addRow("Teams!" + getAsXCoord((user > 3 ? user - 4 : user) * 5 + 1) + ((user > 3 ? 24 : 7) + i), list);
        }
        b.execute();
    }

    @Override
    public void process(GuildCommandEvent e) {
        exec(e.getChannel(), e.getMessage().getContentDisplay(), e.getMember(), false);
    }
}
