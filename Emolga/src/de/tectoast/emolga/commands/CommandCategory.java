package de.tectoast.emolga.commands;

import de.tectoast.emolga.utils.Constants;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Predicate;

public enum CommandCategory {

    Admin("\uD83C\uDDE6"),
    Moderator("\uD83C\uDDF2", "Moderator"),
    Draft("\uD83C\uDDE9"),
    Flo("\uD83C\uDDEB"),
    Dexquiz("\uD83C\uDDF6"),
    Music("\uD83C\uDDF2", "Musik"),
    Pokemon("\uD83C\uDDF5", "Pokémon"),
    BS("\uD83C\uDDE7", "Blazing Strikers"),
    Various("\uD83C\uDDFB", "Verschiedenes"),
    Showdown("\uD83C\uDDF8");

    private static final ArrayList<CommandCategory> order = new ArrayList<>(Arrays.asList(Showdown, Pokemon, Draft, Dexquiz, BS, Various, Admin, Moderator));

    static {
        Admin.allowsMember = m -> m.hasPermission(Permission.ADMINISTRATOR);
        Moderator.allowsMember = m -> Admin.allowsMember(m) || m.getRoles().stream().anyMatch(r -> Command.moderatorRoles.containsValue(r.getId()));
        BS.allowsGuild = gid -> gid.equals("712035338846994502");
        Music.allowsGuild = gid -> gid.equals("700504340368064562") || gid.equals("712035338846994502") || gid.equals("673833176036147210");
        Moderator.allowsGuild = Command.moderatorRoles::containsKey;
        Flo.allowsMember = mem -> mem.getId().equals(Constants.FLOID);
        Draft.everywhere = true;
        Flo.everywhere = true;
        Admin.everywhere = true;
        Moderator.everywhere = true;
    }

    String emoji;
    String name;
    private Predicate<Member> allowsMember = m -> true;
    private Predicate<String> allowsGuild = g -> true;
    private boolean everywhere = false;

    CommandCategory(String emoji, String name) {
        this.emoji = emoji;
        this.name = name;
    }

    //(gid.equals("700504340368064562") || gid.equals("712035338846994502") || gid.equals("673833176036147210")

    CommandCategory(String emoji) {
        this.emoji = emoji;
        this.name = this.name();
    }

    public static ArrayList<CommandCategory> getOrder() {
        return order;
    }

    public boolean isEverywhere() {
        return everywhere;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getName() {
        return name;
    }

    public boolean allowsGuild(Guild g) {
        return allowsGuild(g.getId());
    }

    public boolean allowsGuild(String gid) {
        return gid.equals("447357526997073930") || allowsGuild.test(gid);
    }

    public boolean allowsMember(Member mem) {
        return mem.getId().equals(Constants.FLOID) || allowsMember.test(mem);
    }
}
