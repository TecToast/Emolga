package de.tectoast.commands.bs;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;

public class LeaderboardCommand extends Command {

    public LeaderboardCommand() {
        super("leaderboard", "`!leaderboard` Zeigt das Leaderboard des Servers an", CommandCategory.BS);
        wip = true;
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        StringBuilder str = new StringBuilder();
        HashMap<String, String> names = new HashMap<>();
        e.getGuild().retrieveMembersByIds(leveljson.keySet().toArray(new String[0])).get().forEach(mem -> names.put(mem.getId(), mem.getEffectiveName()));
        for (String s : leveljson.keySet().stream().sorted(Comparator.comparing(leveljson::getLong).reversed()).collect(Collectors.toList())) {
            long l = leveljson.getLong(s);
            if (names.containsKey(s))
                str.append(names.get(s)).append(": ").append(getLevelFromXP(l)).append("\n");
        }
        e.getChannel().sendMessage(str.toString()).queue();
    }
}
