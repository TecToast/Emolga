package de.tectoast.emolga.commands

import de.tectoast.emolga.utils.Constants
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member

enum class CommandCategory {
    Admin(967390962877870090L), Moderator(967390963947438120L), Draft(967390964685602867L), Flo(967390966153609226L), Dexquiz(
        967392435565105172L
    ),
    Music(967392953934966844L, "Musik"), Pokemon(967390967550332968L, "Pokémon"), Various(
        967390968670191717L,
        "Verschiedenes"
    ),
    Showdown(967391265236860948L), Pepe(967391297797251082L), Soullink(967717447199244309L);

    val emote: Long
    val categoryName: String
    private var allowsMemberFun: (Member) -> Boolean = { true }
    private var allowsGuildIdFun: (Long) -> Boolean = { true }
    var isAdmin: Boolean = false
    var isEverywhere = false
        private set

    constructor(emote: Long, name: String) {
        this.emote = emote
        this.categoryName = name
    }

    constructor(emote: Long) {
        this.emote = emote
        this.categoryName = name
    }

    fun allowsGuild(g: Guild): Boolean {
        return allowsGuild(g.idLong)
    }

    fun allowsGuild(gid: Long): Boolean {
        return gid == 447357526997073930L || allowsGuildIdFun(gid)
    }

    fun allowsMember(mem: Member): Boolean {
        return mem.idLong == Constants.FLOID || this.allowsMemberFun(mem)
    }


    companion object {

        val musicGuilds: MutableList<Long> = mutableListOf()
        val order = listOf(Flo, Admin, Moderator, Pepe, Showdown, Pokemon, Draft, Dexquiz, Various, Music, Soullink)

        init {
            Moderator.allowsMemberFun = { m: Member ->
                Admin.allowsMember(m) || m.roles.any { Command.moderatorRoles.containsValue(it.idLong) }
            }
            Music.allowsGuildIdFun = musicGuilds::contains
            Moderator.allowsGuildIdFun =
                Command.moderatorRoles::containsKey
            Pepe.allowsGuildIdFun =
                { it == 605632286179983360L || it == 817156502912761867L }
            Flo.allowsMemberFun =
                { it.idLong == Constants.FLOID }
            Admin.allowsMemberFun = Flo.allowsMemberFun
            Soullink.allowsGuildIdFun = { gid: Long? -> gid == 695943416789598208L }
            Draft.isEverywhere = true
            Flo.isEverywhere = true
            Admin.isEverywhere = true
            Moderator.isEverywhere = true
            Flo.isAdmin = true
            Admin.isAdmin = true
            Moderator.isAdmin = true
            //Music.disabled = "Die Musikfunktionen wurden aufgrund einer Fehlfunktion komplett deaktiviert!";
        }

        //(gid.equals("700504340368064562") || gid.equals("712035338846994502") || gid.equals("673833176036147210")
        fun byName(name: String?): CommandCategory? {
            return values()
                .firstOrNull { commandCategory: CommandCategory? ->
                    commandCategory!!.categoryName.equals(
                        name,
                        ignoreCase = true
                    )
                }
        }
    }
}