package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class RandomizeKillsCommand extends Command {

    public RandomizeKillsCommand() {
        super("randomizekills", "Randomized die Kills auf 6 Mons", CommandCategory.Pokemon);
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) throws Exception {
        List<Integer> l = new LinkedList<>();
        Random r = new Random();
        for (int i = 0; i < 6; i++) {
            int rand = r.nextInt(6) + 1;
            int sum = l.stream().mapToInt(x -> x).sum();
            if (sum + rand > 6) rand = 6 - sum;
            l.add(rand);
        }
        Collections.shuffle(l);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < l.size(); i++) {
            b.append("Pokemon ").append(i + 1).append(": ").append(l.get(i)).append("\n");
        }
        e.reply(b.toString());
    }
}
