package de.tectoast.utils.Draft;


import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

import static de.tectoast.commands.Command.*;

public class Draft {
    public static final ArrayList<Draft> drafts = new ArrayList<>();
    public static HashMap<String, Integer> already = new HashMap<>();
    //public static Timer write;
    /*public static ArrayList<String> stier;
    public static ArrayList<String> atier;
    public static ArrayList<String> btier;
    public static ArrayList<String> ctier;
    public static ArrayList<String> dtier;*/
    //public HashMap<Member, Message> messages = new HashMap<>();
    public final HashMap<Member, ArrayList<DraftPokemon>> picks = new HashMap<>();
    public final HashMap<Integer, ArrayList<Member>> order = new HashMap<>();
    public final HashMap<Member, Integer> points = new HashMap<>();
    public final TextChannel tc;
    public final String name;
    public final String guild;
    public final boolean isPointBased;
    public ArrayList<Member> members;
    public Member current;
    public int round = 0;
    public Timer cooldown = new Timer();
    public TextChannel ts;

    public Draft(TextChannel tc, String name, String tcid, boolean fromFile) {
        this.tc = tc;
        this.guild = tc.getGuild().getId();
        this.name = name;
        isPointBased = getTierlist().isPointBased;
        JSONObject json = getEmolgaJSON();
        if (drafts.stream().map(draft -> draft.name).anyMatch(name::equals)) {
            tc.sendMessage("Dieser draft läuft bereits!").queue();
            return;
        }
        if (!json.has("drafts")) {
            tc.sendMessage("Es wurde noch keine Draftliga erstellt!").queue();
            return;
        }
        JSONObject league = json.getJSONObject("drafts").getJSONObject("ASLS7").getJSONObject(name);
        JSONObject o = league.getJSONObject("order");
        if (!league.has("points"))
            league.put("points", new JSONObject());
        JSONObject p = league.getJSONObject("points");
        HashMap<String, Member> map = new HashMap<>();
        new Thread(() -> {
            List<Member> memberlist = tc.getGuild().retrieveMembersByIds(o.getString("1").split(",")).get();
            memberlist.forEach(mem -> map.put(mem.getId(), mem));
            for (int i = 1; i <= 12; i++) {
                order.put(i, Arrays.stream(o.getString(Integer.toString(i)).split(",")).map(map::get).collect(Collectors.toCollection(ArrayList::new)));
            }
            this.members = new ArrayList<>(order.get(1));
            ts = tc.getGuild().getTextChannelById(tcid);
            if (!fromFile) {
                round++;
                for (Member member : members) {
                    picks.put(member, new ArrayList<>());
                    points.put(member, 1000);
                }
                for (Member member : members) {
                    ts.sendMessage("**" + member.getEffectiveName() + ":**").queue();
                }
                current = order.get(1).remove(0);
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        timer();
                    }
                }, calculateASLTimer());
                tc.sendMessage("Runde " + round + "!").queue();
                if (isPointBased)
                    tc.sendMessage(getMention(current) + " ist dran! (" + points.get(current) + " mögliche Punkte)").complete().getId();
                else
                    tc.sendMessage(getMention(current) + " ist dran! (Mögliche Tiers: " + getPossibleTiersAsString(current) + ")").complete().getId();
            } else {
                round = league.optInt("round", 1);
                current = tc.getGuild().retrieveMemberById(league.getString("current")).complete();
                int x = 0;
                for (Member member : order.get(round)) {
                    x++;
                    if (current.getId().equals(member.getId())) break;
                }
                if (x > 0) {
                    order.get(round).subList(0, x).clear();
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
                        picks.put(member, list);
                        update(member);
                    } else {
                        picks.put(member, new ArrayList<>());
                    }
                    if (isPointBased) {
                        points.put(member, 1000);
                        for (DraftPokemon mon : picks.get(member)) {
                            points.put(member, points.get(member) - getTierlist().prices.get(mon.tier));
                        }
                        p.put(member.getId(), points.get(member));
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
            drafts.add(this);
        }).start();
    }

    public static void init() {
        Tierlist.setup();
        /*JSONObject json = getEmolgaJSON();
        for (String s : json.getJSONObject("drafts").keySet()) {
            JSONObject league = json.getJSONObject("drafts").getJSONObject(s);
            if (league.has("announcements")) {
                JSONObject announcements = league.getJSONObject("announcements");
                for (int i = 1; i <= announcements.keySet().size(); i++) {
                    if (league.has("alreadyannounced")) {
                        if (Arrays.asList(league.getString("alreadyannounced").split(",")).contains(String.valueOf(i)))
                            continue;
                    }
                    int finalI = i;
                    long delay = announcements.getLong(String.valueOf(i)) - System.currentTimeMillis();
                    if (delay < 0) delay = 0;
                    //System.out.println("delay = " + delay);
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            JSONObject json = getEmolgaJSON();
                            JSONObject league = json.getJSONObject("drafts").getJSONObject(s);
                            already.put(s, finalI);
                            if (write == null) {
                                write = new Timer();
                                write.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        JSONObject json = getEmolgaJSON();
                                        for (String s : already.keySet()) {
                                            JSONObject league = json.getJSONObject("drafts").getJSONObject(s);
                                            if (!league.has("alreadyannounced")) league.put("alreadyannounced", "");
                                            league.put("alreadyannounced", league.getString("alreadyannounced") + already.get(s) + ",");
                                        }
                                        saveEmolgaJSON();
                                    }
                                }, 60000);
                            }
                            Guild g = EmolgaMain.jda.getGuildById(league.getString("guild"));
                            TextChannel tc = g.getTextChannelById(league.getString("announcementchannel"));
                            StringBuilder s = new StringBuilder("**Spieltag " + finalI + ":**\n");
                            for (String order : league.getJSONObject("battleorder").getString(String.valueOf(finalI)).split(";")) {
                                s.append(g.retrieveMemberById(order.split(":")[0]).complete().getEffectiveName()).append(" vs ").append(g.retrieveMemberById(order.split(":")[1]).complete().getEffectiveName()).append("\n");
                            }
                            s.append(g.getRoleById(league.getString("role")).getAsMention());
                            tc.sendMessage(s.toString()).queue();
                            saveEmolgaJSON();
                        }
                    }, delay);
                    //System.out.println(i + " " + (announcements.getLong(String.valueOf(i)) - System.currentTimeMillis()));
                }
            }
        }*/
    }

    /*public static boolean isDraftIn(TextChannel tc) {
        List<String> list = drafts.stream().map(draft -> draft.tc.getId()).collect(Collectors.toList());
        return list.contains(tc.getId());
    }*/

    public static Draft getDraftByMember(Member member, TextChannel tco) {
        JSONObject json = getEmolgaJSON();
        JSONObject drafts = json.getJSONObject("drafts").getJSONObject("ASLS7");
        System.out.println("member.getId() = " + member.getId());
        for (Draft draft : Draft.drafts) {
            System.out.println(draft.members.stream().map(mem -> mem.getId() + ":" + mem.getEffectiveName()).collect(Collectors.joining("\n")));
            if (!draft.tc.getId().equals(tco.getId())) continue;
            ArrayList<String> mates = getTeamMates(member.getId());
            if (draft.members.stream().anyMatch(mem -> mem.getId().equals(member.getId()) || mates.contains(mem.getId())))
                return draft;
            JSONObject league = drafts.getJSONObject(draft.name);
            if (league.has("allowed")) {
                JSONObject allowed = league.getJSONObject("allowed");
                if (allowed.has(member.getId())) return draft;
            }
        }

        return null;
    }

    public Tierlist getTierlist() {
        return Tierlist.getByGuild(guild);
    }

    public String getMention(Member mem) {
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject("ASLS7").getJSONObject(name);
        if (league.has("mentions")) {
            JSONObject mentions = league.getJSONObject("mentions");
            if (mentions.has(mem.getId()))
                return tc.getGuild().retrieveMemberById(mentions.getString(mem.getId())).complete().getAsMention();
        }
        return mem.getAsMention();
    }

    public boolean isCurrent(Member mem) {
        if (current.getId().equals(mem.getId())) return true;
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject("ASLS7").getJSONObject(name);
        return getTeamMates(mem.getId()).contains(current.getId());
    }


    public boolean hasMega(Member mem) {
        return picks.get(mem).stream().anyMatch(mon -> mon.name.startsWith("M-"));
    }

    @SuppressWarnings("SameReturnValue")
    public boolean hasInAnotherForm(Member mem, String pokemon) {
        /*String[] split = pokemon.split("-");
        if (split.length == 1) return false;*/
        return false;
    }

    public boolean isPicked(String pokemon) {
        return picks.values().stream().flatMap(Collection::stream).anyMatch(mon -> mon.name.equalsIgnoreCase(pokemon));
    }

    public void update(Member mem) {
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
    }

    public void timer() {
        if (order.get(round).size() == 0) {
            if (round == 12) {
                saveEmolgaJSON();
                tc.sendMessage("Der draft ist vorbei!").queue();
                Draft.drafts.remove(this);
                return;
            }
            round++;
            tc.sendMessage("Runde " + round + "!").queue();
        }
        String msg = getMention(current) + " war zu langsam und deshalb ist jetzt " + getMention(order.get(round).get(0)) + " dran! ";
        current = order.get(round).remove(0);
        if (isPointBased)
            msg += "(" + points.get(current) + " mögliche Punkte)";
        else
            msg += "(Mögliche Tiers: " + getPossibleTiersAsString(current) + ")";
        tc.sendMessage(msg).queue();
        cooldown = new Timer();
        JSONObject json = getEmolgaJSON();
        if (isPointBased && points.get(current) < 20) {
            ArrayList<DraftPokemon> picks = this.picks.get(current);
            DraftPokemon p = null;
            int price = 0;
            for (DraftPokemon pick : picks) {
                int pr = getTierlist().prices.get(pick.tier);
                if (pr > price) {
                    price = pr;
                    p = pick;
                }
            }
            tc.sendMessage("Du hast nicht mehr genug Punkte um ein weiteres commands zu draften! Deshalb verlierst du " + p + " und erhältst dafür " + price / 2 + " Punkte!").queue();
            points.put(current, points.get(current) + price / 2);
            this.picks.get(current).remove(p);
            json.getJSONObject("drafts").getJSONObject("ASLS7").getJSONObject(name).getJSONObject("picks").put(current.getId(), getTeamAsArray(current));
        }
        long delay = calculateASLTimer();
        cooldown.schedule(new TimerTask() {
            @Override
            public void run() {
                timer();
            }
        }, delay);
    }

    public JSONArray getTeamAsArray(Member mem) {
        JSONArray arr = new JSONArray();
        for (DraftPokemon mon : picks.get(mem)) {
            JSONObject obj = new JSONObject();
            obj.put("name", mon.name);
            obj.put("tier", mon.tier);
            arr.put(obj);
        }
        return arr;
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

    public String getTeamMsg(Member member) {
        ArrayList<DraftPokemon> list = picks.get(member);
        StringBuilder msg = new StringBuilder("**" + member.getEffectiveName() + "**\n");
        for (String o : getTierlist().order) {
            ArrayList<DraftPokemon> mons = list.stream().filter(s -> s.tier.equals(o)).sorted(Comparator.comparing(o2 -> o2.name)).collect(Collectors.toCollection(ArrayList::new));
            for (DraftPokemon mon : mons) {
                msg.append(o).append(": ").append(mon.name).append("\n");
            }
        }
        return msg.toString();
    }

    public HashMap<String, Integer> getPossibleTiers(Member mem) {
        HashMap<String, Integer> possible = new HashMap<>(getTierlist().prices);
        for (DraftPokemon mon : picks.get(mem)) {
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
}
