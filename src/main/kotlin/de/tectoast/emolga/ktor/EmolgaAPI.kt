package de.tectoast.emolga.ktor

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.exposed.GuildManagerDB
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.database.exposed.ResultCodesDB
import de.tectoast.emolga.database.exposed.SpoilerTagsDB
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.draft.DraftPlayer
import de.tectoast.emolga.utils.json.EmolgaConfigHelper
import de.tectoast.emolga.utils.json.EmolgaConfigHelper.findConfig
import de.tectoast.emolga.utils.json.LigaStartData
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.serializerOrNull
import mu.KotlinLogging
import net.dv8tion.jda.api.Permission

private val logger = KotlinLogging.logger {}
private val defaultDataCache = SizeLimitedMap<String, String>(maxSize = 10)

@OptIn(InternalSerializationApi::class)
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
        get("/defaultdata") {
            val path = call.request.queryParameters["path"]?.replace("?", "")
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            if (!path.startsWith("de.tectoast")) return@get call.respond(HttpStatusCode.BadRequest)
            defaultDataCache[path]?.let { return@get call.respondText(it, ContentType.Application.Json) }
            val split = path.split("#")
            var parentSerializer: KSerializer<Any>? = null
            val serializer = runCatching {
                if (split.size == 2) Class.forName(split[0]).kotlin.also {
                    parentSerializer =
                        it.serializerOrNull() as KSerializer<Any>?
                }.sealedSubclasses.mapNotNull { it.serializerOrNull() }
                    .sortedBy { it.descriptor.annotations.findConfig()?.prio ?: Int.MAX_VALUE }
                    .first { split[1] == "" || it.descriptor.serialName == split[1] } else Class.forName(path).kotlin.serializerOrNull()
            }.getOrNull() as? KSerializer<Any>? ?: return@get call.respond(HttpStatusCode.BadRequest)
            val value =
                runCatching { webJSON.decodeFromString(serializer, "{}") }.getOrNull() ?: return@get call.respond(
                    HttpStatusCode.BadRequest
                )
            val defaultData = webJSON.encodeToString(parentSerializer ?: serializer, value)
            defaultDataCache[path] = defaultData
            call.respondText(defaultData, ContentType.Application.Json)
        }
        get("/guilds") {
            val guilds = getGuildsForUser(call.userId)
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
                                        members[u]?.user?.effectiveName ?: "UNKNOWN",
                                        members[u]?.effectiveAvatarUrl?.replace(".gif", ".png")
                                            ?: "https://cdn.discordapp.com/embed/avatars/0.png"
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
                configOption("/config", LigaStartData.serializer().descriptor) {
                    db.signups.get(gid) ?: LigaStartData(
                        guild = gid,
                        signupChannel = 0,
                        announceChannel = 0,
                        signupMessage = "Hier könnt ihr euch anmelden :)",
                        announceMessageId = -1,
                        maxUsers = 0
                    )
                }
            }
        }
    }
    route("/result/{resultid}") {
        get {
            val resultId = call.parameters["resultid"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            ResultCodesDB.getResultDataForUser(resultId)?.let { resultData ->
                call.respond(resultData)
            } ?: call.respond(HttpStatusCode.NotFound)
        }
        post {
            val resultId = call.parameters["resultid"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val resData = ResultCodesDB.getEntryByCode(resultId) ?: return@post call.respond(HttpStatusCode.NotFound)
            val body = call.receive<List<Map<String, KD>>>()
            if (body.size != 2) return@post call.respond(HttpStatusCode.BadRequest)
            // TODO: Maybe combine with ResultEntry? and clean up
            val idx1 = resData[ResultCodesDB.P1]
            val idx2 = resData[ResultCodesDB.P2]
            val idxs = listOf(idx1, idx2)
            ResultCodesDB.delete(resData[ResultCodesDB.CODE])
            League.executeOnFreshLock(resData[ResultCodesDB.LEAGUENAME]) {
                val channel = jda.getTextChannelById(resultChannel!!)!!
                val wifiPlayers = (0..1).map { DraftPlayer(0, false) }
                val gamedayData = getGamedayData(idx1, idx2, wifiPlayers)
                if (config.replayDataStore != null) {
                    channel.sendResultEntryMessage(
                        resData[ResultCodesDB.GAMEDAY],
                        ResultEntryDescription.FromUids(idxs.map { table[it] }
                            .let { if (gamedayData.u1IsSecond) it.reversed() else it })
                    )
                } else {
                    channel.sendResultEntryMessage(
                        gamedayData.gameday, ResultEntryDescription.Direct(generateFinalMessage(this, idxs, body))
                    )
                }
                val game = body.mapIndexed { index, d ->
                    wifiPlayers[index].apply {
                        val dead = d.count { it.value.d }
                        alivePokemon = d.size - dead
                        winner = d.size != dead
                    }
                }
                val officialNameCache = mutableMapOf<String, String>()
                docEntry?.analyse(
                    listOf(
                        ReplayData(
                            game = game,
                            uindices = idxs,
                            kd = body.map { p ->
                                p.map {
                                    NameConventionsDB.getDiscordTranslation(
                                        it.key,
                                        guild
                                    )!!.official.also { official ->
                                        officialNameCache[it.key] = official
                                    } to (it.value.k to if (it.value.d) 1 else 0)
                                }.toMap()
                            },
                            mons = body.map { l -> l.map { officialNameCache[it.key]!! } },
                            url = "WIFI",
                            gamedayData = gamedayData.apply {
                                numbers = game.map { it.alivePokemon }
                                    .let { l -> if (gamedayData.u1IsSecond) l.reversed() else l }
                            })
                    )
                )
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private suspend fun generateFinalMessage(league: League, idxs: List<Int>, data: List<Map<String, KD>>): String {
    val spoiler = SpoilerTagsDB.contains(league.guild)
    return "${
        data.mapIndexed { index, sdPlayer ->
            mutableListOf<Any>("<@${league[idxs[index]]}>", sdPlayer.count { !it.value.d }).apply {
                if (spoiler) add(
                    1, "||"
                )
            }.let { if (index % 2 > 0) it.asReversed() else it }
        }.joinToString(":") { it.joinToString(" ") }
    }\n\n${
        data.mapIndexed { index, monData ->
            "<@${league[idxs[index]]}>:\n${
                monData.entries.joinToString("\n") {
                    "${it.key} ${it.value.k}".condAppend(
                        it.value.d,
                        " X"
                    )
                }.surroundWith(if (spoiler) "||" else "")
            }"
        }.joinToString("\n\n")
    }"
}

@Serializable
data class KD(val k: Int, val d: Boolean)

data class RouteDataProvider(val user: Long, val gid: Long)

inline fun <reified T : Any> Route.configOption(
    path: String,
    descriptor: SerialDescriptor,
    crossinline dataProvider: suspend RouteDataProvider.() -> T
) {
    route(path) {
        get("/struct") {
            call.respond(EmolgaConfigHelper.buildFromDescriptor(descriptor))
        }
        get("/content") {
            val gid = call.requireGuild() ?: return@get
            call.respond(dataProvider(RouteDataProvider(call.userId, gid)))
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
    if (getGuildsForUser(userId).contains(gid)) return gid
    respond(HttpStatusCode.Forbidden)
    return null
}

private suspend fun getGuildsForUser(userId: Long) =
    if (userId == Constants.FLOID) db.league.find().toFlow().map { it.guild }
        .toSet() + db.signups.find().toFlow().map { it.guild }.toSet() else GuildManagerDB.getGuildsForUser(userId)

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
