package de.tectoast.emolga.commands.flegmon;

import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.commands.PepeCommand;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;

public class SoundsCommand extends PepeCommand {

    private final List<String> off = Arrays.asList("scream", "screamlong", "rickroll");

    public SoundsCommand() {
        super("sounds", "Zeigt alle Sound-Snippets an, die der Bot hat");
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        File dir = new File("audio/clips/");
        EmbedBuilder b = new EmbedBuilder();
        b.setTitle("Sounds");
        b.setColor(Color.PINK);
        StringBuilder sb = new StringBuilder();
        for (File file : dir.listFiles()) {
            String s = file.getName().split("\\.")[0];
            if (off.contains(s)) {
                continue;
            }
            sb.append(s.toLowerCase()).append("\n");
        }
        b.setDescription(sb.toString());
        e.reply(b.build());
    }
}
