package de.tectoast.emolga.buttons;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;

import static de.tectoast.emolga.commands.Command.*;

public class HelpButton extends ButtonListener {

    public HelpButton() {
        super("help");
    }

    @Override
    public void process(ButtonClickEvent e, String name) {
        Guild g = e.getGuild();
        Member mem = e.getMember();
        if (name.equals("BACK")) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle("Commands").setColor(java.awt.Color.CYAN);
            builder.setDescription(getHelpDescripion(g, mem));
            builder.setColor(java.awt.Color.CYAN);
            e.editMessageEmbeds(builder.build()).queue();
        } else {
            CommandCategory c = CommandCategory.byName(name);
            if (c.allowsGuild(g) && c.allowsMember(mem)) {
                List<Command> l = getWithCategory(c, g, mem);
                List<MessageEmbed> embeds = new LinkedList<>();
                StringBuilder b = new StringBuilder();
                boolean first = true;
                for (Command cmd : l) {
                    b.append(cmd.getHelp(g)).append("\n");
                    if (b.length() > 1900) {
                        EmbedBuilder emb = new EmbedBuilder();
                        if (first) emb.setTitle(c.getName());
                        embeds.add(emb.setColor(Color.CYAN).setDescription(b.toString()).build());
                        first = false;
                        b.setLength(0);
                    }
                }
                if (b.length() > 0) {

                    EmbedBuilder emb = new EmbedBuilder();
                    if (first) emb.setTitle(c.getName());
                    embeds.add(emb.setColor(Color.CYAN).setDescription(b.toString()).build());
                }
                e.editMessageEmbeds(embeds).queue(i -> i.editOriginalComponents(getHelpButtons(g, mem)).queue());
            } else {
                e.reply("Auf die Kategorie " + c.getName() + " hast du keinen Zugriff!").queue();
            }
        }
    }
}

