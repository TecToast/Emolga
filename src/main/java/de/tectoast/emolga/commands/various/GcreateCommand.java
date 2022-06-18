package de.tectoast.emolga.commands.various;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Giveaway;
import de.tectoast.toastilities.interactive.ErrorMessage;
import de.tectoast.toastilities.interactive.InteractiveTemplate;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class GcreateCommand extends Command {
    private final static String CANCEL = "Die Erstellung des Giveaways wurde abgebrochen.";
    private final static String CHANNEL = "\n\n`Tagge bitte einen Channel.`";
    private final static String TIME = "\n\n`Bitte gib die Länge des Giveaways an.`";
    private final static String WINNERS = "\n\n`Bitte gib die Anzahl der Gewinner ein.`";
    private final static String PRIZE = "\n\n`Bitte gib den Preis ein. Dies wird ebenfalls das Giveaway starten.`";

    private final static List<String> CANCEL_WORDS = Arrays.asList("cancel", "!gcancel", "g!cancel");

    private final Set<String> current = new HashSet<>();

    private final InteractiveTemplate template;

    //GuildMessageReceivedEvent event, TextChannel tchan, int seconds, int winners
    public GcreateCommand() {
        super("gcreate", "Startet ein Giveaway", CommandCategory.Various,
                712035338846994502L, 756239772665511947L, 518008523653775366L, 673833176036147210L, 745934535748747364L, 821350264152784896L);

        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
        template = new InteractiveTemplate((u, tco, l) -> {
            current.remove(tco.getId());
            TextChannel tchan = (TextChannel) l.get("channel");
            int seconds = (int) l.get("seconds");
            int winners = (int) l.get("winners");
            String prize = (String) l.get("prize");
            Instant now = Instant.now();
            Instant end = now.plusSeconds(seconds);
            Giveaway g = new Giveaway(tchan.getIdLong(), u.getIdLong(), end, winners, prize);
            Message message = g.render(now);
            tchan.sendMessage(message).queue(m -> {
                m.addReaction(tco.getJDA().getEmoteById("772191611487780934")).queue();
                g.setMessageId(m.getIdLong());
                tco.sendMessage("Das Giveaway wurde erstellt!").queue();
            });
        }, CANCEL)
                .addLayer("channel", "Es geht los! Zuerst, in welchem Channel soll das Giveaway stattfinden?" + CHANNEL, m -> {
                    List<TextChannel> channels = m.getMentions().getChannels(TextChannel.class);
                    if (channels.size() != 1) {
                        return new ErrorMessage("Du musst einen Channel taggen!");
                    }
                    return channels.get(0);
                }, o -> ((TextChannel) o).getAsMention())
                .addLayer("seconds", "Das Giveaway wird in {channel} stattfinden! Wie lange soll das Giveaway laufen?" + TIME, m -> {
                    int seconds = parseShortTime(m.getContentRaw());
                    if (seconds == -1) {
                        return new ErrorMessage("Das ist keine valide Zeitangabe!");
                    }
                    return seconds;
                }, o -> secondsToTime((Integer) o))
                .addLayer("winners", "Okay! Das Giveaway dauert {seconds}! Wieviele Gewinner soll es geben?" + WINNERS, m -> {
                    try {
                        return Integer.parseInt(m.getContentRaw().trim());
                    } catch (NumberFormatException ex) {
                        return new ErrorMessage("Das ist keine valide Zahl.");
                    }
                })
                .addLayer("prize", "Okay! {winners} Gewinner! Zum Schluss: Was möchtest du giveawayen?" + PRIZE, m -> {
                    String prize = m.getContentRaw();
                    if (prize.length() > 250) {
                        return new ErrorMessage("Der Preis ist zu lang! Bitte wähle einen kürzeren aus!" + PRIZE);
                    }
                    return prize;
                });
        CANCEL_WORDS.forEach(template::addCancelCommand);
        template.setTimer(2, TimeUnit.MINUTES, "Du hast länger als 2 Minuten für eine Antwort gebraucht, deshalb wurde das Giveaway gelöscht.");
        template.setOnCancel(i -> current.remove(i.getTco().getId()));
    }


    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        if (current.contains(tco.getId()))
            return;
        current.add(tco.getId());
        template.createInteractive(e.getAuthor(), tco, e.getMessage().getIdLong());
    }
}
