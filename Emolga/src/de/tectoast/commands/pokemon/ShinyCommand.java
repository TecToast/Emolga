package de.tectoast.commands.pokemon;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class ShinyCommand extends Command {
    public ShinyCommand() {
        super("shiny", "`!shiny <Pokemon>` Zeigt das Shiny des Pokemons an", CommandCategory.Pokemon);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        String msg = e.getMessage().getContentDisplay();
        String mon = getGerName(msg.substring(7));
        if (!mon.startsWith("pkmn;")) {
            tco.sendMessage("Das ist kein Pokemon!").queue();
            return;
        }
        tco.sendMessage(getShinySpriteJSON().getString(String.valueOf(getDataJSON(getModByGuild(e)).getJSONObject(getSDName(mon.substring(5))).getInt("num")))).queue();
    }
}
