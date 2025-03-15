package de.tectoast.emolga.ktor

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.exposed.GuildManagerDB
import de.tectoast.emolga.utils.SizeLimitedMap
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get
import dev.minn.jda.ktx.coroutines.await
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import net.dv8tion.jda.api.Permission

private val logger = KotlinLogging.logger {}

fun Route.emolgaAPI() {
    route("/") {
        install(apiGuard)
        get("/validateuser") {
            call.respond(GuildManagerDB.isUserAuthorized(call.userId))
        }
        Ktor.injectedRouteHandlers.forEach { (path, handler) ->
            get(path) {
                handler(call)
            }
        }
        get("/guilds") {
            val guilds = GuildManagerDB.getGuildsForUser(call.userId)
            call.respond(guilds.mapNotNull {
                val g = jda.getGuildById(it) ?: return@mapNotNull null
                GuildMeta(id = g.id, name = g.name, icon = g.iconUrl ?: "")
            })
        }
        route("{guild}") {
            get("channels") {
                val gid = call.requireGuild() ?: return@get
                val guild = jda.getGuildById(gid)!!
                call.respond(guild.categories.associate { cat -> cat.name to cat.textChannels.associate { it.id to it.name } })
            }
            route("/signup") {
                route("/participants") {
                    get {
                        val gid = call.requireGuild() ?: return@get
                        val lsData = db.signups.get(gid) ?: return@get call.respond(HttpStatusCode.NotFound)
                        val members =
                            jda.getGuildById(gid)!!.retrieveMembersByIds(lsData.users.flatMap { it.users }).await()
                                .associateBy { it.idLong }
                        val result = lsData.users.map {
                            ParticipantData(
                                it.users.map { u ->
                                    UserData(
                                        u.toString(),
                                        members[u]!!.user.effectiveName,
                                        members[u]!!.effectiveAvatarUrl.replace(".gif", ".png")
                                    )
                                },
                                it.data,
                                it.conference
                            )
                        }
                        call.respond(ParticipantDataGet(lsData.conferences, result))
                    }
                    post {
                        val gid = call.requireGuild() ?: return@post
                        val (conferences, data) = call.receive<ParticipantDataSet>()
                        val lsData = db.signups.get(gid) ?: return@post call.respond(HttpStatusCode.NotFound)
                        lsData.conferences = conferences
                        data.forEach { (uid, conf) ->
                            lsData.getDataByUser(uid)?.conference = conf
                        }
                        lsData.save()
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }
    }
}

@Serializable
data class ParticipantDataSet(val conferences: List<String>, val data: Map<Long, String?>)

@Serializable
data class ParticipantDataGet(val conferences: List<String>, val data: List<ParticipantData>)

@Serializable
data class ParticipantData(
    val users: List<UserData>,
    val data: Map<String, String>,
    val conference: String? = null,
)

@Serializable
data class UserData(val id: String, val name: String, val avatar: String)

@Serializable
data class GuildMeta(val id: String, val name: String, val icon: String)

val userIdKey = AttributeKey<Long>("userId")
val apiGuard = createRouteScopedPlugin("AuthGuard") {
    onCall { call ->
        val value = call.request.header("UserID") ?: return@onCall call.respondText(
            "No UserID provided",
            status = HttpStatusCode.Unauthorized
        )
        call.attributes.put(userIdKey, value.toLong())
    }
}
val ApplicationCall.userId: Long
    get() = attributes[userIdKey]

suspend fun ApplicationCall.requireGuild(): Long? {
    val gid = parameters["guild"]?.toLongOrNull() ?: run {
        respond(HttpStatusCode.BadRequest)
        return null
    }
    if (GuildManagerDB.getGuildsForUser(userId).contains(gid)) return gid
    respond(HttpStatusCode.Forbidden)
    return null
}

/**
 * Should only be used in routes that are guarded by [apiGuard]
 */
fun PipelineContext<*, ApplicationCall>.userId(): Long {
    return call.sessions.get<UserSession>()!!.userId
}

private val userdataCache = SizeLimitedMap<String, DiscordUser>()
suspend fun HttpClient.getUserData(accessToken: String): DiscordUser {
    return userdataCache.getOrPut(accessToken) {
        logger.info("Fetching user data for $accessToken")
        getWithToken("https://discord.com/api/users/@me", accessToken)
    }
}

suspend fun HttpClient.getGuilds(accessToken: String): List<DiscordGuildData> {
    return getWithToken("https://discord.com/api/users/@me/guilds", accessToken)
}

suspend inline fun <reified T> HttpClient.getWithToken(url: String, accessToken: String): T {
    return get(url) {
        header("Authorization", "Bearer $accessToken")
    }.body()
}

fun List<DiscordGuildData>.emolga(): List<GuildData> {
    val emolgaguilds = jda.guilds.map { it.id }
    return asSequence().filter {
        it.permissions and Permission.MANAGE_SERVER.rawValue > 0
    }.map { gd ->
        GuildData(
            gd.name,
            gd.icon?.let { "https://cdn.discordapp.com/icons/${gd.id}/$it.png" }
                ?: "assets/images/defaultservericon.png",
            gd.id,
            gd.id in emolgaguilds)
    }.sortedBy { !it.joined }.toList()
}
