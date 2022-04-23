package de.tectoast.emolga.commands;

import de.tectoast.emolga.utils.Constants;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

public enum CommandCategory {

    Admin(967390962877870090L),
    Moderator(967390963947438120L),
    Draft(967390964685602867L),
    Flo(967390966153609226L),
    Dexquiz(967392435565105172L),
    Music(967392953934966844L, "Musik"),
    Pokemon(967390967550332968L, "Pokémon"),
    Various(967390968670191717L, "Verschiedenes"),
    Showdown(967391265236860948L),
    Pepe(967391297797251082L);

    public static final List<Long> musicGuilds = new LinkedList<>();
    private static final List<CommandCategory> order = Arrays.asList(Flo, Admin, Moderator, Pepe, Showdown, Pokemon, Draft, Dexquiz, Various, Music);

    static {
        Moderator.allowsMember = m -> Admin.allowsMember(m) || m.getRoles().stream().anyMatch(r -> Command.moderatorRoles.containsValue(r.getIdLong()));
        Music.allowsGuildId = musicGuilds::contains;
        Moderator.allowsGuildId = Command.moderatorRoles::containsKey;
        Pepe.allowsGuildId = gid -> gid.equals(605632286179983360L);
        Flo.allowsMember = mem -> mem.getIdLong() == Constants.FLOID;
        Admin.allowsMember = Flo.allowsMember;
        Draft.everywhere = true;
        Flo.everywhere = true;
        Admin.everywhere = true;
        Moderator.everywhere = true;
        //Music.disabled = "Die Musikfunktionen wurden aufgrund einer Fehlfunktion komplett deaktiviert!";
    }

    final long emote;
    final String name;
    final String disabled = "";
    private Predicate<Member> allowsMember = m -> true;
    private Predicate<Long> allowsGuildId = g -> true;
    private boolean everywhere = false;

    CommandCategory(long emote, String name) {
        this.emote = emote;
        this.name = name;
    }

    CommandCategory(long emote) {
        this.emote = emote;
        this.name = this.name();
    }

    //(gid.equals("700504340368064562") || gid.equals("712035338846994502") || gid.equals("673833176036147210")

    public static CommandCategory byName(String name) {
        return Arrays.stream(values()).filter(cc -> cc.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public static List<CommandCategory> getOrder() {
        return order;
    }

    public boolean isEverywhere() {
        return everywhere;
    }

    public long getEmote() {
        return emote;
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
