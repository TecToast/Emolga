package de.tectoast.emolga.commands;

import de.tectoast.emolga.utils.Constants;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

public enum CommandCategory {

    Admin("\uD83C\uDDE6"),
    Moderator("\uD83C\uDDF2"),
    Draft("\uD83C\uDDE9"),
    Flo("\uD83C\uDDEB"),
    Dexquiz("\uD83C\uDDF6"),
    Music("\uD83C\uDDF2", "Musik"),
    Pokemon("\uD83C\uDDF5", "Pokémon"),
    BS("\uD83C\uDDE7", "Blazing Strikers"),
    Various("\uD83C\uDDFB", "Verschiedenes"),
    Showdown("\uD83C\uDDF8"),
    Pepe("822140307558629446");

    private static final ArrayList<CommandCategory> order = new ArrayList<>(Arrays.asList(Pepe, Showdown, Pokemon, Draft, Dexquiz, Various, Moderator, Music));

    public static final List<Long> musicGuilds = new LinkedList<>();

    static {
        Moderator.allowsMember = m -> Admin.allowsMember(m) || m.getRoles().stream().anyMatch(r -> Command.moderatorRoles.containsValue(r.getIdLong()));
        BS.allowsGuildId = gid -> gid.equals(712035338846994502L);
        Music.allowsGuildId = musicGuilds::contains;
        Moderator.allowsGuildId = Command.moderatorRoles::containsKey;
        Pepe.allowsGuildId = gid -> gid.equals(605632286179983360L);
        Flo.allowsMember = mem -> mem.getIdLong() == Constants.FLOID;
        Admin.allowsMember = Flo.allowsMember;
        Draft.everywhere = true;
        Flo.everywhere = true;
        Admin.everywhere = true;
        Moderator.everywhere = true;
        Pepe.isEmote = true;
        BS.disabled = "Die Blazing Strikers Commands wurden dauerhaft abgeschafft!";
        //Music.disabled = "Die Musikfunktionen wurden aufgrund einer Fehlfunktion komplett deaktiviert!";
    }

    String disabled = "";
    String emoji;
    String name;
    boolean isEmote = false;
    private Predicate<Member> allowsMember = m -> true;
    private Predicate<Long> allowsGuildId = g -> true;
    private boolean everywhere = false;

    CommandCategory(String emoji, String name) {
        this.emoji = emoji;
        this.name = name;
    }

    CommandCategory(String emoji) {
        this.emoji = emoji;
        this.name = this.name();
    }

    //(gid.equals("700504340368064562") || gid.equals("712035338846994502") || gid.equals("673833176036147210")

    public static CommandCategory byName(String name) {
        return Arrays.stream(values()).filter(cc -> cc.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public static ArrayList<CommandCategory> getOrder() {
        return order;
    }

    public boolean isEmote() {
        return isEmote;
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
        return allowsGuild(g.getIdLong());
    }

    public boolean allowsGuild(long gid) {
        return gid == 447357526997073930L || allowsGuildId.test(gid);
    }

    public boolean allowsMember(Member mem) {
        boolean test = allowsMember.test(mem);
        return mem.getIdLong() == Constants.FLOID || test;
    }
}
