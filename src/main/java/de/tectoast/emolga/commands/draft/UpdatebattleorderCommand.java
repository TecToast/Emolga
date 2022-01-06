package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jsolf.JSONObject;

public class UpdatebattleorderCommand extends Command {
    public UpdatebattleorderCommand() {
        super("updatebattleorder", "Aktualisiert den Spielplan MID für die Draftliga", CommandCategory.Flo);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("mid", "Message-ID", "Die MessageID der Kampfreihenfolge", ArgumentManagerTemplate.DiscordType.ID)
                .add("name", "Draftname", "Der Name der Draftliga", ArgumentManagerTemplate.draft())
                .setExample("!updatebattleorder 839470836624130098 Emolga-Conference")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        ArgumentManager args = e.getArguments();
        long mid = args.getID("mid");
        String name = args.getText("name");
        JSONObject order = getEmolgaJSON().getJSONObject("drafts").getJSONObject(name).getJSONObject("battleorder");
        StringBuilder str = new StringBuilder();
        for (int i = 1; i <= order.keySet().size(); i++) {
            str.append("**Spieltag ").append(i).append(":**\n");
            for (String s : order.getString(String.valueOf(i)).split(";")) {
                str.append(e.getGuild().retrieveMemberById(s.split(":")[0]).complete().getEffectiveName()).append(" vs ").append(e.getGuild().retrieveMemberById(s.split(":")[1]).complete().getEffectiveName()).append("\n");
            }
            str.append("\n");
        }
        boolean b = false;
        for (TextChannel textChannel : e.getGuild().getTextChannels()) {
            try {
                Message mes = textChannel.retrieveMessageById(mid).complete();
                mes.editMessage(str.toString()).queue();
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
    }
}
