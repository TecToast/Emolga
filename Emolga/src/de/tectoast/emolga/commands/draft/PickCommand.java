package de.tectoast.emolga.commands.draft;

import com.google.api.services.sheets.v4.model.*;
import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.utils.CommandEvent;
import de.tectoast.emolga.utils.RequestBuilder;
import de.tectoast.emolga.utils.draft.Draft;
import de.tectoast.emolga.utils.draft.DraftPokemon;
import de.tectoast.emolga.utils.draft.Tierlist;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class PickCommand extends Command {
    public static boolean isEnabled = true;

    public PickCommand() {
        super("pick", "`!pick <Pokemon>` Pickt das pokemon", CommandCategory.Draft);
    }

    public static void exec(TextChannel tco, String msg, Member member, boolean isRandom) {
        try {
            Draft d = Draft.getDraftByMember(member, tco);
            if (d == null) {
                tco.sendMessage(member.getAsMention() + " Du bist in keinem Draft drin!").queue();
                return;
            }
            if (!d.tc.getId().equals(tco.getId())) return;
            if (d.isNotCurrent(member)) {
                tco.sendMessage(d.getMention(member) + " Du bist nicht dran!").queue();
                return;
            }
            Member mem;
            JSONObject json = getEmolgaJSON();
            JSONObject league = json.getJSONObject("drafts").getJSONObject(d.name);
            //JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject("ZBSL2");
            /*if (asl.has("allowed")) {
                JSONObject allowed = asl.getJSONObject("allowed");
                if (allowed.has(member.getId())) {
                    mem = d.tc.getGuild().retrieveMemberById(allowed.getString(member.getId())).complete();
                } else mem = member;
            } else mem = member;*/
            if (d.current.getId().equals(member.getId())) mem = member;
            else {
                mem = tco.getGuild().retrieveMemberById(league.getJSONObject("allowed").getString(member.getId())).complete();
            }
            Member finalMem = mem;
            String[] split = msg.substring(6).split(" ");
            String tier;
            String pokemon;
            Tierlist tierlist = d.getTierlist();
            if (split.length == 2 && !d.isPointBased) {
                pokemon = getDraftGerName(split[0]);
                if (!pokemon.startsWith("pkmn;")) {
                    tco.sendMessage("Das ist kein Pokemon!").queue();
                    return;
                }
                pokemon = pokemon.substring(5);
                tier = split[1];
            } else {
                pokemon = getDraftGerName(split[0]);
                if (!pokemon.startsWith("pkmn;")) {
                    tco.sendMessage("Das ist kein Pokemon!").queue();
                    return;
                }
                pokemon = pokemon.substring(5);
                tier = tierlist.getTierOf(pokemon);
            }
            if (d.isPicked(pokemon)) {
                tco.sendMessage(member.getAsMention() + " Dieses Pokemon wurde bereits gepickt!").queue();
                return;
            }
            int needed = tierlist.getPointsNeeded(pokemon);
            if (d.isPointBased) {
                if (needed == -1) {
                    tco.sendMessage(member.getAsMention() + " Das ist kein Pokemon!").queue();
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
            if (d.hasMega(mem) && pokemon.startsWith("M-")) {
                tco.sendMessage(member.getAsMention() + " Du hast bereits ein Mega!").complete().getId();
                return;
            }
            if (d.hasInAnotherForm(mem, pokemon)) {
                tco.sendMessage(member.getAsMention() + " Damit würdest du gegen die Species Clause verstoßen!").queue();
                return;
            }
            if (d.isPointBased && d.points.get(mem) - needed < 0) {
                tco.sendMessage(member.getAsMention() + " Dafür hast du nicht genug Punkte!").queue();
                return;
            }
            if (d.isPointBased)
                d.points.put(mem, d.points.get(mem) - needed);
            d.picks.get(mem).add(new DraftPokemon(pokemon, tier));
            if (!league.has("picks"))
                league.put("picks", new JSONObject());
            if (!league.getJSONObject("picks").has(mem.getId()))
                league.getJSONObject("picks").put(mem.getId(), new JSONArray());
            if (d.isPointBased && !league.has("points"))
                league.put("points", new JSONObject());
            if (!league.getJSONObject("picks").has(mem.getId()) && d.isPointBased)
                league.getJSONObject("points").put(mem.getId(), 1000);
            league.getJSONObject("picks").put(mem.getId(), d.getTeamAsArray(mem));
            if (d.isPointBased)
                league.getJSONObject("points").put(mem.getId(), d.points.get(mem));
            //m.delete().queue();
            d.update(mem);
            if (isRandom) {
                tco.sendMessage("**" + mem.getEffectiveName() + "** hat aus dem " + tier + "-Tier ein **" + pokemon + "** bekommen!").queue();
            }
            try {
                d.cooldown.cancel();
            } catch (Exception ignored) {

            }
            int num = 8 - d.order.get(d.round).size();
            int round = d.round;
            if (d.order.get(d.round).size() == 0) {
                if (d.round == 12) {
                    tco.sendMessage("Der Draft ist vorbei!").queue();
                    d.ended = true;
                    if(d.afterDraft.size() > 0)
                    tco.sendMessage("Reihenfolge zum Nachdraften:\n" + d.afterDraft.stream().map(d::getMention).collect(Collectors.joining("\n"))).queue();
                    saveEmolgaJSON();
                    Draft.drafts.remove(d);
                    return;
                }
                d.round++;
                d.tc.sendMessage("Runde " + d.round + "!").queue();
                league.put("round", d.round);
            }
            d.current = d.order.get(d.round).remove(0);
            league.put("current", d.current.getId());
            DraftPokemon toremove = null;
            if (d.isPointBased && d.points.get(d.current) < 20) {
                ArrayList<DraftPokemon> picks = d.picks.get(d.current);
                int price = 0;
                for (DraftPokemon pick : picks) {
                    int pr = tierlist.getPointsNeeded(pick.name);
                    if (pr > price) {
                        price = pr;
                        toremove = pick;
                    }
                }
                tco.sendMessage(d.getMention(d.current) + " Du hast nicht mehr genug Punkte um ein weiteres Pokemon zu draften! Deshalb verlierst du " + toremove.name + " und erhältst dafür " + price / 2 + " Punkte!").queue();
                d.points.put(d.current, d.points.get(d.current) + price / 2);
                d.picks.get(d.current).remove(toremove);
                league.getJSONObject("picks").put(d.current.getId(), d.getTeamAsArray(d.current));
                d.afterDraft.add(d.current);
            }
            if (d.isPointBased)
                tco.sendMessage(d.getMention(d.current) + " ist dran! (" + d.points.get(d.current) + " mögliche Punkte)").complete().getId();
            else
                tco.sendMessage(d.getMention(d.current) + " ist dran! (Mögliche Tiers: " + d.getPossibleTiersAsString(d.current) + ")").complete().getId();
            d.cooldown = new Timer();
            long delay = calculateASLTimer();
            league.put("cooldown", System.currentTimeMillis() + delay);
            d.cooldown.schedule(new TimerTask() {
                @Override
                public void run() {
                    d.timer();
                }
            }, delay);
            saveEmolgaJSON();
            asldoc(tierlist, pokemon, d, mem, needed, toremove);
        } catch (Exception ex) {
            tco.sendMessage("Es ist ein Fehler aufgetreten!").queue();
            ex.printStackTrace();
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

            Request request = new Request();
            request.setUpdateCells(new UpdateCellsRequest().setRows(Collections.singletonList(new RowData()
                    .setValues(Collections.singletonList(new CellData()
                            .setUserEnteredFormat(new CellFormat()
                                    .setTextFormat(new TextFormat().setStrikethrough(true)))))))
                    .setFields("userEnteredFormat.textFormat.strikethrough").setRange(new GridRange().setSheetId(2048730181).setStartRowIndex(y - 1).setEndRowIndex(y).setStartColumnIndex(x * 2).setEndColumnIndex(x * 2 + 1)));
            Request req = new Request();
            //System.out.println(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));

            req.setUpdateCells(new UpdateCellsRequest().setRows(Collections.singletonList(new RowData()
                    .setValues(Collections.singletonList(new CellData()
                            .setUserEnteredFormat(new CellFormat()
                                    .setTextFormat(new TextFormat().setStrikethrough(true)))))))
                    .setFields("userEnteredFormat.textFormat.strikethrough").setRange(new GridRange().setSheetId(856868721).setStartRowIndex(num + 13).setEndRowIndex(num + 14).setStartColumnIndex(d.round + 1).setEndColumnIndex(d.round + 2)));
            int user = Arrays.asList(league.getString("table").split(",")).indexOf(mem.getId());
            String range = "Liga 2!" + (char) (tier.equals("OU") ? (76 + d.picks.get(mem).stream().filter(p -> p.tier.equals("OU")).count()) : (tierlist.order.indexOf(tier) * 3 + 75 + d.picks.get(mem).stream().filter(p -> p.tier.equals(tier)).count())) + (user + 3);
            System.out.println("range = " + range);
            RequestBuilder b = new RequestBuilder(doc);
            b.addBatch(request, req).addSingle(range, getGen5Sprite(pokemon));
            System.out.println("d.members.size() = " + d.members.size());
            System.out.println("d.order.size() = " + d.order.get(d.round).size());
            System.out.println("d.members.size() - d.order.size() = " + (d.members.size() - d.order.get(d.round).size()));
            //if (d.members.size() - d.order.get(d.round).size() != 1 && isEnabled)
            b.execute();
        }
    }

    /*public void doc(Tierlist tierlist, String pokemon, Draft d, Member mem, String tier, JSONObject league, int pk) {
        zbsdoc(tierlist, pokemon, d, mem, tier, num);
        //asldoc(tierlist, pokemon, d, mem, tier, league, pk);
    }*/

    private static void asldoc(Tierlist tierlist, String pokemon, Draft d, Member mem, int needed, @Nullable DraftPokemon removed) {
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
        if(removed != null) {
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
        ArrayList<DraftPokemon> picks = d.picks.get(mem);
        for (int i = 0; i < 12; i++) {
            List<Object> list = new ArrayList<>();
            if(i < picks.size()) {
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
    public void process(CommandEvent e) {
        exec(e.getChannel(), e.getMessage().getContentDisplay(), e.getMember(), false);
    }
}
