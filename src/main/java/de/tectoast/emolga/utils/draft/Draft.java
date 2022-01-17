package de.tectoast.emolga.utils.draft;


import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.Google;
import de.tectoast.emolga.utils.RequestBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jsolf.JSONArray;
import org.jsolf.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.tectoast.emolga.bot.EmolgaMain.emolgajda;
import static de.tectoast.emolga.commands.Command.*;

public class Draft {
    public static final List<Draft> drafts = new ArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger(Draft.class);
    public final HashMap<Long, List<DraftPokemon>> picks = new HashMap<>();
    public final HashMap<Integer, List<Long>> order = new HashMap<>();
    public final HashMap<Long, Integer> points = new HashMap<>();
    public final List<Long> afterDraft = new ArrayList<>();
    public final TextChannel tc;
    public final String name;
    public final String guild;
    public final boolean isPointBased;
    public final boolean isSwitchDraft;
    public final LinkedList<Long> finished = new LinkedList<>();
    public List<Long> members;
    public long current;
    public int round = 0;
    public Timer cooldown = new Timer();
    public TextChannel ts;
    public boolean ended = false;

    public Draft(TextChannel tc, String name, String tcid, boolean fromFile) {
        this(tc, name, tcid, fromFile, false);
    }

    public Draft(TextChannel tc, String name, String tcid, boolean fromFile, boolean isSwitchDraft) {
        this.isSwitchDraft = isSwitchDraft;
        this.tc = tc;
        this.name = name;
        JSONObject json = getEmolgaJSON();
        JSONObject drafts = json.getJSONObject("drafts");
        Pattern aslpattern = Pattern.compile("^S\\d");
        JSONObject league = aslpattern.matcher(name).find() ? drafts.getJSONObject("ASLS9").getJSONObject(name) : drafts.getJSONObject(name);
        this.guild = league.has("guild") ? league.getString("guild") : tc.getGuild().getId();
        isPointBased = getTierlist().isPointBased;
        logger.info("isPointBased = " + isPointBased);
        JSONObject o = league.getJSONObject("order");
        new Thread(() -> {
            logger.info(name);
            for (String i : o.keySet()) {
                JSONArray arr = o.getJSONArray(i);
                List<Long> list;
                if (arr.get(0) instanceof JSONArray) {
                    list = arr.toLongListList().stream().map(l -> l.get(0)).collect(Collectors.toCollection(LinkedList::new));
                } else {
                    list = arr.toLongList();
                }
                order.put(Integer.valueOf(i), list);
            }
            logger.info("order = " + order);
            this.members = new ArrayList<>(order.get(1));
            ts = tcid != null ? tc.getGuild().getTextChannelById(tcid) : null;
            if (!fromFile && !isSwitchDraft) {
                round++;
                for (long member : members) {
                    picks.put(member, new ArrayList<>());
                    points.put(member, getTierlist().points);
                }
                if (ts != null) {
                    for (long member : members) {
                        ts.sendMessage("**<@" + member + ">:**").queue();
                    }
                }
                current = order.get(1).remove(0);
                cooldown.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        timer();
                    }
                }, calculateASLTimer());
                tc.sendMessage("Runde " + round + "!").queue();
                if (isPointBased)
                    tc.sendMessage(getMention(current) + " ist dran! (" + points.get(current) + " mögliche Punkte)").queue();
                else
                    tc.sendMessage(getMention(current) + " ist dran! (Mögliche Tiers: " + getPossibleTiersAsString(current) + ")").complete().getId();
            } else {
                if (isSwitchDraft && !fromFile) {
                    round++;
                    current = order.get(1).remove(0);
                    JSONObject pick = league.getJSONObject("picks");
                    for (long member : members) {
                        logger.info("member = " + member);
                        if (pick.has(member)) {
                            JSONArray arr = pick.getJSONArrayL(member);
                            ArrayList<DraftPokemon> list = new ArrayList<>();
                            for (Object ob : arr) {
                                JSONObject obj = (JSONObject) ob;
                                list.add(new DraftPokemon(obj.getString("name"), obj.getString("tier")));
                            }
                            picks.put(member, list);
                            update(member);
                            logger.info("update end");
                        } else {
                            picks.put(member, new ArrayList<>());
                        }
                        if (isPointBased) {
                            points.put(member, getTierlist().points);
                            for (DraftPokemon mon : picks.get(member)) {
                                points.put(member, points.get(member) - getTierlist().prices.get(mon.tier));
                            }
                        }
                    }
                    logger.info("For finished");
                    long delay = calculateASLTimer();
                    league.put("cooldown", System.currentTimeMillis() + delay);
                    if (delay != -1) {
                        cooldown.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                timer();
                            }
                        }, delay);
                    }
                    logger.info("Before send");
                    tc.sendMessage("Runde " + round + "!").queue();
                    if (isPointBased)
                        tc.sendMessage(getMention(current) + " ist dran! (" + points.get(current) + " mögliche Punkte)").queue();
                    else
                        tc.sendMessage(getMention(current) + " ist dran! (Mögliche Tiers: " + getPossibleTiersAsString(current) + ")").complete().getId();
                    saveEmolgaJSON();
                } else {
                    round = league.optInt("round", 1);
                    current = league.getLong("current");
                    int x = 0;
                    for (long member : order.get(round)) {
                        x++;
                        if (current == member) break;
                    }
                    if (x > 0) {
                        order.get(round).subList(0, x).clear();
                    }
                    if (league.has("finished")) {
                        Arrays.asList(league.getString("finished").split(",")).forEach(s -> order.values().forEach(l -> l.removeIf(me -> me == Long.parseLong(s))));
                    }
                    logger.info("order.size() = " + order.get(round).size());
                    JSONObject pick = league.getJSONObject("picks");
                    for (long member : members) {
                        if (pick.has(member)) {
                            JSONArray arr = pick.getJSONArrayL(member);
                            ArrayList<DraftPokemon> list = new ArrayList<>();
                            for (Object ob : arr) {
                                JSONObject obj = (JSONObject) ob;
                                list.add(new DraftPokemon(obj.getString("name"), obj.getString("tier")));
                            }
                            picks.put(member, list);
                            update(member);
                        } else {
                            picks.put(member, new ArrayList<>());
                        }
                        if (isPointBased) {
                            points.put(member, getTierlist().points);
                            for (DraftPokemon mon : picks.get(member)) {
                                Tierlist t = getTierlist();
                                if (t == null) logger.info("TIERLISTNULL");
                                if (t.prices.get(mon.tier) == null) {
                                    logger.info(mon.name + " ERROR " + mon.tier);
                                }
                                points.put(member, points.get(member) - t.prices.get(mon.tier));
                            }
                        }
                    }
                    long delay;
                    if (league.has("cooldown")) {
                        delay = league.getLong("cooldown") - System.currentTimeMillis();
                    } else {
                        delay = calculateASLTimer();
                    }
                    if (!league.has("cooldown"))
                        league.put("cooldown", System.currentTimeMillis() + delay);
                    if (delay != -1) {
                        cooldown.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                timer();
                            }
                        }, delay);
                    }
                    saveEmolgaJSON();
                    sendToMe("Aufgesetzt! " + delay);
                }
            }
            Draft.drafts.add(this);
            logger.info("Initialized Draft " + name + " !");
        }, "CreateDraft " + name).start();
    }

    public static void setupNDSNominate() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        while (c.get(Calendar.DAY_OF_WEEK) != Calendar.TUESDAY) c.add(Calendar.DAY_OF_WEEK, 1);
        long timeUntilStart = c.getTimeInMillis() - System.currentTimeMillis();
        //TextChannel tc = jda.getTextChannelById(predictiongame.getLong("channelid"));
        //TextChannel tc = jda.getTextChannelById(774661698074050581L);
        //for (int i = 0; i < 4; i++) {
        int i = 0;
        /*long delay = timeUntilStart + (i * 604_800_000L);
        delay = 5000;*/
        //ndsnominates.schedule(() -> {
        JSONObject nds = getEmolgaJSON().getJSONObject("drafts").getJSONObject("NDS");
        JSONObject nom = nds.getJSONObject("nominations");
        int cday = nom.getInt("currentDay");
        JSONObject o = nom.getJSONObject(String.valueOf(cday));
        JSONObject picks = nds.getJSONObject("picks");
        String sid = nds.getString("sid");
        RequestBuilder builder = new RequestBuilder(sid);
        for (String u : picks.keySet()) {
            //String u = "297010892678234114";
            if (!o.has(u)) {
                if (cday == 1) {
                    List<JSONObject> mons = arrayToList(picks.getJSONArray(u), JSONObject.class);
                    StringBuilder b = new StringBuilder();
                    Tierlist tierlist = Tierlist.getByGuild(Constants.NDSID);
                    for (int j = 0; j < 11; j++) {
                        JSONObject obj = mons.get(j);
                        b.append(obj.getString("name")).append(",").append(tierlist.getPointsNeeded(obj.getString("name"))).append(";");
                    }
                    b.setLength(b.length() - 1);
                    b.append("###");
                    for (int j = 11; j < mons.size(); j++) {
                        JSONObject obj = mons.get(j);
                        b.append(obj.getString("name")).append(",").append(tierlist.getPointsNeeded(obj.getString("name"))).append(";");
                    }
                    b.setLength(b.length() - 1);
                    o.put(u, b.toString());
                } else {
                    o.put(u, nom.getJSONObject(String.valueOf(cday - 1)).getString(u));
                }
            }
            //logger.info("o.get(u) = " + o.get(u));
            String str = o.getString(u);
            List<String> mons = Arrays.stream(str.split("###")).flatMap(s -> Arrays.stream(s.split(";"))).map(s -> s.split(",")[0]).collect(Collectors.toList());
            logger.info("mons = " + mons);
            String range = nds.getJSONObject("teamnames").getString(u) + "!B15:O29";
            logger.info("u = " + u);
            logger.info("range = " + range);
            builder.addAll(range, Google.get(sid, range, true, false).stream().filter(n -> !n.get(2).equals("")).sorted(Comparator.comparing(n -> {

                String s1 = (String) n.get(2);
                logger.info("s1 = " + s1);
                int ret = mons.indexOf(s1);
                logger.info("ret = " + ret);
                return ret;
            })).collect(Collectors.toList()));
        }
        builder.suppressMessages().execute();
        // }, delay, TimeUnit.MILLISECONDS);
        //}
    }


    public static void doNDSPredictionGame() {
        JSONObject json = getEmolgaJSON();
        JSONObject league = json.getJSONObject("drafts").getJSONObject("NDS");
        int lastDay = league.getInt("lastDay");
        int gameday = lastDay + 1;
        league.put("lastDay", lastDay + 1);
        TextChannel tc = emolgajda.getTextChannelById(844806306027929610L);
        tc.sendMessage("**Spieltag " + gameday + ":**").queue();
        String bo = league.getJSONObject("battleorder").getString(String.valueOf(gameday));
        Guild g = emolgajda.getGuildById(Constants.NDSID);
        for (String str : bo.split(";")) {
            String[] split = str.split(":");
            String u1 = split[0];
            String u2 = split[1];
            logger.info("u1 = " + u1);
            logger.info("u2 = " + u2);
            JSONObject teamnames = league.getJSONObject("teamnames");
            String t1 = teamnames.getString(u1);
            String t2 = teamnames.getString(u2);
            logger.info("t1 = " + t1);
            logger.info("t2 = " + t2);
            Emote e1 = g.getEmotesByName(toSDName(t1), true).get(0);
            Emote e2 = g.getEmotesByName(toSDName(t2), true).get(0);
            //logger.info("<@" + u1 + "> (" + e1.getAsMention() + ") vs. <@" + u2 + "> (" + e2.getAsMention() + ")");
            tc.sendMessage("<@" + u1 + "> (" + e1.getAsMention() + ") vs. <@" + u2 + "> (" + e2.getAsMention() + ")").queue(m -> {
                m.addReaction(e1).queue();
                m.addReaction(e2).queue();
            });
        }
    }


    public static void initializeNDSPredictionGame() {
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject("NDS");
        int lastDay = league.getInt("lastDay");
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 20);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        boolean b = System.currentTimeMillis() - c.getTimeInMillis() >= 0;
        while (c.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) c.add(Calendar.DAY_OF_WEEK, 1);
        long timeUntilStart = c.getTimeInMillis() - System.currentTimeMillis();
        TextChannel tc = emolgajda.getTextChannelById(844806306027929610L);
        //TextChannel tc = jda.getTextChannelById(774661698074050581L);
        for (int i = 0; i < league.getJSONObject("battleorder").length() - lastDay; i++) {
            long delay = timeUntilStart + (i * 604800000L);
            if (i == 0 && b) continue;
            logger.info("NDS" + " -> " + i + " -> " + delay + " -> " + new SimpleDateFormat().format(new Date(System.currentTimeMillis() + delay)));
            int finalI = i + 1;
            logger.info("finalI = " + finalI);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    JSONObject json = getEmolgaJSON();
                    JSONObject league = json.getJSONObject("drafts").getJSONObject("NDS");
                    int lastDay = league.getInt("lastDay");
                    int gameday = lastDay + finalI;
                    league.put("lastDay", lastDay + 1);
                    //tc.sendMessage("**Spieltag " + gameday + ":**").queue();
                    String bo = league.getJSONObject("battleorder").getString(String.valueOf(gameday));
                    Guild g = emolgajda.getGuildById(Constants.NDSID);
                    for (String str : bo.split(";")) {
                        String[] split = str.split(":");
                        String u1 = split[0];
                        String u2 = split[1];
                        JSONObject teamnames = league.getJSONObject("teamnames");
                        Emote e1 = g.getEmotesByName(toSDName(teamnames.getString(u1)), true).get(0);
                        Emote e2 = g.getEmotesByName(toSDName(teamnames.getString(u2)), true).get(0);
                        logger.info("<@" + u1 + "> (" + e1.getAsMention() + ") vs. <@" + u2 + "> (" + e2.getAsMention() + ")");

                        /*tc.sendMessage("<@" + u1 + "> (" + e1.getAsMention() + ") vs. <@" + u2 + "> (" + e2.getAsMention() + ")").queue(m -> {
                            m.addReaction(e1).queue();
                            m.addReaction(e2).queue();
                        });*/
                    }
                }
            }, delay < 0 ? 0 : delay);
        }
    }


    public static void init(JDA jda) {
        Tierlist.setup();
        JSONObject json = getEmolgaJSON();
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy hh:mm:ss", Locale.GERMANY);
        //initializeNDSPredictionGame();
        //setupNDSNominate();
        for (String draftname : json.getJSONObject("drafts").keySet()) {
            JSONObject league = json.getJSONObject("drafts").getJSONObject(draftname);
            if (league.has("predictiongame")) {
                Emote p1emote = jda.getEmoteById(540970044297838597L);
                Emote p2emote = jda.getEmoteById(645622238757781505L);
                JSONObject predictiongame = league.getJSONObject("predictiongame");
                int lastDay = predictiongame.getInt("lastDay");
                Calendar c = Calendar.getInstance();
                c.set(Calendar.HOUR_OF_DAY, 20);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                while (c.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) c.add(Calendar.DAY_OF_WEEK, 1);
                long timeUntilStart = c.getTimeInMillis() - System.currentTimeMillis();
                TextChannel tc = jda.getTextChannelById(predictiongame.getLong("channelid"));
                //TextChannel tc = jda.getTextChannelById(774661698074050581L);
                for (int i = 0; i < league.getJSONObject("battleorder").length() + 7 - lastDay; i++) {
                    long delay = timeUntilStart + (i * 604800000L);
                    logger.info(draftname + " -> " + i + " -> " + delay + " -> " + format.format(new Date(System.currentTimeMillis() + delay)));
                    int finalI = i + 1;
                    logger.info("finalI = " + finalI);
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            JSONObject json = getEmolgaJSON();
                            JSONObject league = json.getJSONObject("drafts").getJSONObject(draftname);
                            JSONObject predictiongame = league.getJSONObject("predictiongame");
                            int lastDay = predictiongame.getInt("lastDay");
                            int gameday = lastDay + finalI;
                            predictiongame.put("lastDay", lastDay + 1);
                            tc.sendMessage("**Spieltag " + gameday + ":**").queue();
                            predictiongame.getJSONObject("ids").put(String.valueOf(gameday), new JSONObject());
                            JSONObject o = predictiongame.getJSONObject("ids").getJSONObject(String.valueOf(gameday));
                            String bo = league.getJSONObject("battleorder").getString(String.valueOf(gameday - 7));
                            ArrayList<CompletableFuture<Message>> list = new ArrayList<>();
                            for (String str : bo.split(";")) {
                                String[] split = str.split(":");
                                String u1 = split[0];
                                String u2 = split[1];
                                list.add(tc.sendMessage("<:yay:540970044297838597> <@" + u1 + "> vs. <@" + u2 + "> <:yay2:645622238757781505>").submit().whenComplete((m, throwable) -> {
                                    try {
                                        m.addReaction(p1emote).queue();
                                        m.addReaction(p2emote).queue();
                                        o.put(u1 + ":" + u2, m.getIdLong());
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                }));
                            }
                            CompletableFuture.allOf(list.toArray(new CompletableFuture[0])).whenComplete((unused, throwable) -> {
                                saveEmolgaJSON();
                                //sendEarlyASLReplays(gameday);
                            });
                        }
                    }, delay < 0 ? 0 : delay);
                }
            }
        }
    }

    /*public static boolean isDraftIn(TextChannel tc) {
        return drafts.stream().anyMatch(d -> d.tc.getId().equals(tc.getId()));
    }*/

    public static Draft getDraftByMember(long member, TextChannel tco) {
        JSONObject json = getEmolgaJSON();
        //logger.info("member.getId() = " + member.getId());
        for (Draft draft : Draft.drafts) {
            //logger.info(draft.members.stream().map(mem -> mem.getId() + ":" + mem.getEffectiveName()).collect(Collectors.joining("\n")));
            if (!draft.tc.getId().equals(tco.getId())) continue;
            if (member == Constants.FLOID) return draft;
            if (member == 690971979821613056L) return draft;
            if (draft.members.stream().anyMatch(mem -> mem == member))
                return draft;
            if (draft.getLeague().has("table1") && draft.getLeague().getJSONArray("table1").toLongListList().stream().anyMatch(l -> l.contains(member)))
                return draft;
            if (draft.getLeague().has("table2") && draft.getLeague().getJSONArray("table2").toLongListList().stream().anyMatch(l -> l.contains(member)))
                return draft;
            /*if (getTeamMembers(member.getIdLong()).stream().anyMatch(l -> draft.members.stream().anyMatch(mem -> mem.getIdLong() == l)))
                return draft;*/
            JSONObject league = draft.getLeague();
            if (league.has("allowed")) {
                JSONObject allowed = league.getJSONObject("allowed");
                if (allowed.has(member)) return draft;
            }
        }
        return null;
    }

    public static Draft getDraftByChannel(TextChannel tc) {
        for (Draft draft : Draft.drafts) {
            if (draft.tc.getId().equals(tc.getId())) return draft;
        }
        return null;
    }

    public static List<Long> getTeamMembers(long userid) {
        JSONObject asl = getEmolgaJSON().getJSONObject("drafts").getJSONObject("ASLS9");
        int index = getIndex(userid);
        LinkedList<Long> l = new LinkedList<>();
        if (index == -1) return l;
        for (int i = 1; i <= 4; i++) {
            l.add(asl.getJSONObject("S" + i).getLongList("table").get(index));
        }
        return l;
    }

    public static List<Long> getTeamMembers(String team) {
        JSONObject asl = getEmolgaJSON().getJSONObject("drafts").getJSONObject("ASLS9");
        int index = asl.getStringList("teams").indexOf(team);
        LinkedList<Long> l = new LinkedList<>();
        if (index == -1) return l;
        for (int i = 1; i <= 4; i++) {
            l.add(asl.getJSONObject("S" + i).getLongList("table").get(index));
        }
        return l;
    }

    public static String getTeamName(long userid) {
        return getEmolgaJSON().getJSONObject("drafts").getJSONObject("ASLS9").getStringList("teams").get(getIndex(userid));
    }

    public static JSONObject getLevelJSON(long userid) {
        return getEmolgaJSON().getJSONObject("drafts").getJSONObject("ASLS9").getJSONObject("S" + getLevel(userid));
    }

    public static int getLevel(long userid) {
        JSONObject asl = getEmolgaJSON().getJSONObject("drafts").getJSONObject("ASLS9");
        for (int i = 1; i <= 4; i++) {
            JSONObject o = asl.getJSONObject("S" + i);
            if (o.getLongList("table").contains(userid)) {
                return i;
            }
        }
        return -1;
    }

    public static int getIndex(long userid) {
        JSONObject levelJSON = getLevelJSON(userid);
        if (levelJSON == null) return -1;
        return levelJSON.getLongList("table").indexOf(userid);
    }

    public static JSONObject getLeagueStatic(String name) {
        JSONObject drafts = getEmolgaJSON().getJSONObject("drafts");
        Pattern aslpattern = Pattern.compile("^S\\d");
        return aslpattern.matcher(name).find() ? drafts.getJSONObject("ASLS9").getJSONObject(name) : drafts.getJSONObject(name);
    }

    public Tierlist getTierlist() {
        return Tierlist.getByGuild(guild);
    }

    public String getMention(long mem) {
        JSONObject league = getLeague();
        if (league.has("mentions")) {
            JSONObject mentions = league.getJSONObject("mentions");
            if (mentions.has(mem))
                return "<@" + mentions.getString(mem) + ">";
        }
        return "<@" + mem + ">";
    }

    public JSONObject getLeague() {
        JSONObject drafts = getEmolgaJSON().getJSONObject("drafts");
        Pattern aslpattern = Pattern.compile("^S\\d");
        return aslpattern.matcher(name).find() ? drafts.getJSONObject("ASLS9").getJSONObject(name) : drafts.getJSONObject(name);
    }

    public boolean isNotCurrent(long mem) {
        if (current == mem) return false;
        if (getLeague().has("table1") && getLeague().getJSONArray("table1").toLongListList().stream().anyMatch(l -> l.contains(current) && l.contains(mem)))
            return false;
        if (getLeague().has("table2") && getLeague().getJSONArray("table2").toLongListList().stream().anyMatch(l -> l.contains(current) && l.contains(mem)))
            return false;
        if (mem == Constants.FLOID) return false;
        if (mem == 690971979821613056L) return false;
        //if (getTeamMembers(mem.getIdLong()).contains(current.getIdLong())) return false;
        JSONObject league = getLeague();
        if (!league.has("allowed")) return true;
        return league.getJSONObject("allowed").optLong(String.valueOf(mem), -1) != current;
    }

    public boolean hasMega(long mem) {
        return picks.get(mem).stream().anyMatch(mon -> mon.name.startsWith("M-"));
    }

    public boolean hasInAnotherForm(long mem, String pokemon) {
        String[] split = pokemon.split("-");
        return picks.get(mem).stream().anyMatch(str -> split[0].equals(str.name.split("-")[0]) && !(split[0].equals("Porygon") && str.name.split("-")[0].equals("Porygon")));
    }

    public boolean isPicked(String pokemon) {
        return picks.values().stream().flatMap(Collection::stream).anyMatch(mon -> mon.name.equalsIgnoreCase(pokemon));
    }

    public boolean isPickedBy(String oldmon, long mem) {
        return picks.get(mem).stream().anyMatch(dp -> dp.name.equalsIgnoreCase(oldmon));
    }

    public void update(long mem) {
        if (ts == null) return;
        new Thread(() -> {
            ArrayList<Message> list = new ArrayList<>();
            for (Message message : ts.getIterableHistory()) {
                list.add(message);
            }
            Collections.reverse(list);
            if (list.isEmpty()) {
                for (long member : members) {
                    list.add(tc.sendMessage(getTeamMsg(mem)).complete());
                }
            }
            int i = 0;
            for (long member : members) {
                if (member == mem) {
                    list.get(i).editMessage(getTeamMsg(mem)).queue();
                    break;
                }
                i++;
            }
            //messages.get(member).editMessage(str.toString()).queue();
        }, "DraftMemberUpdate").start();
    }

    public void timer(TimerReason tr) {
        if (ended) return;
        if (order.get(round).size() == 0) {
            if (round == getTierlist().rounds) {
                saveEmolgaJSON();
                tc.sendMessage("Der Draft ist vorbei!").queue();
                Draft.drafts.remove(this);
                return;
            }
            round++;
            tc.sendMessage("Runde " + round + "!").queue();
        }
        JSONObject asl = getEmolgaJSON().getJSONObject("drafts").getJSONObject("ASLS9");
        JSONObject league = asl.getJSONObject(name);
        /*String msg = tr == TimerReason.REALTIMER ? "**" + current.getEffectiveName() + "** war zu langsam und deshalb ist jetzt " + getMention(order.get(round).get(0)) + " (<@&" + asl.getLongList("roleids").get(getIndex(order.get(round).get(0).getIdLong())) + ">) dran! "
                : "Der Pick von " + current.getEffectiveName() + " wurde " + (isSwitchDraft ? "geskippt" : "verschoben") + " und deshalb ist jetzt " + getMention(order.get(round).get(0)) + " dran!";*/
        String msg = tr == TimerReason.REALTIMER ? "**<@" + current + ">** war zu langsam und deshalb ist jetzt " + getMention(order.get(round).get(0)) + " dran! "
                : "Der Pick von <@" + current + "> wurde " + (isSwitchDraft ? "geskippt" : "verschoben") + " und deshalb ist jetzt " + getMention(order.get(round).get(0)) + " dran!";
        current = order.get(round).remove(0);
        league.put("current", current);
        if (isPointBased)
            msg += "(" + points.get(current) + " mögliche Punkte)";
        else
            msg += "(Mögliche Tiers: " + getPossibleTiersAsString(current) + ")";
        tc.sendMessage(msg).queue();
        try {
            cooldown.cancel();
        } catch (Exception ignored) {

        }
        cooldown = new Timer();
        JSONObject json = getEmolgaJSON();
        if (isPointBased && !isSwitchDraft && points.get(current) < 20) {
            List<DraftPokemon> picks = this.picks.get(current);
            DraftPokemon p = null;
            int price = 0;
            for (DraftPokemon pick : picks) {
                int pr = getTierlist().prices.get(pick.tier);
                if (pr > price) {
                    price = pr;
                    p = pick;
                }
            }
            tc.sendMessage("Du hast nicht mehr genug Punkte um ein weiteres Pokemon zu draften! Deshalb verlierst du " + p + " und erhältst dafür " + price / 2 + " Punkte!").queue();
            points.put(current, points.get(current) + price / 2);
            this.picks.get(current).remove(p);
            json.getJSONObject("drafts").getJSONObject("ASLS9").getJSONObject(name).getJSONObject("picks").put(current, getTeamAsArray(current));
        }
        long delay = calculateASLTimer();
        league.put("cooldown", System.currentTimeMillis() + delay);
        saveEmolgaJSON();
        cooldown.schedule(new TimerTask() {
            @Override
            public void run() {
                timer();
            }
        }, delay);
    }

    public void timer() {
        timer(TimerReason.REALTIMER);
    }

    public JSONArray getTeamAsArray(long mem) {
        JSONArray arr = new JSONArray();
        for (DraftPokemon mon : picks.get(mem)) {
            JSONObject obj = new JSONObject();
            obj.put("name", mon.name);
            obj.put("tier", mon.tier);
            arr.put(obj);
        }
        return arr;
    }

    public String getTeamMsg(long member) {
        List<DraftPokemon> list = picks.get(member);
        StringBuilder msg = new StringBuilder("**<@" + member + ">**\n");
        for (String o : getTierlist().order) {
            ArrayList<DraftPokemon> mons = list.stream().filter(s -> s.tier.equals(o)).sorted(Comparator.comparing(o2 -> o2.name)).collect(Collectors.toCollection(ArrayList::new));
            for (DraftPokemon mon : mons) {
                msg.append(o).append(": ").append(mon.name).append("\n");
            }
        }
        return msg.toString();
    }

    /*public static String getTeamMsgFromString(Guild g, String mem, String str) {
        Member member = g.retrieveMemberById(mem).complete();
        StringBuilder msg = new StringBuilder("**" + member.getEffectiveName() + "**\n");
        Tierlist tierlist = Tierlist.getByGuild(g.getId());
        for (String o : tierlist.order) {
            ArrayList<DraftPokemon> mons = Arrays.stream(str.split(",")).filter(s -> s.tier.equals(o)).sorted(Comparator.comparing(o2 -> o2.name)).collect(Collectors.toCollection(ArrayList::new));
            for (DraftPokemon mon : mons) {
                msg.append(o).append(": ").append(mon.name).append("\n");
            }
        }
        return msg.toString();
    }*/

    public Map<String, Integer> getPossibleTiers(long mem) {
        HashMap<String, Integer> possible = new HashMap<>(getTierlist().prices);
        for (DraftPokemon mon : picks.get(mem)) {
            possible.put(mon.tier, possible.get(mon.tier) - 1);
        }
        return possible;
    }

    public String getPossibleTiersAsString(long mem) {
        Map<String, Integer> possible = getPossibleTiers(mem);
        ArrayList<String> list = new ArrayList<>();
        for (String s : getTierlist().order) {
            if (possible.get(s) == 0) continue;
            list.add(possible.get(s) + "x **" + s + "**");
        }
        return String.join(", ", list);
    }


    public enum TimerReason {
        REALTIMER,
        SKIP
    }
}
