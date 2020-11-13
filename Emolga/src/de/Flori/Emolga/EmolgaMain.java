package de.Flori.Emolga;

import de.Flori.Commands.Command;
import de.Flori.Subscriber.NotificationCallback;
import de.Flori.Subscriber.Subscriber;
import de.Flori.Subscriber.impl.SubscriberImpl;
import de.Flori.utils.Giveaway;
import de.Flori.utils.MessageWaiter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.net.URI;
import java.time.Instant;
import java.util.*;

import static de.Flori.Commands.Command.*;

public class EmolgaMain {

    public static final MessageWaiter messageWaiter = new MessageWaiter();
    public static final ArrayList<Giveaway> todel = new ArrayList<>();
    public static JDA jda;
    public static final NotificationCallback parseYT = XML -> {

        System.out.println("XML = " + XML);
        if (XML.contains("at:deleted-entry")) {
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
        //System.out.println(link);
    };
    private static int i = 0;

    public static void start() throws LoginException, InterruptedException {
        jda = JDABuilder.createDefault(Command.tokens.getString("discord"))
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .addEventListeners(new EmolgaListener(), messageWaiter)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .build();
        jda.awaitReady();
        ArrayList<String> youtube = new ArrayList<>(Arrays.asList("UCYoTO-akZCsiusTe4rBxfhA", "UCMqmTa_6_wE7r9jQ6b8yqjQ", "UCUkb-7kNR03r4ldj_fm_BhA"));
        Subscriber subscriber = new SubscriberImpl(Command.tokens.getJSONObject("subscriber").getString("host"), Command.tokens.getJSONObject("subscriber").getInt("port"));
        for (String s : youtube) {
            subscriber.subscribe(URI.create("https://www.youtube.com/xml/feeds/videos.xml?channel_id=" + s)).setNotificationCallback(parseYT);
        }

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
