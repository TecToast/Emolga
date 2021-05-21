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
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

public class SwitchCommand extends Command {

    public SwitchCommand() {
        super("switch", "Switcht ein Pokemon", CommandCategory.Draft);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("oldmon", "Altes Mon", "Das Pokemon, was rausgeschmissen werden soll", ArgumentManagerTemplate.withPredicate("Pokemon", s -> getDraftGerName(s).isFromType(Translation.Type.POKEMON), false, draftnamemapper), false, "Das, was du rauswerfen möchtest, ist kein Pokemon!")
                .add("newmon", "Neues Mon", "Das Pokemon, was stattdessen reinkommen soll", ArgumentManagerTemplate.withPredicate("Pokemon", s -> getDraftGerName(s).isFromType(Translation.Type.POKEMON), false, draftnamemapper), false, "Das, was du haben möchtest, ist kein Pokemon!")
                .setExample("!switch Gufa Emolga").build());
    }

    private static void asldoc(Tierlist tierlist, String pokemon, Draft d, Member mem, int needed, DraftPokemon removed) {
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
        ArrayList<DraftPokemon> picks = d.picks.get(mem);
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
        String msg = e.getMsg();
        TextChannel tco = e.getChannel();
        Member member = e.getMember();
        if (msg.equals("!switch")) {
            e.reply("Willst du vielleicht noch zwei Pokemon dahinter schreiben? xD");
            return;
        }
        Draft d = Draft.getDraftByMember(member, tco);
        if (d == null) {
            tco.sendMessage(member.getAsMention() + " Du bist in keinem Draft drin!").queue();
            return;
        }
        if (!d.tc.getId().equals(tco.getId())) return;
        if (!d.isSwitchDraft) {
            e.reply("Dieser Draft ist kein Switch-Draft, daher wird !switch nicht unterstützt!");
            return;
        }
        if (d.isNotCurrent(member)) {
            tco.sendMessage(d.getMention(member) + " Du bist nicht dran!").queue();
            return;
        }
        Member mem = d.current;
        JSONObject json = getEmolgaJSON();
        JSONObject league = json.getJSONObject("drafts").getJSONObject(d.name);
        //JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject("ZBSL2");
            /*if (asl.has("allowed")) {
                JSONObject allowed = asl.getJSONObject("allowed");
                if (allowed.has(member.getId())) {
                    mem = d.tc.getGuild().retrieveMemberById(allowed.getString(member.getId())).complete();
                } else mem = member;
            } else mem = member;*/
        String tier;
        Translation t;
        ArgumentManager args = e.getArguments();
        String oldmon = args.getText("oldmon");
        String newmon = args.getText("newmon");
        Tierlist tierlist = d.getTierlist();
        if (!d.isPickedBy(oldmon, mem)) {
            e.reply(member.getAsMention() + " " + oldmon + " befindet sich nicht in deinem Kader!");
            return;
        }
        if (d.isPicked(newmon)) {
            e.reply(member.getAsMention() + " " + newmon + " wurde bereits gepickt!");
            return;
        }
        int pointsBack = tierlist.getPointsNeeded(oldmon);
        if (pointsBack == -1) {
            e.reply("Das, was du rauswerfen möchtest, steht nicht in der Tierliste!");
            return;
        }
        d.points.put(mem, d.points.get(mem) + pointsBack);
        System.out.println("oldmon = " + oldmon);
        System.out.println("newmon = " + newmon);
        int newpoints = tierlist.getPointsNeeded(newmon);
        if (newpoints == -1) {
            e.reply("Das, was du haben möchtest, steht nicht in der Tierliste!");
            return;
        }
        if (d.points.get(mem) - newpoints < 0) {
            e.reply(member.getAsMention() + " Du kannst dir " + newmon + " nicht leisten!");
            return;
        }
        d.points.put(mem, d.points.get(mem) - newpoints);
        d.picks.get(mem).stream().filter(dp -> dp.name.equalsIgnoreCase(oldmon)).forEach(dp -> {
            dp.setName(newmon);
            dp.setTier(tierlist.getTierOf(newmon));
        });
        //m.delete().queue();
        d.update(mem);
        league.getJSONObject("picks").put(d.current.getId(), d.getTeamAsArray(d.current));
        if (newmon.equals("Emolga")) {
            tco.sendMessage("<:liebenior:827210993141678142> <:liebenior:827210993141678142> <:liebenior:827210993141678142> <:liebenior:827210993141678142> <:liebenior:827210993141678142>").queue();
        }
        try {
            d.cooldown.cancel();
        } catch (Exception ignored) {

        }
        int round = d.round;
        if (d.order.get(d.round).size() == 0) {
            if (d.round == 4) {
                tco.sendMessage("Der Draft ist vorbei!").queue();
                d.ended = true;
                //wooloodoc(tierlist, pokemon, d, mem, needed, null, num, round);
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
        d.current = d.order.get(d.round).remove(0);
        league.put("current", d.current.getId());
        tco.sendMessage(d.getMention(d.current) + " ist dran! (" + d.points.get(d.current) + " mögliche Punkte)").queue();
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
        asldoc(tierlist, newmon, d, mem, newpoints, new DraftPokemon(oldmon, tierlist.getTierOf(oldmon)));
    }
}
