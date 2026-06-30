package de.tectoast.emolga.ktor.controllers.emolga.authenticated.guild

import de.tectoast.emolga.discord.GuildRoleRepository
import de.tectoast.emolga.ktor.EmolgaGuildWebController
import de.tectoast.emolga.ktor.WebController
import de.tectoast.emolga.ktor.utils.AuthRoutingHelper
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.annotation.Single

@Single(binds = [WebController::class])
class RoleListController(private val auth: AuthRoutingHelper, private val guildRoleRepo: GuildRoleRepository) :
    EmolgaGuildWebController() {
    override fun Route.setup() {
        get("/roles") {
            auth.withGuildAuth { ctx ->
                call.respond(guildRoleRepo.getRolesOfGuild(ctx.guildId))
            }
        }
    }
}
