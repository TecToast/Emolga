package de.Flori.Commands.Pokemon;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class CanlearnCommand extends Command {
    public CanlearnCommand() {
        super("canlearn", "`!canlearn <Pokemon> <Attacke>` Zeigt, ob das Pokemon diese Attacke erlernen kann", CommandCategory.Pokemon);
        aliases.add("canlearn5");
    }


    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        String[] args = msg.split(" ");
        if (args.length > 2) {
            String pokemon;
            String form = "Normal";
            String atk;
            if (args[1].toLowerCase().contains("alola")) {
                pokemon = args[2];
                atk = msg.substring(pokemon.length() + 17);
                form = "Alola";
            } else if (args[1].toLowerCase().contains("galar")) {
                pokemon = args[2];
                atk = msg.substring(pokemon.length() + 17);
                form = "Galar";
            } else {
                pokemon = args[1];
                atk = msg.substring(pokemon.length() + 11);
            }
            pokemon = getGerName(pokemon);
            if (!pokemon.startsWith("pkmn;")) {
                tco.sendMessage("Das ist kein Pokemon!").queue();
                return;
            }
            pokemon = pokemon.substring(5);
            String str = getGerName(atk);
            if (!str.split(";")[0].equals("atk")) {
                tco.sendMessage("Das ist keine Attacke!").queue();
                return;
            }
            atk = str.split(";")[1];
            try {
                tco.sendMessage((form.equals("Normal") ? "" : form + "-") + pokemon + " kann " + atk + (canLearn(pokemon, form, atk, msg, e.getGuild().getId().equals("747357029714231299") || args[0].equalsIgnoreCase("!canlearn5") ? 5 : 8) ? "" : " nicht") + " erlernen!").queue();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
