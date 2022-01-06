package de.tectoast.emolga.commands.various;

import de.tectoast.emolga.bot.EmolgaMain;
import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Constants;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jsolf.JSONObject;

public class NicknameCommand extends Command {
    public NicknameCommand() {
        super("nickname", "Ändert deinen Nickname (funktioniert nur 1x pro Woche)", CommandCategory.Various, Constants.BSID, Constants.ASLID);
        setArgumentTemplate(ArgumentManagerTemplate.builder().add("name", "Neuer Name", "Der neue Nickname", ArgumentManagerTemplate.Text.any())
                .setExample("!nickname IchMagEmolga")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        String nickname = msg.substring(10);
        if (!e.getGuild().getSelfMember().canInteract(member)) {
            tco.sendMessage("Ich kann deinen Nickname nicht modifizieren!").queue();
            return;
        }
        if (nickname.length() > 32) {
            tco.sendMessage("Dieser Nickname ist zu lang! (Er darf maximal 32 Zeichen enthalten)").queue();
            return;
        }
        try {
            JSONObject json = getEmolgaJSON();
            Guild g = tco.getGuild();
            if (!json.has("cooldowns")) json.put("cooldowns", new JSONObject());
            if (!json.getJSONObject("cooldowns").has(g.getId()))
                json.getJSONObject("cooldowns").put(g.getId(), new JSONObject());
            if (json.getJSONObject("cooldowns").getJSONObject(g.getId()).has(member.getId())) {
                long l = Long.parseLong(json.getJSONObject("cooldowns").getJSONObject(g.getId()).getString(member.getId()));
                long untilnow = System.currentTimeMillis() - l;
                if (untilnow < 604800000) {
                    long delay = 604800000 - untilnow;
                    int days = (int) (delay / 86400000);
                    delay -= days * 86400000L;
                    int hours = (int) (delay / 3600000);
                    delay -= hours * 3600000L;
                    int minutes = (int) (delay / 60000);
                    tco.sendMessage(member.getAsMention() + " Du kannst deinen Namen noch nicht wieder ändern!\nCooldown: " + days + "d " + hours + "h " + minutes + "m").queue();
                    return;
                }
            }
            String oldname = member.getEffectiveName();
            member.modifyNickname(nickname).complete();
            if (g.getId().equals("518008523653775366"))
                EmolgaMain.emolgajda.getGuildById("518008523653775366").getTextChannelById("728675253924003870").sendMessage(oldname + " hat sich in " + nickname + " umbenannt!").queue();
            tco.sendMessage(member.getAsMention() + " Dein Nickname wurde erfolgreich geändert!").queue();
            json.getJSONObject("cooldowns").getJSONObject(g.getId()).put(member.getId(), Long.toString(System.currentTimeMillis()));
            saveEmolgaJSON();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
