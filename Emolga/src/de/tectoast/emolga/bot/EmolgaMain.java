package de.tectoast.emolga.bot;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.database.Database;
import de.tectoast.emolga.utils.Giveaway;
import de.tectoast.emolga.utils.MessageWaiter;
import de.tectoast.emolga.utils.ModManager;
import de.tectoast.emolga.ytsubscriber.NotificationCallback;
import de.tectoast.toastilities.managers.ReactionManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.nio.file.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

import static de.tectoast.emolga.commands.Command.*;

public class EmolgaMain {

    public static final MessageWaiter messageWaiter = new MessageWaiter();
    public static final ArrayList<Giveaway> todel = new ArrayList<>();
    public static JDA jda;
    public static final ArrayList<String> alreadywritten = new ArrayList<>();
    public static final HashMap<String, Consumer<String>> sdmessages = new HashMap<>();
    public static final NotificationCallback parseYT = feed -> {

        System.out.println(feed.toString(4));
        sendToMe("KONSOLE! YT und so lol");

        /*if (XML.contains("at:deleted-entry")) {
            jda.getTextChannelById("447357526997073932").sendMessage("Deleted!").queue();
            return;
        }
        String link = XML.substring(XML.indexOf("href=", XML.indexOf("rel=\"alternate\"")) + 6, XML.indexOf("\"", XML.indexOf("href=", XML.indexOf("rel=\"alternate\"")) + 7));
        String id = link.substring(32);
        JSONObject json = Command.ytjson;
        if (!json.has("ytsend")) json.put("ytsend", new JSONArray());
        JSONArray arr = json.getJSONArray("ytsend");
        for (Object o : arr) {
            if (id.equals(o)) return;
        }
        String date = XML.substring(XML.indexOf("published") + 10, XML.indexOf("published") + 29);
        System.out.println("date = " + date);
        Calendar curr = Calendar.getInstance();
        Calendar c = Calendar.getInstance();
        String[] split = date.split("-");

        c.set(Calendar.YEAR, Integer.parseInt(split[0]));
        c.set(Calendar.MONTH, Integer.parseInt(split[1]) - 1);
        c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(split[2].substring(0, 2)));
        String[] t = date.substring(11).split(":");
        c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(t[0]));
        c.add(Calendar.HOUR_OF_DAY, 2);
        c.set(Calendar.MINUTE, Integer.parseInt(t[1]));
        c.set(Calendar.SECOND, Integer.parseInt(t[2]));
        long dif = curr.getTimeInMillis() - c.getTimeInMillis();
        System.out.println("dif = " + dif);
        if (dif > 100000000) {
            System.out.println("Skipped because of Time!");
            return;
        }
        arr.put(id);
        Command.save(json, "./yt.json");
        jda.getTextChannelById("752119281918935071").sendMessage(link).queue();
        //System.out.println(link);*/

    };
    private static int i = 0;

    public static void start() throws LoginException, InterruptedException, SQLException {
        //ArrayList<String> youtube = new ArrayList<>(Arrays.asList("UCYoTO-akZCsiusTe4rBxfhA", "UCMqmTa_6_wE7r9jQ6b8yqjQ", "UCUkb-7kNR03r4ldj_fm_BhA"));
        /*ArrayList<String> youtube = new ArrayList<>(Arrays.asList("UCwbxxz-T7dBccRIo_TZIeoQ", "UCNsChOlGuhsifD-WkaGV7yA"));
        Subscriber subscriber = new SubscriberImpl(Command.tokens.getJSONObject("subscriber").getString("host"), Command.tokens.getJSONObject("subscriber").getInt("port"));
        new Thread(() -> {
            for (String s : youtube) {
                subscriber.subscribe(URI.create("https://www.youtube.com/xml/feeds/videos.xml?channel_id=" + s)).setNotificationCallback(parseYT);
            }
        }).start();*/
        jda = JDABuilder.createDefault(Command.tokens.getString("discord"))
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
                .addEventListeners(new EmolgaListener(), messageWaiter)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .build();
        ResultSet replana = Database.select("select * from analysis");
        while (replana.next()) {
            replayAnalysis.put(replana.getLong("replay"), replana.getLong("result"));
        }
        ResultSet spoiler = Database.select("select * from spoilertags");
        while (spoiler.next()) {
            spoilerTags.add(spoiler.getLong("guildid"));
        }
        jda.awaitReady();
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updatePresence();
            }
        }, 0, 60000);
        ReactionManager manager = new ReactionManager(jda);
        manager.registerReaction("715249205186265178", "813025531779743774", "813025179114405898", "719928482544484352")
        .registerReaction("715249205186265178", "813025531779743774", "813025403098628097", "813005659619590184")
        .registerReaction("715249205186265178", "813025531779743774", "813025709232488480", "813027599743713320")
                .registerReaction("540899923789611018", "820784528888561715", "820781668586618901", "820783085976420372")
                .registerReaction("830146866812420116", "830391184459300915", "540969934457667613", "830392346348355594");
        new ModManager("default", "./ShowdownData/");
        new ModManager("nml", "../Showdown/sspserver/data/");
        sdmessages.put("joinServer", str -> jda.getTextChannelById("791284726677766155").sendMessage(str + " hat den Server betreten!").queue());
        sdmessages.put("leaveServer", str -> jda.getTextChannelById("791284726677766155").sendMessage(str + " hat den Server verlassen!").queue());
        sdmessages.put("manualMessage", str -> {
            String user = str.split("\\|")[0];
            jda.getTextChannelById("447357526997073932").sendMessage(user + ": " + str.substring(user.length() + 1)).queue();
        });

        new Thread(() -> {
            Path path = Paths.get("/home/florian/Showdown/sspserver/discord");
            try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
                final WatchKey watchKey = path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                while (true) {
                    final WatchKey wk = watchService.take();
                    for (WatchEvent<?> event : wk.pollEvents()) {
                        //we only register "ENTRY_MODIFY" so the context is always a Path.
                        final Path changed = (Path) event.context();
                        if (changed.endsWith("send.txt")) {
                            String message = String.join("\n", Files.readAllLines(path.resolve("send.txt")));
                            if(message.length() < 1) continue;
                            if(alreadywritten.remove(message)) continue;
                            alreadywritten.add(message);
                            System.out.println("Message from SD: " + message);
                            String type = message.split("\\|")[0];
                            if(!sdmessages.containsKey(type)) {
                                sendToMe(type + " wurde noch nicht registriert!"); // Should never happen
                            } else {
                                sdmessages.get(type).accept(message.substring(type.length() + 1));
                            }
                        }
                    }
                    // reset the key
                    boolean valid = wk.reset();
                    if (!valid) {
                        System.out.println("Key has been unregisterede");
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        /*new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Giveaway.giveaways.forEach(giveaway -> {
                    if (giveaway.messageId != null) {
                        try {
                            jda.getTextChannelById(giveaway.channelId).editMessageById(giveaway.messageId, giveaway.render(Instant.now())).queue();
                        } catch (Exception ex) {

                        }
                    }
                });
                boolean modified = false;
                for (Giveaway giveaway : todel5) {
                    modified = true;
                    JSONObject json = getEmolgaJSON();
                    if (json.has("giveaways")) {
                        JSONArray arr = json.getJSONArray("giveaways");
                        int index = 0;
                        int x = -1;
                        for (Object o : arr) {
                            JSONObject obj = (JSONObject) o;
                            if (obj.getString("mid").equals(giveaway.messageId)) x = index;
                            index++;
                        }
                        if (x != -1) {
                            arr.remove(x);
                            saveEmolgaJSON();
                        }
                    }
                }

                if (modified)
                    saveEmolgaJSON();
            }
        }, 0, 5000);*/
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                i++;
                //System.out.println(Giveaway.toadd);
                Giveaway.giveaways.addAll(Giveaway.toadd);
                Giveaway.toadd.clear();
                Giveaway.giveaways.forEach(giveaway -> {
                    //System.out.println("giveaway.messageId = " + giveaway.messageId);
                    if (giveaway.messageId == null) return;
                    if (giveaway.isEnded) return;
                    try {
                        if (giveaway.end.toEpochMilli() - System.currentTimeMillis() <= 10000 || i >= 5) {
                            jda.getTextChannelById(giveaway.channelId).editMessageById(giveaway.messageId, giveaway.render(Instant.now())).complete();
                        }
                    } catch (ErrorResponseException ex) {
                        ex.printStackTrace();
                        if (ex.getErrorCode() == 10008) {
                            sendToMe("GIVEAWAY DELETED!");
                            todel.add(giveaway);
                        }
                    }
                });
                if (i >= 5) i = 0;
                Giveaway.giveaways.removeAll(todel);
                boolean modified = false;
                for (Giveaway giveaway : todel) {
                    modified = true;
                    JSONObject json = getEmolgaJSON();
                    if (json.has("giveaways")) {
                        JSONArray arr = json.getJSONArray("giveaways");
                        int index = 0;
                        int x = -1;
                        for (Object o : arr) {
                            JSONObject obj = (JSONObject) o;
                            if (obj.getString("mid").equals(giveaway.messageId)) x = index;
                            index++;
                        }
                        if (x != -1) {
                            arr.remove(x);
                        }
                    }
                }
                todel.clear();
                if (modified) saveEmolgaJSON();
            }
        }, 0, 1000);
        /*Subscription test = subscriber.subscribe(URI.create("https://www.youtube.com/xml/feeds/videos.xml?channel_id=UCypQULrVZkp1-_RUnrpJ2nQ"));
        test.setNotificationCallback(XML -> {
            System.out.println("XML = " + XML);
        });*/
        //jda.getPresence().setActivity(Activity.playing("Wartungsarbeiten"));
    }
}
