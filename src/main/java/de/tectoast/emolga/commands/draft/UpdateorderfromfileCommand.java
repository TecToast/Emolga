package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jsolf.JSONObject;

public class UpdateorderfromfileCommand extends Command {
    public UpdateorderfromfileCommand() {
        super("updateorderfromfile", "Aktualisiert die Draftreihenfolge in einer Nachricht", CommandCategory.Flo);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("mid", "Message-ID", "Die MessageID der Draftreihenfolge", ArgumentManagerTemplate.DiscordType.ID)
                .add("name", "Draftname", "Der Name der Draftliga", ArgumentManagerTemplate.draft())
                .setExample("!updateorderfromfile 839470836624130098")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        try {
            ArgumentManager args = e.getArguments();
            long mid = args.getID("mid");
            String name = args.getText("name");
            Guild g = e.getGuild();
            JSONObject json = getEmolgaJSON();
            StringBuilder edit = new StringBuilder(name + ":\n");
            if (json.has("drafts")) {
                JSONObject drafts = json.getJSONObject("drafts").getJSONObject("ASLS7");
                if (drafts.has(name)) {
                    JSONObject order = drafts.getJSONObject(name).getJSONObject("order");
                    for (int i = 1; i <= 12; i++) {
                        String str = order.getString(Integer.toString(i));
                        edit.append(i).append(". Runde\n");
                        for (String s : str.split(",")) {
                            //logger.info(s);
                            edit.append(g.retrieveMemberById(s).complete().getEffectiveName()).append("\n");
                        }
                        edit.append("\n");
                    }
                    boolean b = false;
                    for (TextChannel textChannel : g.getTextChannels()) {
                        try {
                            Message mes = textChannel.retrieveMessageById(mid).complete();
                            mes.editMessage(edit.toString()).queue();
                            b = true;
                            break;
                        } catch (Exception ignored) {
                        }
                    }
                    if (b) {
                        e.reply("Success!");
                    } else {
                        e.reply("Die Nachricht wurde nicht gefunden!");
                    }
                } else {
                    e.reply("Es gibt keine Liga mit dem Namen " + name + "!");
                }
            } else e.reply("Es wurde noch kein draft erstellt!");
        } catch (Exception ex) {
            e.reply("Es ist ein Fehler aufgetreten!");
            ex.printStackTrace();
        }
    }
}
