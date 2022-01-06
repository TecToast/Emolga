package de.tectoast.emolga.commands.draft;

import com.google.api.services.sheets.v4.model.*;
import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.RequestBuilder;
import de.tectoast.emolga.utils.draft.Draft;
import de.tectoast.emolga.utils.draft.DraftPokemon;
import de.tectoast.emolga.utils.draft.Tierlist;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jsolf.JSONArray;
import org.jsolf.JSONObject;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static de.tectoast.emolga.utils.draft.Draft.getIndex;

public class PickCommand extends Command {
    public static boolean isEnabled = true;

    public PickCommand() {
        super("pick", "Pickt das Pokemon", CommandCategory.Draft);
        setArgumentTemplate(ArgumentManagerTemplate.noSpecifiedArgs("!pick <Pokemon> [Optionales Tier]", "!pick Emolga"));
    }

    public static void exec(TextChannel tco, String msg, Member member, boolean isRandom) {
        try {
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
                tco.sendMessage(member.getAsMention() + " Du bist in keinem Draft drin!").queue();
                return;
            }
            if (!d.tc.getId().equals(tco.getId())) return;
            if (d.isSwitchDraft) {
                tco.sendMessage("Dieser Draft ist ein Switch-Draft, daher wird !pick nicht unterstützt!").queue();
                return;
            }
            if (d.isNotCurrent(member)) {
                tco.sendMessage(d.getMention(member) + " Du bist nicht dran!").queue();
                return;
            }
            Member mem = d.current;
            JSONObject json = getEmolgaJSON();
            //JSONObject asl = json.getJSONObject("drafts").getJSONObject("ASLS9");
            JSONObject league = json.getJSONObject("drafts").getJSONObject(d.name);
            //JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject("ZBSL2");
            /*if (asl.has("allowed")) {
                JSONObject allowed = asl.getJSONObject("allowed");
                if (allowed.has(member.getId())) {
                    mem = d.tc.getGuild().retrieveMemberById(allowed.getString(member.getId())).complete();
                } else mem = member;
            } else mem = member;*/
            /*if (d.current.getId().equals(member.getId())) mem = member;
            else if (member.getIdLong() == Constants.FLOID) mem = d.current;
            else if (Draft.getTeamMembers(member.getIdLong()).contains(d.current.getIdLong())) mem = d.current;
            else
                mem = tco.getGuild().retrieveMemberById(league.getJSONObject("allowed").getString(member.getId())).complete();*/
            String[] split = msg.substring(6).split(" ");
            String tier;
            Translation t;
            String pokemon;
            Tierlist tierlist = d.getTierlist();
            if (split.length == 2 && !d.isPointBased) {
                t = getDraftGerName(split[0]);
                if (!t.isFromType(Translation.Type.POKEMON)) {
                    tco.sendMessage("Das ist kein Pokemon!").queue();
                    return;
                }
                pokemon = t.getTranslation();
                tier = split[1];
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
                tco.sendMessage(member.getAsMention() + " Dieses Pokemon wurde bereits gepickt!").queue();
                return;
            }
            int needed = tierlist.getPointsNeeded(pokemon);
            if (d.isPointBased) {
                if (needed == -1) {
                    tco.sendMessage(member.getAsMention() + " Das Pokemon steht nicht in der Tierliste!").queue();
                    return;
                }
            } else {
                String origtier = tierlist.getTierOf(pokemon);
                if (origtier.equals("")) {
                    tco.sendMessage(member.getAsMention() + " Das ist kein Pokemon!").queue();
                    return;
                }
                if (tierlist.order.indexOf(origtier) < tierlist.order.indexOf(tier)) {
                    tco.sendMessage("Du kannst ein " + origtier + "-Mon nicht ins " + tier + " hochdraften!").queue();
                    return;
                }
                HashMap<String, Integer> map = d.getPossibleTiers(mem);
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
            if (d.isPointBased && d.points.get(mem.getIdLong()) - needed < 0) {
                tco.sendMessage(member.getAsMention() + " Dafür hast du nicht genug Punkte!").queue();
                return;
            }
            if (d.isPointBased)
                d.points.put(mem.getIdLong(), d.points.get(mem.getIdLong()) - needed);
            d.picks.get(mem.getIdLong()).add(new DraftPokemon(pokemon, tier));
            if (!league.has("picks"))
                league.put("picks", new JSONObject());
            league.getJSONObject("picks").put(mem.getId(), d.getTeamAsArray(mem));
            //m.delete().queue();
            d.update(mem);
            if (isRandom) {
                tco.sendMessage("**" + mem.getEffectiveName() + "** hat aus dem " + tier + "-Tier ein **" + pokemon + "** bekommen!").queue();
            } else if (pokemon.equals("Emolga")) {
                tco.sendMessage("<:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991>").queue();
            }
            //zbsdoc(tierlist, pokemon, d, mem, tier, d.members.size() - d.order.get(d.round).size(), d.round);
            fpldoc(tierlist, pokemon, d, mem, tier, d.members.size() - d.order.get(d.round).size(), d.round);
            int round = d.round;
            if (d.order.get(d.round).size() == 0) {
                if (d.round == tierlist.rounds) {
                    tco.sendMessage("Der Draft ist vorbei!").queue();
                    d.ended = true;
                    //ndsdoc(tierlist, pokemon, d, mem, tier, round);
                    //aslCoachDoc(tierlist, pokemon, d, mem, needed, round, null);
                    if (d.afterDraft.size() > 0)
                        tco.sendMessage("Reihenfolge zum Nachdraften:\n" + d.afterDraft.stream().map(d::getMention).collect(Collectors.joining("\n"))).queue();
                    saveEmolgaJSON();
                    Draft.drafts.remove(d);
                    return;
                }
                d.round++;
                d.tc.sendMessage("Runde " + d.round + "!").queue();
                league.put("round", d.round);
            }
            boolean normal = /*round != 12 || d.picks.get(d.current.getIdLong()).size() == tierlist.rounds;*/ true;
            if (normal) {
                d.current = d.order.get(d.round).remove(0);
                league.put("current", d.current.getId());
                try {
                    d.cooldown.cancel();
                } catch (Exception ignored) {

                }
            }
            DraftPokemon toremove = null;
            if (d.isPointBased && d.points.get(d.current.getIdLong()) < 20) {
                List<DraftPokemon> picks = d.picks.get(d.current.getIdLong());
                int price = 0;
                for (DraftPokemon pick : picks) {
                    int pr = tierlist.getPointsNeeded(pick.name);
                    if (pr > price) {
                        price = pr;
                        toremove = pick;
                    }
                }
                tco.sendMessage(d.getMention(d.current) + " Du hast nicht mehr genug Punkte um ein weiteres Pokemon zu draften! Deshalb verlierst du " + toremove.name + " und erhältst dafür " + price / 2 + " Punkte!").queue();
                d.points.put(d.current.getIdLong(), d.points.get(d.current.getIdLong()) + price / 2);
                d.picks.get(d.current.getIdLong()).remove(toremove);
                d.afterDraft.add(d.current);
            }
            league.getJSONObject("picks").put(d.current.getId(), d.getTeamAsArray(d.current));
            if (d.isPointBased)
                //tco.sendMessage(d.getMention(d.current) + " (<@&" + asl.getLongList("roleids").get(getIndex(d.current.getIdLong())) + ">) ist dran! (" + d.points.get(d.current.getIdLong()) + " mögliche Punkte)").queue();
                tco.sendMessage(d.getMention(d.current) + " ist dran! (" + d.points.get(d.current.getIdLong()) + " mögliche Punkte)").queue();
            else
                tco.sendMessage(d.getMention(d.current) + " ist dran! (Mögliche Tiers: " + d.getPossibleTiersAsString(d.current) + ")").queue();
            if(normal) {
                d.cooldown = new Timer();
                long delay = calculateASLTimer();
                league.put("cooldown", System.currentTimeMillis() + delay);
                d.cooldown.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        d.timer();
                    }
                }, delay);
            }
            saveEmolgaJSON();
            //aslCoachDoc(tierlist, pokemon, d, mem, needed, round, toremove);
            //ndsdoc(tierlist, pokemon, d, mem, tier, round);
        } catch (Exception ex) {
            tco.sendMessage("Es ist ein Fehler aufgetreten!").queue();
            ex.printStackTrace();
        }
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
            //System.out.println(s + " " + y);
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
                //System.out.println(s + " " + y);
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
        //System.out.println(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
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

    public static void ndsdoc(Tierlist tierlist, String pokemon, Draft d, Member mem, String tier, int round) {
        int FIRST_ROW = 4;
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject(d.name);
        if (league.has("sid")) {
            String doc = league.getString("sid");
            int x = 1;
            int y = FIRST_ROW;
            for (String s : tierlist.tiercolumns) {
                if (s.equalsIgnoreCase(pokemon)) break;
                //System.out.println(s + " " + y);
                if (s.equals("NEXT")) {
                    x++;
                    y = FIRST_ROW;
                } else y++;
            }

            //System.out.println(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
            int user = Arrays.asList(league.getString("table").split(",")).indexOf(mem.getId());
            RequestBuilder b = new RequestBuilder(doc);
            String teamname = league.getJSONObject("teamnames").getString(mem.getId());
            String sdName = getSDName(pokemon);
            JSONObject o = getDataJSON().getJSONObject(sdName);
            int i = round + 14;
            b
                    .addSingle(teamname + "!B" + i, getGen5Sprite(o))
                    .addSingle(teamname + "!D" + i, pokemon)
                    .addSingle("Tierliste!" + getAsXCoord(x * 6) + y, "='" + teamname + "'!B2");
            JSONArray arr = o.getJSONArray("types");
            List<Object> t = new LinkedList<>();
            for (String s : arrayToList(arr, String.class)) {
                t.add(getTypeIcons().getString(s));
            }
            if (t.size() == 1) t.add("/");
            b.addRow(teamname + "!F" + i, t);
            b.addSingle(teamname + "!H" + i, o.getJSONObject("baseStats").getInt("spe"));
            b.addSingle(teamname + "!I" + i, tierlist.getPointsNeeded(pokemon));
            b.addSingle(teamname + "!J" + i, "2");
            b.addRow(teamname + "!L" + i, Arrays.asList(canLearnNDS(sdName, "stealthrock"), canLearnNDS(sdName, "defog"), canLearnNDS(sdName, "rapidspin"), canLearnNDS(sdName, "voltswitch", "uturn", "flipturn", "batonpass", "teleport")));
            System.out.println("d.members.size() = " + d.members.size());
            System.out.println("d.order.size() = " + d.order.get(d.round).size());
            System.out.println("d.members.size() - d.order.size() = " + (d.members.size() - d.order.get(d.round).size()));
            //if (d.members.size() - d.order.get(d.round).size() != 1 && isEnabled)
            b.execute();
        }
    }

    private static void fpldoc(Tierlist tierlist, String pokemon, Draft d, Member mem, String tier, int num, int round) {
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject(d.name);
        if (league.has("sid")) {
            String doc = league.getString("sid");
            int x = 1;
            int y = 3;
            for (String s : tierlist.tiercolumns) {
                if (s.equalsIgnoreCase(pokemon)) break;
                //System.out.println(s + " " + y);
                if (s.equals("NEXT")) {
                    x++;
                    y = 3;
                } else y++;
            }
            System.out.println("num = " + num);
            RequestBuilder b = new RequestBuilder(doc);
            b.addStrikethroughChange(league.getInt("tierlist"), x * 2, y, true);
            //System.out.println(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
            b.addStrikethroughChange(league.getInt("draftorder"), d.round + 1, num + 6, true);
            int user = league.getLongList("table").indexOf(mem.getIdLong());
            String range = "Kader %s!%s%d".formatted(d.name.substring(5), getAsXCoord((user / 4) * 22 + 2), (user % 4) * 20 + 7 + d.picks.get(mem.getIdLong()).size());
            System.out.println("range = " + range);
            b.addRow(range, Arrays.asList(tier, "", pokemon, "", getDataJSON().getJSONObject(getSDName(pokemon)).getJSONObject("baseStats").getInt("spe")));
            System.out.println("d.members.size() = " + d.members.size());
            System.out.println("d.order.size() = " + d.order.get(d.round).size());
            System.out.println("d.members.size() - d.order.size() = " + (d.members.size() - d.order.get(d.round).size()));
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
                //System.out.println(s + " " + y);
                if (s.equals("NEXT")) {
                    x++;
                    y = 2;
                } else y++;
            }
            System.out.println("num = " + num);
            RequestBuilder b = new RequestBuilder(doc);
            b.addStrikethroughChange(910228334, x * 2 + 1, y, true);
            //System.out.println(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
            b.addStrikethroughChange(856868721, d.round + 2, num + 2, true);
            int user = league.getLongList("table").indexOf(mem.getIdLong());
            String range = "Liga 2!" + getAsXCoord((int) (tier.equals("S") ? (12 + d.picks.get(mem.getIdLong()).stream().filter(p -> p.tier.equals("S")).count()) : (tierlist.order.indexOf(tier) * 3 + 11 + d.picks.get(mem.getIdLong()).stream().filter(p -> p.tier.equals(tier)).count()))) + (user + 3);
            System.out.println("range = " + range);
            b.addSingle(range, getGen5Sprite(pokemon));
            System.out.println("d.members.size() = " + d.members.size());
            System.out.println("d.order.size() = " + d.order.get(d.round).size());
            System.out.println("d.members.size() - d.order.size() = " + (d.members.size() - d.order.get(d.round).size()));
            //if (d.members.size() - d.order.get(d.round).size() != 1 && isEnabled)
            b.execute();
        }
    }

    /*public void doc(Tierlist tierlist, String pokemon, Draft d, Member mem, String tier, JSONObject league, int pk) {
        zbsdoc(tierlist, pokemon, d, mem, tier, num);
        //aslnocoachdoc(tierlist, pokemon, d, mem, tier, league, pk);
    }*/

    private static void wooloodoc(Tierlist tierlist, String pokemon, Draft d, Member mem, int needed, @Nullable DraftPokemon removed, int num, int round) {
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
            //System.out.println(s + " " + y);
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
                //System.out.println(s + " " + y);
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
        //System.out.println(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));

        req.setUpdateCells(new UpdateCellsRequest().setRows(Collections.singletonList(new RowData()
                        .setValues(Collections.singletonList(new CellData()
                                .setUserEnteredFormat(new CellFormat()
                                        .setTextFormat(new TextFormat().setStrikethrough(true)))))))
                .setFields("userEnteredFormat.textFormat.strikethrough").setRange(new GridRange().setSheetId(1316641169).setStartRowIndex(num + 1).setEndRowIndex(num + 2).setStartColumnIndex(round).setEndColumnIndex(round + 1)));
        //System.out.println(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
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
            //System.out.println(s + " " + y);
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
                //System.out.println(s + " " + y);
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
        //System.out.println(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));


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
