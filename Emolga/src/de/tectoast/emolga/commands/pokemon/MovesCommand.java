package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;

public class MovesCommand extends Command {
    public MovesCommand() {
        super("moves", "`!moves [Alola|Galar] <pokemon> [--phys|--spez|--status] [--feuer|...]` Zeigt die möglichen Attacken des pokemon an (Entweder alle oder nur die physischen etc.)", CommandCategory.Pokemon);
        aliases.add("moves5");
    }


    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        String[] args = msg.split(" ");
        if (args.length > 1) {
            String pokemon;
            String form = "Normal";
            if (args[1].toLowerCase().contains("alola")) {
                pokemon = args[2];
                form = "Alola";
            } else if (args[1].toLowerCase().contains("galar")) {
                pokemon = args[2];
                form = "Galar";
            } else if (args[1].toLowerCase().contains("unova") && getModByGuild(e).equals("nml")) {
                pokemon = args[2];
                form = "Unova";
            } else {
                pokemon = args[1];
            }
            String string = getGerName(pokemon, getModByGuild(e));
            if (!string.split(";")[0].equals("pkmn")) {
                tco.sendMessage("Das ist kein pokemon!").queue();
                return;
            }
            pokemon = string.split(";")[1];
            System.out.println(pokemon);
            try {
                ArrayList<String> attacks;
                int gen = e.getGuild().getId().equals("747357029714231299") || args[0].equalsIgnoreCase("!moves5") ? 5 : 8;
                //System.out.println("args[0] = " + args[0]);
                //System.out.println("gen = " + gen);
                attacks = getAttacksFrom(pokemon, msg, form, gen, getModByGuild(e));
                Collections.sort(attacks);
                if (attacks.size() == 0) {
                    tco.sendMessage(pokemon + " kann keine Attacken mit den angegebenen Spezifikationen erlernen!").queue();
                } else {
                    if (attacks.size() == 1) if (attacks.get(0).equals("ERROR")) {
                        tco.sendMessage("Dieses pokemon hat keine " + form + "-Form!").queue();
                        return;
                    }
                    EmbedBuilder builder = new EmbedBuilder();
                    String prefix = form.equals("Normal") ? "" : form + "-";
                    builder.setTitle("Attacken von " + prefix + pokemon).setColor(Color.CYAN);
                    StringBuilder str = new StringBuilder();
                    for (String o : attacks) {
                        str.append(o).append("\n");
                        if (str.length() > 1900) {
                            tco.sendMessage(builder.setDescription(str.toString()).build()).queue();
                            builder = new EmbedBuilder();
                            builder.setTitle("Attacken von " + prefix + pokemon).setColor(Color.CYAN);
                            str = new StringBuilder();
                        }
                    }
                    builder.setDescription(str.toString());
                    tco.sendMessage(builder.build()).queue();
                }
            } catch (Exception ioException) {
                tco.sendMessage("Es ist ein Fehler aufgetreten!").queue();
                ioException.printStackTrace();
            }
        } else tco.sendMessage("Syntax: !moves <pokemon>").queue();
    }
}
