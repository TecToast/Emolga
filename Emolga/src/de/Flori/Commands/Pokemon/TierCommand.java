package de.Flori.Commands.Pokemon;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import de.Flori.utils.Draft.Tierlist;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class TierCommand extends Command {
    public TierCommand() {
        super("tier", "`!tier <Pokemon>` Zeigt das Tier des Pokemon in der Liga dieses Servers an.", CommandCategory.Pokemon);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        String pkmn = msg.substring(6);
        Tierlist tierlist = Tierlist.getByGuild(tco.getGuild().getId());
        String tier = tierlist.getTierOf(pkmn);
        if (!tier.equals("")) {
            tco.sendMessage(pkmn + " ist im " + tier + "-Tier!").queue();
        } else {
            tco.sendMessage(pkmn + " befindet sich nicht in der Tierliste!").queue();
        }
    }
}
