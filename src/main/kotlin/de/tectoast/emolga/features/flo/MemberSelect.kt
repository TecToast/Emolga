package de.tectoast.emolga.features.flo

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.db
import dev.minn.jda.ktx.interactions.components.menu
import dev.minn.jda.ktx.messages.into
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu
import org.litote.kmongo.eq
import org.litote.kmongo.set
import org.litote.kmongo.setTo

object MemberSelect {
    object Command : CommandFeature<Command.Args>(
        ::Args,
        CommandSpec("memberselect", "NUR FÜR PROGRAMMIERER", 1034431735481520149, 883292170533077002, Constants.G.UDTA)
    ) {
        init {
            restrict(flo)
            registerListener<EntitySelectInteractionEvent> {
                if (it.user.idLong != Constants.FLOID) return@registerListener
                val split = it.componentId.split(";")
                if (split[0] != "memberselect") return@registerListener
                val league = split[1]
                val members = it.mentions.members
                db.db.getCollection<League>("league")
                    .updateOne(League::leaguename eq league, set(League::table setTo members.map { it.idLong }))
                it.reply("Updated Table for $league: " + members.joinToString { it.asMention }).setEphemeral(true)
                    .queue()
            }
        }

        class Args : Arguments() {
            var league by string("League", "League")
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            reply(
                components = EntitySelectMenu.SelectTarget.USER.menu("memberselect;${e.league}", valueRange = 1..25)
                    .into(), ephemeral = true
            )
        }
    }

}