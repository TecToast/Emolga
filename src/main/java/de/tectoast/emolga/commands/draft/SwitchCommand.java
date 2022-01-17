package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.RequestBuilder;
import de.tectoast.emolga.utils.draft.Draft;
import de.tectoast.emolga.utils.draft.DraftPokemon;
import de.tectoast.emolga.utils.draft.Tierlist;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jsolf.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.tectoast.emolga.utils.draft.Draft.getIndex;

public class SwitchCommand extends Command {

    private static final Logger logger = LoggerFactory.getLogger(SwitchCommand.class);

    public SwitchCommand() {
        super("switch", "Switcht ein Pokemon", CommandCategory.Draft);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("oldmon", "Altes Mon", "Das Pokemon, was rausgeschmissen werden soll", ArgumentManagerTemplate.withPredicate("Pokemon", s -> getDraftGerName(s).isFromType(Translation.Type.POKEMON), false, draftnamemapper), false, "Das, was du rauswerfen möchtest, ist kein Pokemon!")
                .add("newmon", "Neues Mon", "Das Pokemon, was stattdessen reinkommen soll", ArgumentManagerTemplate.withPredicate("Pokemon", s -> getDraftGerName(s).isFromType(Translation.Type.POKEMON), false, draftnamemapper), false, "Das, was du haben möchtest, ist kein Pokemon!")
                .setExample("!switch Gufa Emolga").build());
    }

    private static void asldoc(Tierlist tierlist, String pokemon, Draft d, long mem, int needed, DraftPokemon removed) {
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
            b.addBGColorChange(league.getInt("tierlist"), x * 2, y, convertColor(0xFF0000));
        }
        x = 1;
        y = 5;
        found = false;
        for (String s : tierlist.tiercolumns) {
            if (s.equalsIgnoreCase(removed.name)) {
                found = true;
                break;
            }
            //logger.info(s + " " + y);
            if (s.equals("NEXT")) {
                x++;
                y = 5;
            } else y++;
        }
        if (found)
            b.addBGColorChange(league.getInt("tierlist"), x * 2, y, convertColor(0x93c47d));

        //logger.info(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
        String team = asl.getStringList("teams").get(getIndex(mem));
        List<DraftPokemon> picks = d.picks.get(mem);
        int index = IntStream.range(0, picks.size()).filter(i -> {
            logger.info("picks.get(" + i + ") = " + picks.get(i));
            return picks.get(i).name.equals(pokemon);
        }).findFirst().orElse(-1);
        int yc = (Draft.getLevel(mem) * 20 + index + 1);
        List<Integer> list = new LinkedList<>();
        for (int i = 0; i < 10; i++) {
            list.add(i * 5 + 10);
        }
        b.addRow(team + "!B" + yc, Arrays.asList(getGen5Sprite(pokemon), pokemon, needed,
                "=SUMME(" + list.stream().map(i -> getAsXCoord(i) + yc).collect(Collectors.joining(";")) + ")",
                "=SUMME(" + list.stream().map(i -> getAsXCoord(i + 1) + yc).collect(Collectors.joining(";")) + ")",
                ("=E" + yc + " - F" + yc), getDataJSON().getJSONObject(getSDName(pokemon)).getJSONObject("baseStats").getInt("spe")));
        b.execute();
    }

    private static void fpldoc(Tierlist tierlist, String pokemon, Draft d, long mem, String tier, int num, int round, String removed) {
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject(d.name);
        if (league.has("sid")) {
            String doc = league.getString("sid");
            logger.info("num = " + num);
            RequestBuilder b = new RequestBuilder(doc);
            Pair<Integer, Integer> ncoords = tierlist.getLocation(pokemon, 1, 3);
            b.addStrikethroughChange(league.getInt("tierlist"), ncoords.getLeft() * 2, ncoords.getRight(), true);
            Pair<Integer, Integer> ocoords = tierlist.getLocation(removed, 1, 3);
            b.addStrikethroughChange(league.getInt("tierlist"), ocoords.getLeft() * 2, ocoords.getRight(), false);
            //logger.info(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
            b.addStrikethroughChange(league.getInt("draftorder"), d.round + 1, num + 6, true);
            int user = league.getLongList("table").indexOf(mem);
            String range = "Kader %s!%s%d".formatted(d.name.substring(5), getAsXCoord((user / 4) * 22 + 2), (user % 4) * 20 + 8 + d.picks.get(mem).stream().filter(dp -> dp.getName().equals(pokemon)).map(dp -> d.picks.get(mem).indexOf(dp)).findFirst().orElse(-1));
            logger.info("range = " + range);
            b.addRow(range, Arrays.asList(tier, "", pokemon, "", getDataJSON().getJSONObject(getSDName(pokemon)).getJSONObject("baseStats").getInt("spe")));
            logger.info("d.members.size() = " + d.members.size());
            logger.info("d.order.size() = " + d.order.get(d.round).size());
            logger.info("d.members.size() - d.order.size() = " + (d.members.size() - d.order.get(d.round).size()));
            //if (d.members.size() - d.order.get(d.round).size() != 1 && isEnabled)
            b.execute();
        }
    }

    @Override
    public void process(GuildCommandEvent e) {
        String msg = e.getMsg();
        TextChannel tco = e.getChannel();
        Member memberr = e.getMember();
        long member = memberr.getIdLong();
        Draft d = Draft.getDraftByMember(member, tco);
        if (d == null) {
            tco.sendMessage(memberr.getAsMention() + " Du bist in keinem Draft drin!").queue();
            return;
        }
        if (!d.tc.getId().equals(tco.getId())) return;
        if (!d.isSwitchDraft) {
            e.reply("Dieser Draft ist kein Switch-Draft, daher wird !switch nicht unterstützt!");
            return;
        }
        if (msg.equals("!switch")) {
            e.reply("Willst du vielleicht noch zwei Pokemon dahinter schreiben? xD");
            return;
        }
        if (d.isNotCurrent(member)) {
            tco.sendMessage(d.getMention(member) + " Du bist nicht dran!").queue();
            return;
        }
        long mem = d.current;
        JSONObject json = getEmolgaJSON();
        JSONObject league = json.getJSONObject("drafts").getJSONObject(d.name);
        //JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject("ZBSL2");
            /*if (asl.has("allowed")) {
                JSONObject allowed = asl.getJSONObject("allowed");
                if (allowed.has(member.getId())) {
                    mem = d.tc.getGuild().retrieveMemberById(allowed.getString(member.getId())).complete();
                } else mem = member;
            } else mem = member;*/
        Translation t;
        ArgumentManager args = e.getArguments();
        String oldmon = args.getText("oldmon");
        String newmon = args.getText("newmon");
        Tierlist tierlist = d.getTierlist();
        if (!d.isPickedBy(oldmon, mem)) {
            e.reply(memberr.getAsMention() + " " + oldmon + " befindet sich nicht in deinem Kader!");
            return;
        }
        if (d.isPicked(newmon)) {
            e.reply(memberr.getAsMention() + " " + newmon + " wurde bereits gepickt!");
            return;
        }
        int pointsBack = tierlist.getPointsNeeded(oldmon);
        if (pointsBack == -1) {
            e.reply("Das, was du rauswerfen möchtest, steht nicht in der Tierliste!");
            return;
        }
        logger.info("oldmon = " + oldmon);
        logger.info("newmon = " + newmon);
        int newpoints = tierlist.getPointsNeeded(newmon);
        if (newpoints == -1) {
            e.reply("Das, was du haben möchtest, steht nicht in der Tierliste!");
            return;
        }
        String tier = tierlist.getTierOf(newmon);
        d.points.put(mem, d.points.get(mem) + pointsBack);
        if (d.points.get(mem) - newpoints < 0) {
            d.points.put(mem, d.points.get(mem) - pointsBack);
            e.reply(memberr.getAsMention() + " Du kannst dir " + newmon + " nicht leisten!");
            return;
        }
        d.points.put(mem, d.points.get(mem) - newpoints);
        d.picks.get(mem).stream().filter(dp -> dp.name.equalsIgnoreCase(oldmon)).forEach(dp -> {
            dp.setName(newmon);
            dp.setTier(tierlist.getTierOf(newmon));
        });
        //m.delete().queue();
        d.update(mem);
        //fpldoc(tierlist, newmon, d, mem, newpoints, new DraftPokemon(oldmon, tierlist.getTierOf(oldmon)));
        fpldoc(tierlist, newmon, d, mem, tier, d.members.size() - d.order.get(d.round).size(), d.round, oldmon);
        league.getJSONObject("picks").put(d.current, d.getTeamAsArray(d.current));
        if (newmon.equals("Emolga")) {
            tco.sendMessage("<:liebenior:827210993141678142> <:liebenior:827210993141678142> <:liebenior:827210993141678142> <:liebenior:827210993141678142> <:liebenior:827210993141678142>").queue();
        }
        try {
            d.cooldown.cancel();
        } catch (Exception ignored) {

        }
        int round = d.round;
        if (d.order.get(d.round).size() == 0) {
            if (d.round == tierlist.rounds) {
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
        league.put("current", d.current);
        JSONObject drafts = getEmolgaJSON().getJSONObject("drafts");
        tco.sendMessage(d.getMention(d.current) + " ist dran! (" + d.points.get(d.current) + " mögliche Punkte)").queue();
        //tco.sendMessage(d.getMention(d.current) + " ist dran! (" + d.points.get(d.current.getIdLong()) + " mögliche Punkte)").queue();
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
    }
}
