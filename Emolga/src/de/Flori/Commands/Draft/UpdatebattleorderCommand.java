package de.Flori.Commands.Draft;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONObject;

public class UpdatebattleorderCommand extends Command {
    public UpdatebattleorderCommand() {
        super("updatebattleorder", "`!updatebattleorder <MID> <Name>` Aktualisiert den Spielplan MID für die Draftliga", CommandCategory.Flo);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        String mid = msg.split(" ")[1];
        String name = msg.substring(mid.length() + 20);
        JSONObject order = getEmolgaJSON().getJSONObject("drafts").getJSONObject("ASLS7").getJSONObject(name).getJSONObject("battleorder");
        StringBuilder str = new StringBuilder();
        for (int i = 1; i <= order.keySet().size(); i++) {
            str.append("**Spieltag ").append(i).append(":**\n");
            for (String s : order.getString(String.valueOf(i)).split(";")) {
                str.append(tco.getGuild().retrieveMemberById(s.split(":")[0]).complete().getEffectiveName()).append(" vs ").append(tco.getGuild().retrieveMemberById(s.split(":")[1]).complete().getEffectiveName()).append("\n");
            }
            str.append("\n");
        }
        boolean b = false;
        for (TextChannel textChannel : tco.getGuild().getTextChannels()) {
            try {
                Message mes = textChannel.retrieveMessageById(mid).complete();
                mes.editMessage(str.toString()).queue();
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
    }
}
