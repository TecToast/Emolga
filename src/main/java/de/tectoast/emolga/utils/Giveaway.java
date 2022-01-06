package de.tectoast.emolga.utils;

import de.tectoast.emolga.bot.EmolgaMain;
import de.tectoast.emolga.utils.sql.DBManagers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;

import java.awt.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static de.tectoast.emolga.bot.EmolgaMain.emolgajda;
import static de.tectoast.emolga.commands.various.GcreateCommand.secondsToTime;

public class Giveaway {


    public static final Set<Giveaway> giveaways = new HashSet<>();
    public static final Set<Giveaway> toadd = new HashSet<>();
    private static final ScheduledExecutorService giveawayExecutor = new ScheduledThreadPoolExecutor(5);
    private static final HashMap<Long, ScheduledFuture<?>> giveawayFutures = new HashMap<>();
    private static final HashMap<Long, ScheduledFuture<?>> giveawayFinalizes = new HashMap<>();
    private final Instant end;
    private final int winners;
    private final String prize;
    private final long channelId;
    private final long userId;
    private long messageId = -1;
    private boolean isEnded = false;

    public Giveaway(long channelId, long userId, Instant end, int winners, String prize) {
        this.channelId = channelId;
        this.userId = userId;
        this.end = end;
        this.winners = winners;
        this.prize = prize == null ? null : prize.isEmpty() ? null : prize;
        saveToDB();
    }

    public Giveaway(long channelId, long userId, Instant end, int winners, String prize, long mid) {
        this.messageId = mid;
        this.channelId = channelId;
        this.userId = userId;
        this.end = end;
        this.winners = winners;
        this.prize = prize == null ? null : prize.isEmpty() ? null : prize;
        long delay = end.toEpochMilli() - System.currentTimeMillis();
        System.out.println(end.toEpochMilli() - System.currentTimeMillis());
        giveawayFutures.put(mid, giveawayExecutor.scheduleAtFixedRate(() -> {
            Instant now = Instant.now();
            if (now.until(end, ChronoUnit.MILLIS) < 6000) {
                giveawayFinalizes.put(mid, giveawayExecutor.scheduleAtFixedRate(() -> {
                    if (now.until(end, ChronoUnit.MILLIS) < 1000) {
                        end();
                        giveawayFinalizes.get(mid).cancel(false);
                        giveawayFinalizes.remove(mid);
                        return;
                    }
                    render(now);
                }, 0, 1, TimeUnit.SECONDS));
                giveawayFutures.get(mid).cancel(false);
                giveawayFutures.remove(mid);
                return;
            }
            render(now);
        }, 0, 5000, TimeUnit.MILLISECONDS));

    }

    public Instant getEnd() {
        return end;
    }

    public int getWinners() {
        return winners;
    }

    public String getPrize() {
        return prize;
    }

    public long getChannelId() {
        return channelId;
    }

    public long getUserId() {
        return userId;
    }

    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    public boolean isEnded() {
        return isEnded;
    }

    public void saveToDB() {
        DBManagers.GIVEAWAY.saveGiveaway(this);
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
                + "\nGehostet von: <@" + userId + ">");
        if (prize != null)
            eb.setAuthor(prize, null, null);
        if (close)
            eb.setTitle("Letzte Chance!!!", null);
        mb.setEmbeds(eb.build());
        return mb.build();
    }

    private String messageLink() {
        return String.format("\n<https://discordapp.com/channels/%s/%s/%s>", emolgajda.getTextChannelById(channelId).getGuild().getIdLong(), channelId, messageId);
    }

    public void end() {
        isEnded = true;
        DBManagers.GIVEAWAY.removeGiveaway(this);
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
            if (messageId == 775761399737352222L) {
                opt = emolgajda.getTextChannelById(channelId).retrieveMessageById(messageId).complete().getReactions().stream().filter(mr -> mr.getReactionEmote().isEmoji() && mr.getReactionEmote().getEmoji().equals("\uD83C\uDF89")).findFirst();
            } else {
                opt = emolgajda.getTextChannelById(channelId).retrieveMessageById(messageId).complete().getReactions().stream().filter(mr -> mr.getReactionEmote().isEmote() && (mr.getReactionEmote().getEmote().getId().equals("772191611487780934") || mr.getReactionEmote().getEmote().getId().equals("774817002636181535"))).findFirst();
            }
            ArrayList<Member> members = new ArrayList<>();
            opt.ifPresent(mr -> mr.retrieveUsers().complete().stream().filter(u -> !u.isBot() && u.getIdLong() != userId).forEach(u -> members.add(emolgajda.getTextChannelById(channelId).getGuild().retrieveMember(u).complete())));
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
            mb.setEmbeds(eb.appendDescription("\nGehostet von: <@" + userId + ">").build());
            EmolgaMain.todel.add(this);
            emolgajda.getTextChannelById(channelId).editMessageById(messageId, mb.build()).queue();
            emolgajda.getTextChannelById(channelId).sendMessage(toSend).queue();

        } catch (Exception e) {
            e.printStackTrace();
            mb.setEmbeds(eb.setDescription("Es konnte kein Gewinner festgestellt werden!\nGehostet von: <@" + userId + ">").build());
            emolgajda.getTextChannelById(channelId).editMessageById(messageId, mb.build()).queue();
            emolgajda.getTextChannelById(channelId).sendMessage("Es konnte kein Gewinner festgestellt werden!").queue();
        }
    }

    @Override
    public String toString() {
        return "Giveaway{" + "end=" + end +
                ", winners=" + winners +
                ", prize='" + prize + '\'' +
                ", channelId='" + channelId + '\'' +
                ", userId='" + userId + '\'' +
                ", messageId='" + messageId + '\'' +
                ", isEnded=" + isEnded +
                '}';
    }
}
