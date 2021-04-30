package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class CanlearnCommand extends Command {
    public CanlearnCommand() {
        super("canlearn", "`!canlearn <Pokemon> <Attacke>` Zeigt, ob das pokemon diese Attacke erlernen kann", CommandCategory.Pokemon);
        aliases.add("canlearn5");
    }


    @Override
    public void process(GuildCommandEvent e) {
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
            } else if (args[1].toLowerCase().contains("unova") && getModByGuild(e).equals("nml")) {
                pokemon = args[2];
                atk = msg.substring(pokemon.length() + 17);
                form = "Unova";
            } else {
                pokemon = args[1];
                atk = msg.substring(pokemon.length() + 11);
            }
            Translation t = getGerName(pokemon, getModByGuild(e));
            if (!t.isFromType(Translation.Type.POKEMON)) {
                tco.sendMessage("Das ist kein Pokemon!").queue();
                return;
            }
            pokemon = t.getTranslation();
            Translation movet = getGerName(atk, getModByGuild(e));
            if (!movet.isFromType(Translation.Type.MOVE)) {
                tco.sendMessage("Das ist keine Attacke!").queue();
                return;
            }
            atk = movet.getTranslation();
            try {
                tco.sendMessage((form.equals("Normal") ? "" : form + "-") + pokemon + " kann " + atk + (canLearn(pokemon, form, atk, msg, e.getGuild().getId().equals("747357029714231299") || args[0].equalsIgnoreCase("!canlearn5") ? 5 : 8, getModByGuild(e)) ? "" : " nicht") + " erlernen!").queue();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
