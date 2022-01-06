package de.tectoast.emolga.utils.draft;


import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.Google;
import de.tectoast.emolga.utils.RequestBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import org.jsolf.JSONArray;
import org.jsolf.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.tectoast.emolga.bot.EmolgaMain.emolgajda;
import static de.tectoast.emolga.commands.Command.*;

public class Draft {
    public static final List<Draft> drafts = new ArrayList<>();
    public final HashMap<Long, List<DraftPokemon>> picks = new HashMap<>();
    public final HashMap<Integer, List<Member>> order = new HashMap<>();
    public final HashMap<Long, Integer> points = new HashMap<>();
    public final List<Member> afterDraft = new ArrayList<>();
    public final TextChannel tc;
    public final String name;
    public final String guild;
    public final boolean isPointBased;
    public final boolean isSwitchDraft;
    public final LinkedList<Member> finished = new LinkedList<>();
    public ArrayList<Member> members;
    public Member current;
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
        JSONObject asl = drafts.getJSONObject("ASLS9");
        Pattern aslpattern = Pattern.compile("^S\\d");
        JSONObject league = aslpattern.matcher(name).find() ? asl.getJSONObject(name) : drafts.getJSONObject(name);
        this.guild = league.has("guild") ? league.getString("guild") : tc.getGuild().getId();
        isPointBased = getTierlist().isPointBased;
        System.out.println("isPointBased = " + isPointBased);
        JSONObject o = league.getJSONObject("order");
        HashMap<Long, Member> map = new HashMap<>();
        new Thread(() -> {
            List<Member> memberlist = tc.getJDA().getGuildById(this.guild).retrieveMembersByIds(o.getLongList("1")).get();
            memberlist.forEach(mem -> map.put(mem.getIdLong(), mem));
            System.out.println(name);
            System.out.println("map = " + map);
            for (String i : o.keySet()) {
                order.put(Integer.valueOf(i), o.getLongList(i).stream().map(map::get).collect(Collectors.toCollection(ArrayList::new)));
            }
            System.out.println("order = " + order);
            this.members = new ArrayList<>(memberlist);
            ts = tcid != null ? tc.getGuild().getTextChannelById(tcid) : null;
            if (!fromFile && !isSwitchDraft) {
                round++;
                for (Member member : members) {
                    picks.put(member.getIdLong(), new ArrayList<>());
                    points.put(member.getIdLong(), getTierlist().points);
                }
                if (ts != null) {
                    for (Member member : members) {
                        ts.sendMessage("**" + member.getEffectiveName() + ":**").queue();
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
                    tc.sendMessage(getMention(current) + " ist dran! (" + points.get(current.getIdLong()) + " mögliche Punkte)").queue();
                else
                    tc.sendMessage(getMention(current) + " ist dran! (Mögliche Tiers: " + getPossibleTiersAsString(current) + ")").complete().getId();
            } else {
                if (isSwitchDraft && !fromFile) {
                    round++;
                    current = order.get(1).remove(0);
                    JSONObject pick = league.getJSONObject("picks");
                    for (Member member : members) {
                        System.out.println("member = " + member);
                        if (pick.has(member.getId())) {
                            JSONArray arr = pick.getJSONArray(member.getId());
                            ArrayList<DraftPokemon> list = new ArrayList<>();
                            for (Object ob : arr) {
                                JSONObject obj = (JSONObject) ob;
                                list.add(new DraftPokemon(obj.getString("name"), obj.getString("tier")));
                            }
                            picks.put(member.getIdLong(), list);
                            update(member);
                            System.out.println("update end");
                        } else {
                            picks.put(member.getIdLong(), new ArrayList<>());
                        }
                        if (isPointBased) {
                            points.put(member.getIdLong(), getTierlist().points);
                            for (DraftPokemon mon : picks.get(member.getIdLong())) {
                                points.put(member.getIdLong(), points.get(member.getIdLong()) - getTierlist().prices.get(mon.tier));
                            }
                        }
                    }
                    System.out.println("For finished");
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
                    System.out.println("Before send");
                    tc.sendMessage("Runde " + round + "!").queue();
                    if (isPointBased)
                        tc.sendMessage(getMention(current) + " ist dran! (" + points.get(current.getIdLong()) + " mögliche Punkte)").queue();
                    else
                        tc.sendMessage(getMention(current) + " ist dran! (Mögliche Tiers: " + getPossibleTiersAsString(current) + ")").complete().getId();
                    saveEmolgaJSON();
                } else {
                    round = league.optInt("round", 1);
                    current = tc.getJDA().getGuildById(this.guild).retrieveMemberById(league.getString("current")).complete();
                    int x = 0;
                    for (Member member : order.get(round)) {
                        x++;
                        if (current.getId().equals(member.getId())) break;
                    }
                    if (x > 0) {
                        order.get(round).subList(0, x).clear();
                    }
                    if (league.has("finished")) {
                        Arrays.asList(league.getString("finished").split(",")).forEach(s -> order.values().forEach(l -> l.removeIf(me -> me.getId().equals(s))));
                    }
                    System.out.println("order.size() = " + order.get(round).size());
                    JSONObject pick = league.getJSONObject("picks");
                    for (Member member : members) {
                        if (pick.has(member.getId())) {
                            JSONArray arr = pick.getJSONArray(member.getId());
                            ArrayList<DraftPokemon> list = new ArrayList<>();
                            for (Object ob : arr) {
                                JSONObject obj = (JSONObject) ob;
                                list.add(new DraftPokemon(obj.getString("name"), obj.getString("tier")));
                            }
                            picks.put(member.getIdLong(), list);
                            update(member);
                        } else {
                            picks.put(member.getIdLong(), new ArrayList<>());
                        }
                        if (isPointBased) {
                            points.put(member.getIdLong(), getTierlist().points);
                            for (DraftPokemon mon : picks.get(member.getIdLong())) {
                                Tierlist t = getTierlist();
                                if (t == null) System.out.println("TIERLISTNULL");
                                if (t.prices.get(mon.tier) == null) {
                                    System.out.println(mon.name + " ERROR " + mon.tier);
                                }
                                points.put(member.getIdLong(), points.get(member.getIdLong()) - t.prices.get(mon.tier));
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
            System.out.println("Initialized Draft " + name + " !");
        }).start();
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
            //System.out.println("o.get(u) = " + o.get(u));
            String str = o.getString(u);
            List<String> mons = Arrays.stream(str.split("###")).flatMap(s -> Arrays.stream(s.split(";"))).map(s -> s.split(",")[0]).collect(Collectors.toList());
            System.out.println("mons = " + mons);
            String range = nds.getJSONObject("teamnames").getString(u) + "!B15:O29";
            System.out.println("u = " + u);
            System.out.println("range = " + range);
            builder.addAll(range, Google.get(sid, range, true, false).stream().filter(n -> !n.get(2).equals("")).sorted(Comparator.comparing(n -> {

                String s1 = (String) n.get(2);
                System.out.println("s1 = " + s1);
                int ret = mons.indexOf(s1);
                System.out.println("ret = " + ret);
                return ret;
            })).collect(Collectors.toList()));
        }
        builder.execute(true);
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
            System.out.println("u1 = " + u1);
            System.out.println("u2 = " + u2);
            JSONObject teamnames = league.getJSONObject("teamnames");
            String t1 = teamnames.getString(u1);
            String t2 = teamnames.getString(u2);
            System.out.println("t1 = " + t1);
            System.out.println("t2 = " + t2);
            Emote e1 = g.getEmotesByName(toSDName(t1), true).get(0);
            Emote e2 = g.getEmotesByName(toSDName(t2), true).get(0);
            //System.out.println("<@" + u1 + "> (" + e1.getAsMention() + ") vs. <@" + u2 + "> (" + e2.getAsMention() + ")");
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
            System.out.println("NDS" + " -> " + i + " -> " + delay + " -> " + new SimpleDateFormat().format(new Date(System.currentTimeMillis() + delay)));
            int finalI = i + 1;
            System.out.println("finalI = " + finalI);
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
                        System.out.println("<@" + u1 + "> (" + e1.getAsMention() + ") vs. <@" + u2 + "> (" + e2.getAsMention() + ")");

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
                    System.out.println(draftname + " -> " + i + " -> " + delay + " -> " + format.format(new Date(System.currentTimeMillis() + delay)));
                    int finalI = i + 1;
                    System.out.println("finalI = " + finalI);
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

    public static Draft getDraftByMember(Member member, TextChannel tco) {
        JSONObject json = getEmolgaJSON();
        //System.out.println("member.getId() = " + member.getId());
        for (Draft draft : Draft.drafts) {
            //System.out.println(draft.members.stream().map(mem -> mem.getId() + ":" + mem.getEffectiveName()).collect(Collectors.joining("\n")));
            if (!draft.tc.getId().equals(tco.getId())) continue;
            if (member.getIdLong() == Constants.FLOID) return draft;
            if (member.getIdLong() == 690971979821613056L) return draft;
            if (draft.members.stream().anyMatch(mem -> mem.getId().equals(member.getId())))
                return draft;
            /*if (getTeamMembers(member.getIdLong()).stream().anyMatch(l -> draft.members.stream().anyMatch(mem -> mem.getIdLong() == l)))
                return draft;*/
            JSONObject league = draft.getLeague();
            if (league.has("allowed")) {
                JSONObject allowed = league.getJSONObject("allowed");
                if (allowed.has(member.getId())) return draft;
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

    public Tierlist getTierlist() {
        return Tierlist.getByGuild(guild);
    }

    public String getMention(Member mem) {
        JSONObject league = getLeague();
        if (league.has("mentions")) {
            JSONObject mentions = league.getJSONObject("mentions");
            if (mentions.has(mem.getId()))
                return "<@" + mentions.getString(mem.getId()) + ">";
        }
        return mem.getAsMention();
    }

    public JSONObject getLeague() {
        JSONObject drafts = getEmolgaJSON().getJSONObject("drafts");
        JSONObject asl = drafts.getJSONObject("ASLS9");
        Pattern aslpattern = Pattern.compile("^S\\d");
        return aslpattern.matcher(name).find() ? asl.getJSONObject(name) : drafts.getJSONObject(name);
    }

    public static JSONObject getLeagueStatic(String name) {
        JSONObject drafts = getEmolgaJSON().getJSONObject("drafts");
        JSONObject asl = drafts.getJSONObject("ASLS9");
        Pattern aslpattern = Pattern.compile("^S\\d");
        return aslpattern.matcher(name).find() ? asl.getJSONObject(name) : drafts.getJSONObject(name);
    }

    public boolean isNotCurrent(Member mem) {
        if (current.getId().equals(mem.getId())) return false;
        if (mem.getIdLong() == Constants.FLOID) return false;
        //if (mem.getIdLong() == 694543579414134802L) return false;
        if (mem.getIdLong() == 690971979821613056L) return false;
        //if (getTeamMembers(mem.getIdLong()).contains(current.getIdLong())) return false;
        JSONObject league = getLeague();
        if (!league.has("allowed")) return true;
        return !league.getJSONObject("allowed").optString(mem.getId(), "").equals(current.getId());
    }

    public boolean hasMega(Member mem) {
        return picks.get(mem.getIdLong()).stream().anyMatch(mon -> mon.name.startsWith("M-"));
    }

    public boolean hasInAnotherForm(Member mem, String pokemon) {
        String[] split = pokemon.split("-");

        return picks.get(mem.getIdLong()).stream().anyMatch(str -> split[0].equals(str.name.split("-")[0]) && !(split[0].equals("Porygon") && str.name.split("-")[0].equals("Porygon")));
    }

    public boolean isPicked(String pokemon) {
        return picks.values().stream().flatMap(Collection::stream).anyMatch(mon -> mon.name.equalsIgnoreCase(pokemon));
    }

    public boolean isPickedBy(String oldmon, Member mem) {
        return picks.get(mem.getIdLong()).stream().anyMatch(dp -> dp.name.equalsIgnoreCase(oldmon));
    }

    public void update(Member mem) {
        if (ts == null) return;
        new Thread(() -> {
            ArrayList<Message> list = new ArrayList<>();
            for (Message message : ts.getIterableHistory()) {
                list.add(message);
            }
            Collections.reverse(list);
            if (list.isEmpty()) {
                for (Member member : members) {
                    list.add(tc.sendMessage(getTeamMsg(mem)).complete());
                }
            }
            int i = 0;
            for (Member member : members) {
                if (member.getId().equals(mem.getId())) {
                    list.get(i).editMessage(getTeamMsg(mem)).queue();
                    break;
                }
                i++;
            }
            //messages.get(member).editMessage(str.toString()).queue();
        }).start();
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
        String msg = tr == TimerReason.REALTIMER ? "**" + current.getEffectiveName() + "** war zu langsam und deshalb ist jetzt " + getMention(order.get(round).get(0)) + " dran! "
                : "Der Pick von " + current.getEffectiveName() + " wurde " + (isSwitchDraft ? "geskippt" : "verschoben") + " und deshalb ist jetzt " + getMention(order.get(round).get(0)) + " dran!";
        current = order.get(round).remove(0);
        league.put("current", current.getId());
        if (isPointBased)
            msg += "(" + points.get(current.getIdLong()) + " mögliche Punkte)";
        else
            msg += "(Mögliche Tiers: " + getPossibleTiersAsString(current) + ")";
        tc.sendMessage(msg).queue();
        try {
            cooldown.cancel();
        } catch (Exception ignored) {

        }
        cooldown = new Timer();
        JSONObject json = getEmolgaJSON();
        if (isPointBased && !isSwitchDraft && points.get(current.getIdLong()) < 20) {
            List<DraftPokemon> picks = this.picks.get(current.getIdLong());
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
            points.put(current.getIdLong(), points.get(current.getIdLong()) + price / 2);
            this.picks.get(current.getIdLong()).remove(p);
            json.getJSONObject("drafts").getJSONObject("ASLS9").getJSONObject(name).getJSONObject("picks").put(current.getId(), getTeamAsArray(current));
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

    public JSONArray getTeamAsArray(Member mem) {
        JSONArray arr = new JSONArray();
        for (DraftPokemon mon : picks.get(mem.getIdLong())) {
            JSONObject obj = new JSONObject();
            obj.put("name", mon.name);
            obj.put("tier", mon.tier);
            arr.put(obj);
        }
        return arr;
    }

    public String getTeamMsg(Member member) {
        List<DraftPokemon> list = picks.get(member.getIdLong());
        StringBuilder msg = new StringBuilder("**" + member.getEffectiveName() + "**\n");
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

    public HashMap<String, Integer> getPossibleTiers(Member mem) {
        HashMap<String, Integer> possible = new HashMap<>(getTierlist().prices);
        for (DraftPokemon mon : picks.get(mem.getIdLong())) {
            possible.put(mon.tier, possible.get(mon.tier) - 1);
        }
        return possible;
    }

    public String getPossibleTiersAsString(Member mem) {
        HashMap<String, Integer> possible = getPossibleTiers(mem);
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
