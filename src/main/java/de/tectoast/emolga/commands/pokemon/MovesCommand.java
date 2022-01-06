package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;

public class MovesCommand extends Command {
    private static final Logger logger = LoggerFactory.getLogger(MovesCommand.class);

    public MovesCommand() {
        super("moves", "Zeigt die möglichen Attacken des pokemon an (Entweder alle oder nur die physischen etc.)", CommandCategory.Pokemon);
        aliases.add("moves5");
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("form", "Form", "Optionale alternative Form", ArgumentManagerTemplate.Text.of(
                        SubCommand.of("Alola"), SubCommand.of("Galar")
                ), true)
                .add("mon", "Pokemon", "Das Pokemon", Translation.Type.POKEMON)
                .setExample("!moves Emolga --spez --flying")
                .setCustomDescription("!moves [Alola|Galar] <Pokemon> [--phys|--spez|--status] [--feuer|...]")
                .setNoCheck(true)
                .build()
        );
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
            Translation t = getGerName(pokemon, getModByGuild(e), false);
            if (!t.isFromType(Translation.Type.POKEMON)) {
                tco.sendMessage("Das ist kein Pokemon!").queue();
                return;
            }
            pokemon = t.getTranslation();
            logger.info(pokemon);
            try {
                ArrayList<String> attacks;
                int gen = e.getGuild().getId().equals("747357029714231299") || args[0].equalsIgnoreCase("!moves5") ? 5 : 8;
                //logger.info("args[0] = " + args[0]);
                //logger.info("gen = " + gen);
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
                            tco.sendMessageEmbeds(builder.setDescription(str.toString()).build()).queue();
                            builder = new EmbedBuilder();
                            builder.setTitle("Attacken von " + prefix + pokemon).setColor(Color.CYAN);
                            str = new StringBuilder();
                        }
                    }
                    builder.setDescription(str.toString());
                    tco.sendMessageEmbeds(builder.build()).queue();
                }
            } catch (Exception ioException) {
                tco.sendMessage("Es ist ein Fehler aufgetreten!").queue();
                ioException.printStackTrace();
            }
        } else tco.sendMessage("Syntax: !moves <pokemon>").queue();
    }
}
