package de.Flori.Commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Predicate;

public enum CommandCategory {
    Admin("\uD83C\uDDE6"),
    Draft("\uD83C\uDDE9"),
    Flo("\uD83C\uDDEB"),
    Dexquiz("\uD83C\uDDF6"),
    Music("\uD83C\uDDF2", "Musik"),
    Pokemon("\uD83C\uDDF5"),
    BS("\uD83C\uDDE7", "Blazing Strikers"),
    Various("\uD83C\uDDFB", "Verschiedenes");

    private static final ArrayList<CommandCategory> order = new ArrayList<>(Arrays.asList(Pokemon, Music, Draft, Dexquiz, BS, Various, Admin));

    static {
        Admin.allowsMember = m -> m.hasPermission(Permission.ADMINISTRATOR);
        BS.allowsGuild = g -> g.getId().equals("712035338846994502");
        Music.allowsGuild = g -> g.getId().equals("700504340368064562") || g.getId().equals("712035338846994502") || g.getId().equals("673833176036147210");
    }

    String emoji;
    String name;
    private Predicate<Member> allowsMember = m -> true;
    private Predicate<Guild> allowsGuild = g -> true;

    //(gid.equals("700504340368064562") || gid.equals("712035338846994502") || gid.equals("673833176036147210")

    CommandCategory(String emoji, String name) {
        this.emoji = emoji;
        this.name = name;
    }

    CommandCategory(String emoji) {
        this.emoji = emoji;
        this.name = this.name();
    }

    public static ArrayList<CommandCategory> getOrder() {
        return order;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getName() {
        return name;
    }

    public boolean allowsGuild(Guild g) {
        return g.getId().equals("447357526997073930") || allowsGuild.test(g);
    }

    public boolean allowsMember(Member mem) {
        return mem.getId().equals("175910318608744448") || allowsMember.test(mem);
    }
}
