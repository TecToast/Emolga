package de.tectoast.emolga.utils.draft;


import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static de.tectoast.emolga.commands.Command.*;

public class Draft {
    public static final ArrayList<Draft> drafts = new ArrayList<>();
    public static HashMap<String, Integer> already = new HashMap<>();
    public final HashMap<Member, ArrayList<DraftPokemon>> picks = new HashMap<>();
    public final HashMap<Integer, ArrayList<Member>> order = new HashMap<>();
    public final HashMap<Member, Integer> points = new HashMap<>();
    public final ArrayList<Member> afterDraft = new ArrayList<>();
    public final TextChannel tc;
    public final String name;
    public final String guild;
    public final boolean isPointBased;
    public ArrayList<Member> members;
    public Member current;
    public int round = 0;
    public Timer cooldown = new Timer();
    public TextChannel ts;
    public boolean ended = false;

    public Draft(TextChannel tc, String name, String tcid, boolean fromFile) {
        this.tc = tc;
        this.guild = tc.getGuild().getId();
        this.name = name;
        isPointBased = getTierlist().isPointBased;
        JSONObject json = getEmolgaJSON();
        if (drafts.stream().map(draft -> draft.name).anyMatch(name::equals)) {
            tc.sendMessage("Dieser Draft läuft bereits!").queue();
            return;
        }
        if (!json.has("drafts")) {
            tc.sendMessage("Es wurde noch keine Draftliga erstellt!").queue();
            return;
        }
        JSONObject league = json.getJSONObject("drafts").getJSONObject(name);
        JSONObject o = league.getJSONObject("order");
        if (!league.has("points"))
            league.put("points", new JSONObject());
        JSONObject p = league.getJSONObject("points");
        HashMap<String, Member> map = new HashMap<>();
        new Thread(() -> {
            List<Member> memberlist = tc.getGuild().retrieveMembersByIds(o.getString("1").split(",")).get();
            memberlist.forEach(mem -> map.put(mem.getId(), mem));
            for (String i : o.keySet()) {
                order.put(Integer.valueOf(i), Arrays.stream(o.getString(i).split(",")).map(map::get).collect(Collectors.toCollection(ArrayList::new)));
            }
            this.members = new ArrayList<>(order.get(1));
            ts = tc.getGuild().getTextChannelById(tcid);
            if (!fromFile) {
                round++;
                for (Member member : members) {
                    picks.put(member, new ArrayList<>());
                    points.put(member, 1100);
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
                        points.put(member, 1100);
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

    public static void init(JDA jda) {
        Tierlist.setup();
        JSONObject json = getEmolgaJSON();
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy hh:mm:ss", Locale.GERMANY);
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
                for (int i = 0; i < league.getJSONObject("battleorder").length() - lastDay; i++) {
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
                            String bo = league.getJSONObject("battleorder").getString(String.valueOf(gameday));
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
                                sendEarlyASLReplays(gameday);
                            });
                        }
                    }, delay);
                }
            }
        }
    }

    /*public static boolean isDraftIn(TextChannel tc) {
        return drafts.stream().anyMatch(d -> d.tc.getId().equals(tc.getId()));
    }*/

    public static Draft getDraftByMember(Member member, TextChannel tco) {
        JSONObject json = getEmolgaJSON();
        System.out.println("member.getId() = " + member.getId());
        for (Draft draft : Draft.drafts) {
            System.out.println(draft.members.stream().map(mem -> mem.getId() + ":" + mem.getEffectiveName()).collect(Collectors.joining("\n")));
            if (!draft.tc.getId().equals(tco.getId())) continue;
            if (draft.members.stream().anyMatch(mem -> mem.getId().equals(member.getId())))
                return draft;
            JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject(draft.name);
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

    public Tierlist getTierlist() {
        return Tierlist.getByGuild(guild);
    }

    public String getMention(Member mem) {
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject(name);
        if (league.has("mentions")) {
            JSONObject mentions = league.getJSONObject("mentions");
            if (mentions.has(mem.getId()))
                return "<@" + mentions.getString(mem.getId()) + ">";
        }
        return mem.getAsMention();
    }

    public boolean isNotCurrent(Member mem) {
        if (current.getId().equals(mem.getId())) return false;
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject(name);
        return !league.getJSONObject("allowed").optString(mem.getId(), "").equals(current.getId());
    }


    public boolean hasMega(Member mem) {
        return picks.get(mem).stream().anyMatch(mon -> mon.name.startsWith("M-"));
    }

    public boolean hasInAnotherForm(Member mem, String pokemon) {
        String[] split = pokemon.split("-");

        return picks.get(mem).stream().anyMatch(str -> split[0].equals(str.name.split("-")[0]) && !(split[0].equals("Porygon") && str.name.split("-")[0].equals("Porygon")));
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

    public void timer(TimerReason tr) {
        if (ended) return;
        if (order.get(round).size() == 0) {
            if (round == 13) {
                saveEmolgaJSON();
                tc.sendMessage("Der Draft ist vorbei!").queue();
                Draft.drafts.remove(this);
                return;
            }
            round++;
            tc.sendMessage("Runde " + round + "!").queue();
        }
        String msg = tr == TimerReason.REALTIMER ? getMention(current) + " war zu langsam und deshalb ist jetzt " + getMention(order.get(round).get(0)) + " dran! " : getMention(current) + " wurde geskippt und deshalb ist jetzt " + getMention(order.get(round).get(0)) + " dran! ";
        current = order.get(round).remove(0);
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject(name).put("current", current.getId());
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
            json.getJSONObject("drafts").getJSONObject(name).getJSONObject("picks").put(current.getId(), getTeamAsArray(current));
        }
        saveEmolgaJSON();
        long delay = calculateASLTimer();
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
        for (DraftPokemon mon : picks.get(mem)) {
            JSONObject obj = new JSONObject();
            obj.put("name", mon.name);
            obj.put("tier", mon.tier);
            arr.put(obj);
        }
        return arr;
    }

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

    public enum TimerReason {
        REALTIMER,
        SKIP
    }
}
