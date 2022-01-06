package de.tectoast.emolga.commands.flegmon;

import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.commands.PepeCommand;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SoundsCommand extends PepeCommand {

    private final List<String> off = Arrays.asList("scream", "screamlong", "rickroll");

    public SoundsCommand() {
        super("sounds", "Zeigt alle Sound-Snippets an, die der Bot hat");
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        e.reply(new EmbedBuilder().setTitle("Sounds").setColor(Color.PINK).setDescription(Arrays.stream(new File("audio/clips/").listFiles()).map(file -> file.getName().split("\\.")[0]).filter(s -> !off.contains(s)).sorted().collect(Collectors.joining("\n"))).build());
    }
}
