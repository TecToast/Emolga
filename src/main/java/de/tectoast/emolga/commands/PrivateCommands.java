package de.tectoast.emolga.commands;

import de.tectoast.emolga.bot.EmolgaMain;
import de.tectoast.emolga.database.Database;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.Google;
import de.tectoast.emolga.utils.PrivateCommand;
import de.tectoast.emolga.utils.RequestBuilder;
import de.tectoast.emolga.utils.draft.Draft;
import de.tectoast.emolga.utils.draft.Tierlist;
import de.tectoast.emolga.utils.sql.DBManagers;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.internal.utils.tuple.ImmutablePair;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jsolf.JSONArray;
import org.jsolf.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static de.tectoast.emolga.commands.Command.*;
import static de.tectoast.emolga.utils.sql.DBManagers.ANALYSIS;
import static de.tectoast.emolga.utils.sql.DBManagers.TRANSLATIONS;

public class PrivateCommands {

    private static final Logger logger = LoggerFactory.getLogger(PrivateCommands.class);

    @PrivateCommand(name = "updatetierlist")
    public static void updateTierlist(GenericCommandEvent e) {
        Tierlist.setup();
        e.reply("Die Tierliste wurde aktualisiert!");
    }

    @PrivateCommand(name = "skipprivate")
    public static void skipprivate(GenericCommandEvent e) {
        Draft.drafts.stream().filter(draft -> draft.name.equals(e.getMsg().substring(6))).collect(Collectors.toList()).get(0).timer();
    }

    @PrivateCommand(name = "checkguild")
    public static void checkGuild(GenericCommandEvent e) {
        e.reply(e.getJDA().getGuildById(e.getArg(0)).getName());
    }

    @PrivateCommand(name = "timer")
    public static void timer(GenericCommandEvent e) {
        String name = e.getMsg().substring(7);
        List<Draft> list = Draft.drafts.stream().filter(d -> d.name.equals(name)).collect(Collectors.toList());
        if (list.size() == 0) {
            e.reply("Dieser Draft existiert nicht!");
            return;
        }
        Draft d = list.get(0);
        d.cooldown = new Timer();
        long delay = calculateASLTimer();
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject(d.name);
        league.put("cooldown", System.currentTimeMillis() + delay);
        d.cooldown.schedule(new TimerTask() {
            @Override
            public void run() {
                d.timer();
            }
        }, delay);
        getEmolgaJSON().getJSONObject("drafts").getJSONObject(name).put("timer", true);
        saveEmolgaJSON();
        e.reply("Der Timer bei " + name + " wurde aktiviert!");
    }

    @PrivateCommand(name = "edit")
    public static void edit(GenericCommandEvent e) {
        e.getJDA().getTextChannelById(e.getArg(0)).editMessageById(e.getArg(1), e.getMsg().substring(43)).queue();
    }

    @PrivateCommand(name = "send")
    public static void send(GenericCommandEvent e) {
        Message message = e.getMessage();
        String[] split = message.getContentDisplay().split(" ");
        logger.info(message.getContentRaw());
        String s = message.getContentRaw().substring(24).replaceAll("\\\\", "");
        TextChannel tc = e.getJDA().getTextChannelById(e.getArg(0));
        Guild g = tc.getGuild();
        for (ListedEmote emote : g.retrieveEmotes().complete()) {
            s = s.replace("<<" + emote.getName() + ">>", emote.getAsMention());
        }
        tc.sendMessage(s).queue();
    }

    @PrivateCommand(name = "sendpn")
    public static void sendPN(GenericCommandEvent e) {
        Message message = e.getMessage();
        String[] split = message.getContentDisplay().split(" ");
        logger.info(message.getContentRaw());
        String s = message.getContentRaw().substring(26).replaceAll("\\\\", "");
        String userid = e.getArg(0);
        sendToUser(Long.parseLong(userid), s);
    }

    @PrivateCommand(name = "react")
    public static void react(GenericCommandEvent e) {
        String msg = e.getMsg();
        String s = msg.substring(45);
        TextChannel tc = e.getJDA().getTextChannelById(e.getArg(0));
        Message m = tc.retrieveMessageById(e.getArg(1)).complete();
        assert (m != null);
        if (s.contains("<")) {
            s = s.substring(1);
            logger.info("s = " + s);
            String finalS = s;
            tc.getGuild().retrieveEmotes().complete().stream().filter(emote -> emote.getName().equalsIgnoreCase(finalS)).forEach(emote -> m.addReaction(emote).queue());
        } else {
            m.addReaction(s).queue();
        }
    }

    @PrivateCommand(name = "ban")
    public static void ban(GenericCommandEvent e) {
        e.getJDA().getGuildById(e.getArg(0)).ban(e.getArg(1), 0).queue();
    }

    @PrivateCommand(name = "banwithreason")
    public static void banwithreason(GenericCommandEvent e) {
        e.getJDA().getGuildById(e.getArg(0)).ban(e.getArg(1), 0, e.getMsg().substring(53)).queue();
    }

    @PrivateCommand(name = "updatedatabase")
    public static void updateDatabase(GenericCommandEvent e) {
        loadJSONFiles();
        e.done();
    }

    @PrivateCommand(name = "emolgajson", aliases = {"ej"})
    public static void emolgajson(GenericCommandEvent e) {
        emolgajson = load("./emolgadata.json");
        e.done();
    }

    @PrivateCommand(name = "sortbst")
    public static void sortBSTCmd(GenericCommandEvent e) {
        sortBST();
        e.done();
    }

    @PrivateCommand(name = "del")
    public static void del(GenericCommandEvent e) {
        e.getJDA().getTextChannelById(e.getArg(0)).deleteMessageById(e.getArg(1)).queue();
    }

    @PrivateCommand(name = "sortzbs")
    public static void sortZBSCmd(GenericCommandEvent e) {
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject("ZBSL" + e.getArg(0));
        sortZBS(league.getString("sid"), "Liga " + e.getArg(0), league);
    }

    @PrivateCommand(name = "sortwooloo")
    public static void sortWoolooCmd(GenericCommandEvent e) {
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject("WoolooCupS3L" + e.getArg(0));
        sortWooloo(league.getString("sid"), league);
    }

    @PrivateCommand(name = "troll")
    public static void troll(GenericCommandEvent e) {
        Category category = e.getJDA().getCategoryById(e.getArg(0));
        Guild g = category.getGuild();
        Member user = g.retrieveMemberById(e.getArg(1)).complete();
        ArrayList<VoiceChannel> list = new ArrayList<>(category.getVoiceChannels());
        Collections.shuffle(list);
        VoiceChannel old = user.getVoiceState().getChannel();
        list.remove(old);
        for (VoiceChannel voiceChannel : list) {
            g.moveVoiceMember(user, voiceChannel).queue();
            try {
                Thread.sleep(500);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        }
        g.moveVoiceMember(user, old).queue();
    }

    @PrivateCommand(name = "updatetable")
    public static void updateTableCmd(GenericCommandEvent e) {
        updateTable(getEmolgaJSON().getJSONObject("BlitzTurnier"), e.getJDA().getTextChannelById(771403849029386270L));
    }

    @PrivateCommand(name = "nextround")
    public static void nextRoundCmd(GenericCommandEvent e) {
        new Thread(() -> {
            JSONObject b = getEmolgaJSON().getJSONObject("BlitzTurnier");
            JSONObject bo = b.getJSONObject("battleorder");
            ArrayList<String> already = bo.keySet().stream().map(bo::getString).flatMap(s -> Arrays.stream(s.split(";"))).collect(Collectors.toCollection(ArrayList::new));
            ArrayList<String> order = new ArrayList<>();
            ArrayList<String> names = getBlitzTable(true).stream().map(l -> (String) l.get(0)).collect(Collectors.toCollection(ArrayList::new));
            for (int i = 0; i < names.size(); i++) {
                String s = names.get(i);
                if (order.stream().anyMatch(str -> str.contains(s))) continue;
                for (int j = i + 1; j < names.size(); j++) {
                    String str = names.get(j);
                    if (already.contains(str + ":" + s) || already.contains(s + ":" + str) || order.stream().anyMatch(string -> string.contains(str)))
                        continue;
                    order.add(s + ":" + str);
                    break;
                }
            }
            HashMap<String, String> namesmap = new HashMap<>();
            EmolgaMain.emolgajda.getGuildById(Constants.BSID).retrieveMembersByIds(names.toArray(new String[0])).get().forEach(mem -> namesmap.put(mem.getId(), mem.getEffectiveName()));
            StringBuilder str = new StringBuilder();
            for (String s : order) {
                str.append(namesmap.get(s.split(":")[0])).append(" vs ").append(namesmap.get(s.split(":")[1])).append("\n");
            }
            e.reply(str.toString());
            b.put("gameday", b.getInt("gameday") + 1);
            bo.put(String.valueOf(b.getInt("gameday")), String.join(";", order));
            saveEmolgaJSON();
        }, "NextRound").start();
    }

    @PrivateCommand(name = "levels")
    public static void levels(GenericCommandEvent e) {
        new Thread(() -> {
            HashMap<String, String> map = new HashMap<>();
            JSONObject level = getLevelJSON();
            e.getJDA().getGuildById(Constants.BSID).retrieveMembersByIds(level.keySet().toArray(new String[0])).get().forEach(m -> map.put(m.getId(), m.getEffectiveName()));
            StringBuilder str = new StringBuilder();
            level.keySet().stream().sorted(Comparator.comparing(level::getInt).reversed()).forEach(s -> {
                if (map.get(s) == null) return;
                str.append(map.get(s)).append(": ").append(getLevelFromXP(level.getInt(s))).append("\n");
            });
            e.reply(str.toString());
        }, "Levels").start();
    }

    @PrivateCommand(name = "generateorder")
    public static void generateOrder(GenericCommandEvent e) {
        new Thread(() -> {
            ArrayList<Member> list = new ArrayList<>(e.getJDA().getGuildById(Constants.BSID).findMembers(mem -> mem.getRoles().contains(e.getJDA().getGuildById(Constants.BSID).getRoleById("774659853812760587"))).get());
            Collections.shuffle(list);
            StringBuilder str = new StringBuilder();
            boolean b = false;
            for (Member member : list) {
                str.append(member.getId()).append(b ? ";" : ":");
                b = !b;
            }
            e.reply(str.toString());
        }, "GenerateOrder").start();
    }

    @PrivateCommand(name = "deletedreplays")
    public static void deletedReplays(GenericCommandEvent e) {
        JSONObject a = getEmolgaJSON().getJSONObject("analyse");
        a.keySet().stream().filter(s -> e.getJDA().getTextChannelById(s) == null).collect(Collectors.toList()).forEach(a::remove);
        saveEmolgaJSON();
        updatePresence();
    }


    @PrivateCommand(name = "delfromjson")
    public static void delFromJSON(GenericCommandEvent e) throws SQLException {
        ResultSet set = Database.select("select * from analysis");
        JSONObject an = getEmolgaJSON().getJSONObject("analyse");
        while (set.next()) {
            an.remove(set.getString("replay"));
        }
        saveEmolgaJSON();
    }

    @PrivateCommand(name = "addreactions")
    public static void addReactions(GenericCommandEvent e) {
        Message message = e.getMessage();
        e.getJDA().getTextChannelById(e.getArg(0)).retrieveMessageById(e.getArg(1)).queue(m -> {
            m.getReactions().forEach(mr -> {
                MessageReaction.ReactionEmote emote = mr.getReactionEmote();
                if (emote.isEmoji()) m.addReaction(emote.getEmoji()).queue();
                else m.addReaction(emote.getEmote()).queue();
            });
            e.done();
        });
    }

    @PrivateCommand(name = "replaceplayer")
    public static void replacePlayer(GenericCommandEvent e) {
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject(e.getArg(0));
        JSONObject battleOrder = league.getJSONObject("battleorder");
        String oldid = e.getArg(1);
        String newid = e.getArg(2);
        for (int i = 1; i <= battleOrder.length(); i++) {
            battleOrder.put(String.valueOf(i), battleOrder.getString(String.valueOf(i)).replace(oldid, newid));
        }
        league.put("table", league.getString("table").replace(oldid, newid));
        JSONObject order = league.getJSONObject("order");
        for (int i = 1; i <= order.length(); i++) {
            order.put(i, order.getString(String.valueOf(i)).replace(oldid, newid));
        }
        JSONObject picks = league.getJSONObject("picks");
        picks.put(newid, picks.getJSONArray(oldid));
        picks.remove(oldid);
        if (league.has("results")) {
            JSONObject results = league.getJSONObject("results");
            ArrayList<String> keys = new ArrayList<>(results.keySet());
            for (String key : keys) {
                if (key.contains(oldid)) {
                    results.put(key.replace(oldid, newid), results.getString(key));
                    results.remove(key);
                }
            }
        }
        saveEmolgaJSON();
        e.done();
    }

    @PrivateCommand(name = "saveemolgajson")
    public static void saveEmolga(GenericCommandEvent e) {
        saveEmolgaJSON();
        e.done();
    }

    @PrivateCommand(name = "pdg")
    public static void pdg(GenericCommandEvent e) {
        evaluatePredictions(getEmolgaJSON().getJSONObject("drafts").getJSONObject(e.getArg(0)), Boolean.parseBoolean(e.getArg(1)), Integer.parseInt(e.getArg(2)),
                e.getArg(3), e.getArg(4));
        e.done();
    }

    @PrivateCommand(name = "incrpdg")
    public static void incrPdg(GenericCommandEvent e) {
        for (String arg : e.getArgs()) {
            Database.incrementPredictionCounter(Long.parseLong(arg));
        }
    }

    @PrivateCommand(name = "silentmove")
    public static void silentMove(GenericCommandEvent e) {
        VoiceChannel from = e.getJDA().getVoiceChannelById(e.getArg(0));
    }

    @PrivateCommand(name = "testvolume")
    public static void testVolume(GenericCommandEvent e) {
        logger.info("Start!");
        musicManagers.get(673833176036147210L).player.setVolume(Integer.parseInt(e.getArg(0)));
        logger.info("musicManagers.get(673833176036147210L).player.getVolume() = " + musicManagers.get(673833176036147210L).player.getVolume());
    }

    @PrivateCommand(name = "printcache")
    public static void printCache(GenericCommandEvent e) {
        translationsCacheGerman.forEach((str, t) -> {
            logger.info(str);
            t.print();
            logger.info("=====");
        });
        logger.info(String.valueOf(translationsCacheOrderGerman));
        logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>");
        translationsCacheEnglish.forEach((str, t) -> {
            logger.info(str);
            t.print();
            logger.info("=====");
        });
        logger.info(String.valueOf(translationsCacheOrderEnglish));
        e.done();
    }

    @PrivateCommand(name = "clearcache")
    public static void clearCache(GenericCommandEvent e) {
        translationsCacheGerman.clear();
        translationsCacheOrderGerman.clear();
        translationsCacheEnglish.clear();
        translationsCacheOrderEnglish.clear();
        e.done();
    }

    @PrivateCommand(name = "startdrafts")
    public static void startDrafts(GenericCommandEvent e) {
        JDA jda = e.getJDA();
        new Draft(jda.getTextChannelById(873980093813379092L), "S1", null, false);
        new Draft(jda.getTextChannelById(873980128890331187L), "S2", null, false);
        new Draft(jda.getTextChannelById(873980158527275028L), "S3", null, false);
        new Draft(jda.getTextChannelById(873980191897174036L), "S4", null, false);
    }

    @PrivateCommand(name = "godrafts")
    public static void goDrafts(GenericCommandEvent e) {
        JDA jda = e.getJDA();
        new Draft(jda.getTextChannelById(873980093813379092L), "S1", null, true);
        new Draft(jda.getTextChannelById(873980128890331187L), "S2", null, true);
        new Draft(jda.getTextChannelById(873980158527275028L), "S3", null, true);
        new Draft(jda.getTextChannelById(873980191897174036L), "S4", null, true);
    }

    @PrivateCommand(name = "transfer")
    public static void transfer(GenericCommandEvent e) {
        JSONObject wiki = getWikiJSON();
        JSONObject atk = wiki.getJSONObject("natures");
        ArrayList<String> values = new ArrayList<>();
        for (String s : atk.keySet()) {
            JSONObject o = atk.getJSONObject(s);
            if (o.has("plus"))
                values.add("(\"" + s + "\", \"" + o.getString("plus") + "\", \"" + o.getString("minus") + "\")");
            else
                values.add("(\"" + s + "\", null, null)");
        }
        Database.update("INSERT INTO natures(name, plus, minus) VALUES " + String.join(",", values));
    }

    @PrivateCommand(name = "getguild")
    public static void getGuildCmd(GenericCommandEvent e) {
        e.reply(e.getJDA().getTextChannelById(e.getArg(0)).getGuild().getName());
    }

    @PrivateCommand(name = "downloadpokedex")
    public static void downloadPokedex(GenericCommandEvent event) throws IOException {
        List<String> l = Files.readAllLines(Paths.get("entwicklung.txt"));
        //ArrayList<String> list = new ArrayList<>();
        //int x = 0;
        for (String mon : l) {
            Document d = Jsoup.connect("https://www.pokewiki.de/" + mon).execute().bufferUp().parse();
            Element el = d.select("table[class=\"round centered\"]").first().child(0);
            Elements children = el.children();
            children.remove(0);
            //logger.info("children.html() = " + children.html());
            //logger.info("children.get(0).html() = " + children.get(0).html());
            ArrayList<String> games = new ArrayList<>();
            ArrayList<String> entries = new ArrayList<>();
            for (Element e : children) {
                int startindex = e.children().size() == 3 ? 1 : 0;
                Element edition = e.child(startindex).child(0);
                for (Element child : edition.children()) {
                    String text = child.text();
                    String title = child.child(0).attr("title");
                    String edi = title.substring(title.indexOf(" ") + 1);
                    String entry = e.child(startindex + 1).text();
                    games.add("`" + edi + "`");
                    entries.add("'" + entry.replace("'", "\\'") + "'");
                    //logger.info(text);
                /*list.add(text);
                set.add(text);*/
                }

            }
            String query = "insertBuilder into pokedex(" + "`pokemonname`," + String.join(",", games) + ")" + " values ('" + toSDName(mon) + "'," + String.join(",", entries) + ")";
            logger.info(query);
            Database.update(query);
            /*x++;
            if(x % 10 == 0) logger.info(x);*/
            //Database.insertBuilder("pokedex",  , "`" + toSDName(mon) + "`", entries.toArray());
        }
    }

    @PrivateCommand(name = "inviteme")
    public static void inviteMe(GenericCommandEvent e) {
        for (Guild guild : e.getJDA().getGuilds()) {
            try {
                guild.retrieveMemberById(Constants.FLOID).complete();
            } catch (Exception exception) {
                sendToMe(guild.getTextChannels().get(0).createInvite().complete().getUrl());
            }
        }
    }

    @PrivateCommand(name = "insertnicknames")
    public static void insertNicknames(GenericCommandEvent e) {
        JSONObject json = getEmolgaJSON().getJSONObject("shortcuts");
        for (String s : json.keySet()) {
            Translation t = getGerName(s);
            DBManagers.TRANSLATIONS.addNick(s, t);
        }
    }

    @PrivateCommand(name = "downloadtrainers")
    public static void downloadTrainers(GenericCommandEvent event) throws IOException {
        Document d = Jsoup.connect("https://www.pokewiki.de/Arenaleiter").get();
        Element ele = d.select("table[class=\"innerround c\"]").first();
        List<String> td = ele.select("td").stream().map(Element::text).map(s -> {
            if (s.equals("Ben")) return "Ben und Svenja";
            if (s.equals("Svenja")) return "";
            if (s.equals("Blau")) return "Blau Eich";
            return s;
        }).filter(e -> !e.trim().equals("")).filter(e -> !e.trim().equals("kein best.")).filter(e -> !e.trim().equals("—")).collect(Collectors.toList());
        HashMap<String, String> map = new HashMap<>();
        td.forEach(s -> {
            Document doc;
            try {
                logger.info("s = " + s);
                doc = Jsoup.connect("https://www.pokewiki.de/" + s).get();
                map.put(s, doc.select("span[lang=\"en\"]").first().text());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        td.forEach(s -> {
            String engl = map.get(s);
            DBManagers.TRANSLATIONS.insert(toSDName(engl), toSDName(s), engl, s, "trainer", "default", false, null);
        });
    }

    @PrivateCommand(name = "removeduplicates")
    public static void removeDuplicates(GenericCommandEvent e) {
        DBManagers.TRANSLATIONS.removeDuplicates();
    }

    @PrivateCommand(name = "ndsnominate")
    public static void ndsNominate(GenericCommandEvent e) {
        Draft.setupNDSNominate();
    }

    @PrivateCommand(name = "ndsfix")
    public static void ndsfix(GenericCommandEvent e) {
        JSONObject nds = getEmolgaJSON().getJSONObject("drafts").getJSONObject("NDS");
        JSONObject picks = nds.getJSONObject("picks");
        RequestBuilder b = new RequestBuilder(nds.getString("sid"));
        for (String s : picks.keySet()) {
            b.addColumn(nds.getJSONObject("teamnames").getString(s) + "!A200", new LinkedList<>(getPicksAsList(picks.getJSONArray(s))));
        }
        b.execute();
    }

    @PrivateCommand(name = "ndsprediction")
    public static void ndsPrediction(GenericCommandEvent e) {
        Draft.doNDSPredictionGame();
    }

    @PrivateCommand(name = "matchups")
    public static void matchUps(GenericCommandEvent e) {
        JSONObject nds = getEmolgaJSON().getJSONObject("drafts").getJSONObject("NDS");
        RequestBuilder b = new RequestBuilder(nds.getString("sid"));
        String str = nds.getJSONObject("battleorder").getString(e.getArg(0));
        JSONObject teamnames = nds.getJSONObject("teamnames");
        for (String s : str.split(";")) {
            String[] split = s.split(":");
            String t1 = teamnames.getString(split[0]);
            String t2 = teamnames.getString(split[1]);
            b.addSingle(t1 + "!Q15", "=IMPORTRANGE(\"https://docs.google.com/spreadsheets/d/1vPYBY-IzVSPodd8W_ukVSLME0YGyWF0hT6p3kr-QvZU/edit\"; \"" + t2 + "!B15:O29\")");
            b.addSingle(t1 + "!Q13", "=CONCAT(\"VS. Spieler \"; '" + t2 + "'!O2)");
            b.addSingle(t2 + "!Q15", "=IMPORTRANGE(\"https://docs.google.com/spreadsheets/d/1vPYBY-IzVSPodd8W_ukVSLME0YGyWF0hT6p3kr-QvZU/edit\"; \"" + t1 + "!B15:O29\")");
            b.addSingle(t2 + "!Q13", "=CONCAT(\"VS. Spieler \"; '" + t1 + "'!O2)");
        }
        b.execute();
    }

    @PrivateCommand(name = "aslstuff")
    public static void aslStuff(GenericCommandEvent e) {
        List<String> tiercolumns = Tierlist.getByGuild(Constants.ASLID).tiercolumns;
        List<List<Object>> lists = Arrays.asList(new LinkedList<>(), new LinkedList<>(), new LinkedList<>(), new LinkedList<>(), new LinkedList<>(), new LinkedList<>(), new LinkedList<>());
        int x = 0;
        JSONObject data = getDataJSON();
        for (String s : tiercolumns) {
            if (s.equals("NEXT")) {
                x++;
                continue;
            }
            lists.get(x).add(data.getJSONObject(getSDName(s)).getString("name"));
        }
        RequestBuilder b = new RequestBuilder("1FHgqqlUi3snpLneR6LJ_5Mk1tkEnryFviRa-I0UZ7Ws");
        for (int i = 0; i < 7; i++) {
            b.addColumn("Tierliste!" + getAsXCoord(i * 2 + 2) + "5", lists.get(i));
        }
        b.execute();
    }

    @PrivateCommand(name = "asltest")
    public static void asltest(GenericCommandEvent e) {
        e.reply(Draft.getTeamMembers(Long.parseLong(e.getArg(0))).toString());
    }

    @PrivateCommand(name = "sortnds")
    public static void sortNDSCmd(GenericCommandEvent e) {
        sortNDS("1vPYBY-IzVSPodd8W_ukVSLME0YGyWF0hT6p3kr-QvZU", getEmolgaJSON().getJSONObject("drafts").getJSONObject("NDS"));
        e.done();
    }

    @PrivateCommand(name = "ndskilllist")
    public static void ndskilllist(GenericCommandEvent e) {
        List<List<Object>> send = new LinkedList<>();
        String sid = "1vPYBY-IzVSPodd8W_ukVSLME0YGyWF0hT6p3kr-QvZU";
        JSONObject nds = getEmolgaJSON().getJSONObject("drafts").getJSONObject("NDS");
        JSONObject picks = nds.getJSONObject("picks");
        JSONObject teamnames = nds.getJSONObject("teamnames");
        //BufferedWriter writer = new BufferedWriter(new FileWriter("ndskilllistorder.txt"));
        for (String s : picks.keySet()) {
            List<String> mons = getPicksAsList(picks.getJSONArray(s));
            List<List<Object>> lists = Google.get(sid, "%s!B200:K%d".formatted(teamnames.getString(s), mons.size() + 199), false, false);
            for (int j = 0; j < mons.size(); j++) {
                send.add(lists.get(j));
            }
        }
        RequestBuilder.updateAll(sid, "Killliste!S1001", send);
    }

    private static Pair<Integer, Integer> getTierlistLocation(String pokemon, Tierlist tierlist) {
        int x = 0;
        int y = 0;
        boolean found = false;
        for (String s : tierlist.tiercolumns) {
            if (s.equalsIgnoreCase(pokemon)) {
                found = true;
                break;
            }
            //logger.info(s + " " + y);
            if (s.equals("NEXT")) {
                x++;
                y = 0;
            } else y++;
        }
        if (found)
            //noinspection SuspiciousNameCombination
            return new ImmutablePair<>(x, y);
        return null;
    }

    @PrivateCommand(name = "ndsrr")
    public static void ndsrr(GenericCommandEvent e) {
        JSONObject lastNom = load("ndsdraft.json").getJSONObject("hinrunde").getJSONObject("nominations").getJSONObject("5");
        String sid = "1vPYBY-IzVSPodd8W_ukVSLME0YGyWF0hT6p3kr-QvZU";
        //String sid = "1Lbeko-7ZFuuVon_qmgavDsht5JoWQPVk2TLMN6cCROo";
        String current = "";
        JSONObject nds = emolgajson.getJSONObject("drafts").getJSONObject("NDS");
        JSONObject allpicks = nds.getJSONObject("picks");
        JSONObject toid = nds.getJSONObject("nametoid");
        JSONObject teamnames = nds.getJSONObject("teamnames");
        RequestBuilder b = new RequestBuilder(sid);
        Tierlist tierlist = Tierlist.getByGuild(Constants.NDSID);
        HashMap<String, List<Integer>> currkills = new HashMap<>();
        HashMap<String, List<Integer>> currdeaths = new HashMap<>();
        HashMap<String, AtomicInteger> killstoadd = new HashMap<>();
        HashMap<String, AtomicInteger> deathstoadd = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            List<List<Object>> l = Google.get(sid, "RR Draft!%s5:%s28".formatted(getAsXCoord(i * 4 + 2), getAsXCoord(i * 4 + 4)), false, false);
            int x = 0;
            for (List<Object> objects : l) {
                if (x % 2 == 0) current = toid.getString(((String) objects.get(0)).trim());
                else {
                    List<String> currorder = Arrays.stream(lastNom.getString(current).split("###")).flatMap(s -> Arrays.stream(s.split(";"))).map(s -> s.split(",")[0]).collect(Collectors.toList());
                    String teamname = teamnames.getString(current);
                    if (!currkills.containsKey(current))
                        currkills.put(current, Google.get(sid, "%s!L200:L214".formatted(teamname), false, false).stream().map(li -> Integer.parseInt((String) li.get(0))).collect(Collectors.toList()));
                    if (!currdeaths.containsKey(current))
                        currdeaths.put(current, Google.get(sid, "%s!X200:X214".formatted(teamname), false, false).stream().map(li -> Integer.parseInt((String) li.get(0))).collect(Collectors.toList()));
                    if (!killstoadd.containsKey(current))
                        killstoadd.put(current, new AtomicInteger());
                    if (!deathstoadd.containsKey(current))
                        deathstoadd.put(current, new AtomicInteger());
                    String raus = (String) objects.get(0);
                    String rein = (String) objects.get(2);
                    JSONArray arr = allpicks.getJSONArray(current);
                    if (!raus.trim().equals("/") && !rein.trim().equals("/")) {
                        List<String> picks = getPicksAsList(arr);
                        logger.info("picks = " + picks);
                        logger.info("raus = " + raus);
                        logger.info("rein = " + rein);
                        int index = picks.indexOf(raus);
                        killstoadd.get(current).addAndGet(currkills.get(current).get(index));
                        deathstoadd.get(current).addAndGet(currdeaths.get(current).get(index));
                        JSONObject o = arr.getJSONObject(index);
                        o.put("name", rein);
                        o.put("tier", tierlist.getTierOf(rein));
                        String sdName = getSDName(rein);
                        JSONObject data = getDataJSON().getJSONObject(sdName);
                        int currindex = currorder.indexOf(raus) + 15;
                        Pair<Integer, Integer> outloc = getTierlistLocation(raus, tierlist);
                        Pair<Integer, Integer> inloc = getTierlistLocation(rein, tierlist);
                        b
                                .addSingle(teamname + "!B" + currindex, getGen5Sprite(data))
                                .addSingle(teamname + "!D" + currindex, rein);

                        if (outloc != null) {
                            b.addSingle("Tierliste!" + getAsXCoord((outloc.getLeft() + 1) * 6) + (outloc.getRight() + 4), "-frei-");
                        }
                        if (inloc != null) {
                            b.addSingle("Tierliste!" + getAsXCoord((inloc.getLeft() + 1) * 6) + (inloc.getRight() + 4), "='" + teamname + "'!B2");
                        }
                        b.addRow(teamname + "!A" + (index + 200), Arrays.asList(rein, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
                        b.addRow(teamname + "!N" + (index + 200), Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
                        JSONArray types = data.getJSONArray("types");
                        List<Object> t = new LinkedList<>();
                        for (String s : arrayToList(types, String.class)) {
                            t.add(getTypeIcons().getString(s));
                        }
                        if (t.size() == 1) t.add("/");
                        b.addRow(teamname + "!F" + currindex, t);
                        b.addSingle(teamname + "!H" + currindex, data.getJSONObject("baseStats").getInt("spe"));
                        b.addSingle(teamname + "!I" + currindex, tierlist.getPointsNeeded(rein));
                        b.addSingle(teamname + "!J" + currindex, "2");
                        b.addRow(teamname + "!L" + currindex, Arrays.asList(canLearnNDS(sdName, "stealthrock"), canLearnNDS(sdName, "defog"), canLearnNDS(sdName, "rapidspin"), canLearnNDS(sdName, "voltswitch", "uturn", "flipturn", "batonpass", "teleport")));
                    }
                }
                x++;
            }
        }
        for (String s : killstoadd.keySet()) {
            String teamname = teamnames.getString(s);
            b.addSingle("%s!L215".formatted(teamname), "=SUMME(L199:L214)");
            b.addSingle("%s!L199".formatted(teamname), killstoadd.get(s).get());
            b.addSingle("%s!X215".formatted(teamname), "=SUMME(X199:X214)");
            b.addSingle("%s!X199".formatted(teamname), deathstoadd.get(s).get());
        }
        save(emolgajson, "ndstestemolga.json");
        b.execute();
        e.done();
    }

    @PrivateCommand(name = "ndsicons")
    public static void ndsicons(GenericCommandEvent e) {
        JSONObject nds = emolgajson.getJSONObject("drafts").getJSONObject("NDS");
        JSONObject allpicks = nds.getJSONObject("picks");
        JSONObject teamnames = nds.getJSONObject("teamnames");
        RequestBuilder b = new RequestBuilder("1vPYBY-IzVSPodd8W_ukVSLME0YGyWF0hT6p3kr-QvZU");
        for (String s : allpicks.keySet()) {
            b.addColumn("%s!M200".formatted(teamnames.getString(s)), getPicksAsList(allpicks.getJSONArray(s)).stream().map(Command::getGen5Sprite).collect(Collectors.toList()));
        }
        b.execute();
    }

    @PrivateCommand(name = "ndsprices")
    public static void ndsprices(GenericCommandEvent e) {
        JSONObject nds = emolgajson.getJSONObject("drafts").getJSONObject("NDS");
        JSONObject allpicks = nds.getJSONObject("picks");
        JSONObject teamnames = nds.getJSONObject("teamnames");
        RequestBuilder b = new RequestBuilder("1vPYBY-IzVSPodd8W_ukVSLME0YGyWF0hT6p3kr-QvZU");
        Tierlist t = Tierlist.getByGuild(Constants.NDSID);
        for (String s : allpicks.keySet()) {
            b.addColumn("%s!Y200".formatted(teamnames.getString(s)), getPicksAsList(allpicks.getJSONArray(s)).stream().map(t::getPointsNeeded).collect(Collectors.toList()));
        }
        b.execute();
    }

    @PrivateCommand(name = "ndsnewkl")
    public static void ndsnewkl(GenericCommandEvent e) throws IOException {
        RequestBuilder b = new RequestBuilder("1vPYBY-IzVSPodd8W_ukVSLME0YGyWF0hT6p3kr-QvZU");
        List<String> l = Files.readAllLines(Paths.get("ndskilllistorder.txt"));
        List<List<Object>> send = new LinkedList<>();
        for (int i = 166; i < l.size(); i++) {
            String mon = l.get(i);
            int num = i + 1001;
            send.add(Arrays.asList(getGen5Sprite(mon), mon.toUpperCase(), "=SUMME(S%d:AB%d)".formatted(num, num), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        }
        b.addAll("Killliste!P1167", send);
        b.execute();
    }

    @PrivateCommand(name = "asls9sort")
    public static void aslS9Sort(GenericCommandEvent e) {
        sortASLS9(e.getMsg().substring("!asls9sort ".length()));
    }

    @PrivateCommand(name = "dewit")
    public static void dewit(GenericCommandEvent e) {
        JDA jda = e.getJDA();
        new Draft(jda.getTextChannelById(899709858591428628L), "S1", null, true, true);
        //new Draft(jda.getTextChannelById(899709885875363861L), "S2", null, true, true);
        //new Draft(jda.getTextChannelById(899709908402974760L), "S3", null, true, true);
        //new Draft(jda.getTextChannelById(899709929563246622L), "S4", null, true, true);
    }

    @PrivateCommand(name = "analyseguild")
    public static void analyseGuild(GenericCommandEvent e) {
        ANALYSIS.readWrite(ANALYSIS.selectAll(), set -> {
            while (set.next()) {
                TextChannel replay = EmolgaMain.emolgajda.getTextChannelById(set.getLong("replay"));
                if (replay != null) {
                    set.updateLong("guild", replay.getGuild().getIdLong());
                    set.updateRow();
                }
            }
        });
    }

    @PrivateCommand(name = "fixfpl")
    public static void fixFPL(GenericCommandEvent e) {
        RequestBuilder b = new RequestBuilder("1Aj5AkWiKYshL9igJi6MOzJnt5Sz-gIr81wmOo2nujH8");
        JSONObject league = emolgajson.getJSONObject("drafts").getJSONObject("FPLS1L2");
        int x = 0;
        for (Long id : league.getLongList("table")) {
            List<JSONObject> picks = league.getJSONObject("picks").getJSONList(id);
            int num = 0;
            for (JSONObject o : picks) {
                List<Object> li = Arrays.asList(o.getString("tier"), "", o.getString("name"), "", getDataJSON().getJSONObject(getSDName(o.getString("name"))).getJSONObject("baseStats").getInt("spe"));
                b.addRow("Kader L2!%s%d".formatted(getAsXCoord((x / 4) * 22 + 2), (x % 4) * 20 + 8 + num), li);
                num++;
            }
            x++;
        }
        b.execute();
    }

    @PrivateCommand(name = "cap")
    public static void cap(GenericCommandEvent e) {
        JSONObject data = getDataJSON();
        data.keySet().stream().map(data::getJSONObject).filter(o -> o.getInt("num") < 0 && o.getInt("num") > -3000).forEach(o -> {
            String name = o.getString("name");
            String id = toSDName(name);
            TRANSLATIONS.cap(name, id);
        });
    }

    @PrivateCommand(name = "musicguilds")
    public static void musicGuilds(GenericCommandEvent e) {
        CommandCategory.musicGuilds.forEach(DBManagers.MUSIC_GUILDS::addGuild);
    }

    @PrivateCommand(name = "wooloos4fix")
    public static void woolooFix(GenericCommandEvent e) {
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject("WoolooCupS4");
        JSONObject picks = league.getJSONObject("picks");
        List<String> tiers = Arrays.asList("Lambda", "Theta", "Phi", "Rho");
        for (String s : new ArrayList<>(picks.keySet())) {
            HashMap<String, List<JSONObject>> map = new HashMap<>();
            for (JSONObject o : picks.getJSONList(s)) {
                map.computeIfAbsent(o.getString("tier"), k -> new LinkedList<>()).add(o);
            }
            JSONArray arr = new JSONArray();
            for (String tier : tiers) {
                map.get(tier).forEach(arr::put);
            }
            picks.put(s, arr);
        }
        saveEmolgaJSON();
    }

    public static void execute(Message message) {
        String msg = message.getContentRaw();
        for (Method method : PrivateCommands.class.getDeclaredMethods()) {
            PrivateCommand a = method.getAnnotation(PrivateCommand.class);
            if (a == null) continue;
            if (msg.toLowerCase().startsWith("!" + a.name().toLowerCase() + " ") || msg.equalsIgnoreCase("!" + a.name()) || Arrays.stream(a.aliases()).anyMatch(s -> msg.startsWith("!" + s + " ") || msg.equalsIgnoreCase("!" + s))) {
                try {
                    method.invoke(null, new PrivateCommandEvent(message));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
