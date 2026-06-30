package de.tectoast.emolga.ktor.controllers.emolga.authenticated.guild

import de.tectoast.emolga.discord.GuildChannelRepository
import de.tectoast.emolga.ktor.EmolgaGuildWebController
import de.tectoast.emolga.ktor.WebController
import de.tectoast.emolga.ktor.utils.AuthRoutingHelper
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.annotation.Single

@Single(binds = [WebController::class])
class ChannelListController(private val auth: AuthRoutingHelper, private val guildChannelRepo: GuildChannelRepository) :
    EmolgaGuildWebController() {
    override fun Route.setup() {
        get("/channels") {
            auth.withGuildAuth { ctx ->
                call.respond(guildChannelRepo.getChannelsOfGuild(ctx.guildId))
            }
        }
    }
}
