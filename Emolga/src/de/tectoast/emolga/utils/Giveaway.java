package de.tectoast.emolga.utils;

import de.tectoast.emolga.bot.EmolgaMain;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static de.tectoast.emolga.commands.Command.getEmolgaJSON;
import static de.tectoast.emolga.commands.Command.saveEmolgaJSON;
import static de.tectoast.emolga.commands.various.GcreateCommand.secondsToTime;

public class Giveaway {


    public static final Set<Giveaway> giveaways = new HashSet<>();
    public static final Set<Giveaway> toadd = new HashSet<>();
    public final Instant end;
    public final int winners;
    public final String prize;
    public final String channelId;
    public final String userId;
    public final Timer timer = new Timer();
    public String messageId = null;
    public boolean isEnded = false;
    public boolean isRole = false;

    public Giveaway(String channelId, String userId, Instant end, int winners, String prize) {
        this.channelId = channelId;
        this.userId = userId;
        this.end = end;
        this.winners = winners;
        this.prize = prize == null ? null : prize.isEmpty() ? null : prize;
        long delay = end.toEpochMilli() - System.currentTimeMillis();
        if (delay > 0) {
            this.timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    end();
                }
            }, delay);
            toadd.add(this);
        }
        System.out.println(end.toEpochMilli() - System.currentTimeMillis());
    }

    public Giveaway(String channelId, String userId, Instant end, int winners, String prize, String mid, boolean isRole) {
        this.messageId = mid;
        this.channelId = channelId;
        this.userId = userId;
        this.isRole = isRole;
        this.end = end;
        this.winners = winners;
        this.prize = prize == null ? null : prize.isEmpty() ? null : prize;
        long delay = end.toEpochMilli() - System.currentTimeMillis();
        System.out.println(end.toEpochMilli() - System.currentTimeMillis());
        if (delay > 0) {
            this.timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    end();
                }
            }, delay);
            toadd.add(this);
        } else end();
    }

    public Message render(Instant now) {
        MessageBuilder mb = new MessageBuilder();
        boolean close = now.plusSeconds(9).isAfter(end);
        mb.append("\uD83C\uDF89").append(close ? " **G I V E A W A Y** " : "   **GIVEAWAY**   ").append("\uD83C\uDF89");
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.CYAN);
        eb.setFooter((winners == 1 ? "" : winners + " Gewinner | ") + "Endet", null);
        eb.setTimestamp(end);
        eb.setDescription("Reagiere mit " + Constants.APFELKUCHENMENTION + " um dem Giveaway beizutreten!"
                + "\nVerbleibende Zeit: " + secondsToTime(now.until(end, ChronoUnit.SECONDS))
                + "\nGehostet von: <@" + (isRole ? "&" : "") + userId + ">");
        if (prize != null)
            eb.setAuthor(prize, null, null);
        if (close)
            eb.setTitle("Letzte Chance!!!", null);
        mb.setEmbed(eb.build());
        return mb.build();
    }

    private String messageLink() {
        return String.format("\n<https://discordapp.com/channels/%s/%s/%s>", EmolgaMain.jda.getTextChannelById(channelId).getGuild().getIdLong(), channelId, messageId);
    }

    public void end() {
        isEnded = true;
        JSONObject json = getEmolgaJSON();
        if (json.has("giveaways")) {
            JSONArray arr = json.getJSONArray("giveaways");
            int index = 0;
            int x = -1;
            for (Object o : arr) {
                JSONObject obj = (JSONObject) o;
                if (obj.getString("mid").equals(messageId)) x = index;
                index++;
            }
            if (x != -1) {
                arr.remove(x);
                saveEmolgaJSON();
            }
        }
        MessageBuilder mb = new MessageBuilder();
        mb.append("\uD83C\uDF89").append(" **GIVEAWAY ZU ENDE** ").append("\uD83C\uDF89");
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.CYAN); // dark theme background
        eb.setFooter((winners == 1 ? "" : winners + " Gewinner | ") + "Endete", null);
        eb.setTimestamp(end);
        if (prize != null)
            eb.setAuthor(prize, null, null);
        try {

            Optional<MessageReaction> opt;
            if (messageId.equals("775761399737352222")) {
                opt = EmolgaMain.jda.getTextChannelById(channelId).retrieveMessageById(messageId).complete().getReactions().stream().filter(mr -> mr.getReactionEmote().isEmoji() && mr.getReactionEmote().getEmoji().equals("\uD83C\uDF89")).findFirst();
            } else {
                opt = EmolgaMain.jda.getTextChannelById(channelId).retrieveMessageById(messageId).complete().getReactions().stream().filter(mr -> mr.getReactionEmote().isEmote() && mr.getReactionEmote().getEmote().getId().equals("772191611487780934")).findFirst();
            }
            ArrayList<Member> members = new ArrayList<>();
            opt.ifPresent(mr -> mr.retrieveUsers().complete().stream().filter(u -> !u.isBot() && !u.getId().equals(userId)).forEach(u -> members.add(EmolgaMain.jda.getTextChannelById(channelId).getGuild().retrieveMember(u).complete())));
            ArrayList<Member> wins = new ArrayList<>();
            if (members.size() > 0)
                for (int i = 0; i < winners; i++) {
                    if (members.size() == 0) break;
                    wins.add(members.remove(new Random().nextInt(members.size())));
                }
            //restJDA.getReactionUsers(channelId, messageId, EncodingUtil.encodeUTF8(Constants.TADA))..submit().thenAcceptAsync(ids -> {
            //List<Long> wins = GiveawayUtil.selectWinners(ids, winners);
            String toSend;
            if (wins.isEmpty()) {
                eb.setDescription("Es konnte kein Gewinner ermittelt werden!");
                toSend = "Es konnte kein Gewinner ermittelt werden!";
            } else if (wins.size() == 1) {
                eb.setDescription("Gewinner: " + wins.get(0).getAsMention());
                toSend = "Herzlichen Glückwunsch " + wins.get(0).getAsMention() + "! Du hast" + (prize == null ? "" : " **" + prize + "**") + " gewonnen!";
            } else {
                eb.setDescription("Gewinner:");
                wins.forEach(w -> eb.appendDescription("\n").appendDescription(w.getAsMention()));
                toSend = "Herzlichen Glückwunsch " + wins.stream().map(Member::getAsMention).collect(Collectors.joining(", "));
                toSend += "! Ihr habt" + (prize == null ? "" : " **" + prize + "**") + " gewonnen!";
            }
            mb.setEmbed(eb.appendDescription("\nGehostet von: <@" + (isRole ? "&" : "") + userId + ">").build());
            EmolgaMain.todel.add(this);
            EmolgaMain.jda.getTextChannelById(channelId).editMessageById(messageId, mb.build()).queue();
            EmolgaMain.jda.getTextChannelById(channelId).sendMessage(toSend).queue();

        } catch (Exception e) {
            e.printStackTrace();
            mb.setEmbed(eb.setDescription("Es konnte kein Gewinner festgestellt werden!\nGehostet von: <@" + (isRole ? "&" : "") + userId + ">").build());
            EmolgaMain.jda.getTextChannelById(channelId).editMessageById(messageId, mb.build()).queue();
            EmolgaMain.jda.getTextChannelById(channelId).sendMessage("Es konnte kein Gewinner festgestellt werden!").queue();
        }
    }
}
