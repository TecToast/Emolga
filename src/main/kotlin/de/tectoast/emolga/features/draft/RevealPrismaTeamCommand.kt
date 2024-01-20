package de.tectoast.emolga.features.draft

import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.commands.x
import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.json.db
import dev.minn.jda.ktx.messages.into
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle

object RevealPrisma {
    val prismaTeam: MutableMap<Long, PrismaTeam> = HashMap()

    class PrismaTeam(private val mons: List<String>, val index: Int) {
        private var x = 0
        fun nextMon(): PokemonData {
            return (x++).let {
                PokemonData(mons[it], 13 - it)
            }
        }

        class PokemonData(val pokemon: String, val ycoord: Int)
    }

    object RevealPrismaTeamCommand : CommandFeature<RevealPrismaTeamCommand.Args>(
        ::Args, CommandSpec("revealprismateam", "Revealt die Prisma Teams lol", Constants.G.FLP)
    ) {
        override val slashPermissions: DefaultMemberPermissions = DefaultMemberPermissions.DISABLED

        class Args : Arguments() {
            var user by member("User", "Der User lol")
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            val user = e.user
            val prisma = db.league("Prisma")
            val picks = prisma.picks
            val jsonList = picks[user.idLong]!!.reversed()
            val timestamp = System.currentTimeMillis()
            reply(
                "**" + user.effectiveName + "**", components = RevealPrismaButton { this.timestamp = timestamp }.into()
            )
            prismaTeam[timestamp] = PrismaTeam(
                jsonList.map { it.name }, prisma.table.indexOf(user.idLong)
            )
        }
    }

    object RevealPrismaButton : ButtonFeature<RevealPrismaButton.Args>(::Args, ButtonSpec("prisma")) {
        override val buttonStyle = ButtonStyle.PRIMARY
        override val label = "Nächstes Pokemon"

        class Args : Arguments() {
            var timestamp by long("Timestamp", "Timestamp")
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            if (user != Constants.HENNY && user != 213725720407441410L && user != Constants.FLOID) {
                return reply("nö c:", ephemeral = true)
            }
            val pt = prismaTeam[e.timestamp] ?: return reply(":(")
            val pokemonData = pt.nextMon()
            RequestBuilder.updateSingle(
                "1nCPIc-R5hAsoDXvTGSuGyk2c1K8DQqTBm1NGvLyYYm0",
                "Teamübersicht!${pt.index.x(3, 2)}${pokemonData.ycoord}",
                pokemonData.pokemon
            )
            reply("+1", ephemeral = true)
        }
    }
}
