package de.Flori.Commands.Draft;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONObject;

public class UpdateorderfromfileCommand extends Command {
    public UpdateorderfromfileCommand() {
        super("updateorderfromfile", "`!updateorderfromfile <MID> <Name>", CommandCategory.Flo);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        try {
            String mid = msg.split(" ")[1];
            String name = msg.substring(mid.length() + 22);
            Guild g = tco.getGuild();
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
                            //System.out.println(s);
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
                        tco.sendMessage("Success!").queue();
                    } else {
                        tco.sendMessage("Die Nachricht wurde nicht gefunden!").queue();
                    }
                } else {
                    tco.sendMessage("Es gibt keine Liga mit dem Namen " + name + "!").queue();
                }
            } else tco.sendMessage("Es wurde noch kein Draft erstellt!").queue();
        } catch (Exception ex) {
            tco.sendMessage("Es ist ein Fehler aufgetreten!").queue();
            ex.printStackTrace();
        }
    }
}
