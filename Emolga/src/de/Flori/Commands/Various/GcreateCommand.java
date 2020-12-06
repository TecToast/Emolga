package de.Flori.Commands.Various;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import de.Flori.Emolga.EmolgaMain;
import de.Flori.utils.Giveaway;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class GcreateCommand extends Command {
    private final static String CANCEL = "\n\n`Die Erstellung des Giveaways wurde abgebrochen.`";
    private final static String CHANNEL = "\n\n`Tagge bitte einen Channel.`";
    private final static String TIME = "\n\n`Bitte gib die Länge des Giveaways an.`";
    private final static String WINNERS = "\n\n`Bitte gib die Anzahl der Gewinner ein.`";
    private final static String PRIZE = "\n\n`Bitte gib den Preis ein. Dies wird ebenfalls das Giveaway starten.`";

    private final static List<String> CANCEL_WORDS = Arrays.asList("cancel", "!gcancel", "g!cancel");

    private final Set<String> current = new HashSet<>();

    public GcreateCommand() {
        super("gcreate", "`!gcreate` Startet ein Giveaway", CommandCategory.Various, "712035338846994502", "756239772665511947", "518008523653775366", "673833176036147210", "745934535748747364");
    }


    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        // ignore if there's already creation running here
        if (current.contains(tco.getId()))
            return;

        // preliminary giveaway count check
        // we use current text channel as a basis, even though this may not be the final giveaway channel
        // this might need to be changed at some point


        // get started
        current.add(tco.getId());
        if (e.getGuild().getId().equals("712035338846994502") && !tco.getId().equals("735076688144105493")) {
            tco.sendMessage("Wie lange soll das Giveaway laufen?" + TIME).queue();
            waitForTime(e, e.getGuild().getTextChannelById("754239871870042202"));
            return;
        }
        tco.sendMessage("Es geht los! Zuerst, in welchem Channel soll das Giveaway stattfinden?" + CHANNEL).queue();
        waitForChannel(e);
    }

    private void waitForChannel(GuildMessageReceivedEvent event) {
        wait(event, e ->
        {
            // look for the channel, handle not found and multiple
            List<TextChannel> list = e.getMessage().getMentionedChannels();
            if (list.size() != 1) {
                e.getChannel().sendMessage("Du musst einen Channel taggen!" + CHANNEL).queue();
                waitForChannel(event);
                return;
            }
            TextChannel tchan = list.get(0);
            e.getChannel().sendMessage("Das Giveaway wird in " + tchan.getAsMention() + " stattfinden! Wie lange soll das Giveaway laufen?" + TIME).queue();
            waitForTime(e, tchan);
        });
    }

    private void waitForTime(GuildMessageReceivedEvent event, TextChannel tchan) {
        wait(event, e ->
        {
            int seconds = parseShortTime(e.getMessage().getContentRaw());
            if (seconds == -1) {
                e.getChannel().sendMessage("Das ist keine Zeitangabe!" + TIME).queue();
                waitForTime(e, tchan);
                return;
            }
            e.getChannel().sendMessage("Okay! Das Giveaway dauert " + secondsToTime(seconds) + "! Wieviele Gewinner soll es geben?" + WINNERS).queue();
            waitForWinners(e, tchan, seconds);
        });
    }

    private void waitForWinners(GuildMessageReceivedEvent event, TextChannel tchan, int seconds) {
        wait(event, e ->
        {
            try {
                int num = Integer.parseInt(e.getMessage().getContentRaw().trim());
                e.getChannel().sendMessage("Okay! " + num + " Gewinner! Zum Schluss: Was möchtest du giveawayen?" + PRIZE).queue();
                waitForPrize(e, tchan, seconds, num);
            } catch (NumberFormatException ex) {
                e.getChannel().sendMessage("Das ist keine valide Zahl.").queue();
                waitForWinners(e, tchan, seconds);
            }
        });
    }

    private void waitForPrize(GuildMessageReceivedEvent event, TextChannel tchan, int seconds, int winners) {
        wait(event, e ->
        {
            String prize = e.getMessage().getContentRaw();
            if (prize.length() > 250) {
                e.getChannel().sendMessage("Der Preis ist zu lang! Bitte wähle einen kürzeren aus!" + PRIZE).queue();
                waitForPrize(e, tchan, seconds, winners);
                return;
            }
            current.remove(e.getChannel().getId());
            Instant now = Instant.now();
            Instant end = now.plusSeconds(seconds);
            Giveaway g = new Giveaway(tchan.getId(), event.getAuthor().getId(), end, winners, prize);
            Message message = g.render(now);
            tchan.sendMessage(message).queue(m -> {
                m.addReaction(e.getJDA().getGuildById("712035338846994502").getEmoteById("772191611487780934")).queue();
                g.messageId = m.getId();
                JSONObject json = getEmolgaJSON();
                if (!json.has("giveaways")) json.put("giveaways", new JSONArray());
                JSONArray arr = json.getJSONArray("giveaways");
                JSONObject obj = new JSONObject();
                obj.put("tcid", tchan.getId());
                obj.put("mid", m.getId());
                obj.put("author", event.getAuthor().getId());
                obj.put("end", end.toEpochMilli());
                obj.put("winners", winners);
                obj.put("prize", prize);
                arr.put(obj);
                saveEmolgaJSON();
                e.getChannel().sendMessage("Das Giveaway wurde erstellt!").queue();
            });

        });
    }

    private void wait(GuildMessageReceivedEvent event, Consumer<GuildMessageReceivedEvent> action) {
        EmolgaMain.messageWaiter.waitForGuildMessageReceived(
                e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel()),
                e ->
                {
                    if (CANCEL_WORDS.contains(e.getMessage().getContentRaw().toLowerCase())) {
                        event.getChannel().sendMessage("Die Giveawayerstellung wurde abgebrochen.").queue();
                        current.remove(event.getChannel().getId());
                        return;
                    }
                    action.accept(e);
                }, 2, TimeUnit.MINUTES, new Timeout(event));
    }

    private class Timeout implements Runnable {
        private final GuildMessageReceivedEvent event;
        private boolean ran = false;

        private Timeout(GuildMessageReceivedEvent event) {
            this.event = event;
        }

        @Override
        public void run() {
            if (ran)
                return;
            ran = true;
            event.getChannel().sendMessage("Du hast länger als 2 Minuten für eine Antwort gebraucht, deshalb wurde das Giveaway gelöscht.").queue();
            current.remove(event.getChannel().getId());
        }
    }
}
