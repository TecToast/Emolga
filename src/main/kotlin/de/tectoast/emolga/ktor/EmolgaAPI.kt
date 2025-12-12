@file:OptIn(InternalSerializationApi::class)

package de.tectoast.emolga.ktor

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.exposed.GuildManagerDB
import de.tectoast.emolga.database.exposed.GuildManagerDB.getGuildsForUser
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.database.exposed.ResultCodesDB
import de.tectoast.emolga.database.exposed.SpoilerTagsDB
import de.tectoast.emolga.features.draft.SignupManager
import de.tectoast.emolga.league.GPC
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.draft.DraftPlayer
import de.tectoast.emolga.utils.json.EmolgaConfigHelper
import de.tectoast.emolga.utils.json.EmolgaConfigHelper.findConfig
import de.tectoast.emolga.utils.json.LigaStartConfig
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get
import dev.minn.jda.ktx.coroutines.await
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.*
import kotlinx.serialization.builtins.LongAsStringSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.User
import org.bson.conversions.Bson
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import org.litote.kmongo.json
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}
private val defaultDataCache = SizeLimitedMap<String, String>(maxSize = 10)
private val discordUserCache = SizeLimitedMap<Long, DiscordUserData>(maxSize = 1000)

@Serializable
data class DiscordUserData(val name: String, val avatar: String)

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
        @Suppress("UNCHECKED_CAST")
        get("/defaultdata") {
            val path = call.request.queryParameters["path"]?.replace("?", "")
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            if (!path.startsWith("de.tectoast")) return@get call.respond(HttpStatusCode.BadRequest)
            defaultDataCache[path]?.let { return@get call.respondText(it, ContentType.Application.Json) }
            val split = path.split("#")
            var parentSerializer: KSerializer<Any>? = null
            val serializer = runCatching {
                if (split.size == 2) Class.forName(split[0]).kotlin.also {
                    parentSerializer = it.serializerOrNull() as KSerializer<Any>?
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
            get("roles") {
                val gid = call.requireGuild() ?: return@get
                val guild = jda.getGuildById(gid)!!
                val self = guild.selfMember
                call.respond(guild.roles.filter { !it.isPublicRole }
                    .associate { it.idLong * (if (self.canInteract(it)) 1 else -1) to it.name })
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
                                }, it.data, it.conference
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
                full<LigaStartConfig>(
                    "/config",
                    submitString = "Anmeldung eröffnen",
                    dataHandler = { config, provider ->
                        SignupManager.createSignup(provider.gid, config)
                    }, dataProvider = {
                        LigaStartConfig(
                            signupChannel = 0,
                            announceChannel = 0,
                            signupMessage = "Hier könnt ihr euch anmelden :)",
                            maxUsers = 0
                        )
                    })
            }
            get("/leagues") {
                val gid = call.requireGuild() ?: return@get
                val leagues = db.league.find(League::guild eq gid).toFlow().map { it.leaguename }.toList()
                call.respond(leagues)
            }
            route("/league/{leaguename}") {
                delta("/delta", db.league, filter = {
                    League::leaguename eq ctx.call.parameters["leaguename"]
                }, descriptor = GPC::class.serializer().descriptor)
                get("/users") {
                    val gid = call.requireGuild() ?: return@get
                    val leaguename = call.parameters["leaguename"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val league = db.getLeague(leaguename) ?: return@get call.respond(HttpStatusCode.NotFound)
                    val userIds = league.table
                    val toFetch = userIds.filter { !discordUserCache.containsKey(it) }
                    if (toFetch.isNotEmpty()) {
                        jda.getGuildById(gid)!!.retrieveMembersByIds(toFetch).await().forEach {
                            discordUserCache[it.idLong] = DiscordUserData(
                                name = it.user.effectiveName,
                                avatar = it.effectiveAvatarUrl.replace(".gif", ".png")
                            )
                        }
                    }
                    call.respond(userIds.map {
                        discordUserCache[it] ?: DiscordUserData(
                            it.toString(),
                            User.DEFAULT_AVATAR_URL.format(0)
                        )
                    })
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
            val body = call.receive<List<List<Map<String, KD>>>>()
            // TODO: Maybe combine with ResultEntry? and clean up
            val idx1 = resData[ResultCodesDB.P1]
            val idx2 = resData[ResultCodesDB.P2]
            val idxs = listOf(idx1, idx2)
            ResultCodesDB.delete(resData[ResultCodesDB.CODE])
            League.executeOnFreshLock(resData[ResultCodesDB.LEAGUENAME]) {
                val channel = jda.getTextChannelById(resultChannel!!)!!
                // TODO: refactor DraftPlayer GamedayData etc
                val gamedayData = getGamedayData(idx1, idx2, (0..1).map { DraftPlayer(0, false) })
                val officialNameCache = mutableMapOf<String, String>()
                val replayDatas = body.map { singleGame ->
                    val game = singleGame.mapIndexed { index, d ->
                        val dead = d.count { it.value.d }
                        DraftPlayer(alivePokemon = d.size - dead, winner = d.size != dead)
                    }
                    ReplayData(
                        game = game,
                        uindices = idxs,
                        kd = singleGame.map { p ->
                            p.map {
                                NameConventionsDB.getDiscordTranslation(
                                    it.key, guild
                                )!!.official.also { official ->
                                    officialNameCache[it.key] = official
                                } to (it.value.k to if (it.value.d) 1 else 0)
                            }.toMap()
                        },
                        mons = singleGame.map { l -> l.map { officialNameCache[it.key]!! } },
                        url = "WIFI",
                        gamedayData = gamedayData.apply {
                            numbers = game.map { it.alivePokemon }
                                .let { l -> if (gamedayData.u1IsSecond) l.reversed() else l }
                        })
                }
                if (config.replayDataStore != null) {
                    channel.sendResultEntryMessage(
                        resData[ResultCodesDB.GAMEDAY],
                        ResultEntryDescription.MatchPresent(idxs.map { this[it] }
                            .let { if (gamedayData.u1IsSecond) it.reversed() else it })
                    )
                } else {
                    channel.sendResultEntryMessage(
                        gamedayData.gameday, ResultEntryDescription.Bo3(
                            body, idxs, (0..1).map { replayDatas.count { rd -> rd.game[it].winner } })
                    )
                }
                docEntry?.analyse(replayDatas)
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
    get("/usage/{league}") {
        val league = call.parameters["league"]?.let { db.getLeague(it) } ?: return@get call.respond(
            HttpStatusCode.BadRequest
        )
        val allLeagues = db.league.find(League::guild eq league.guild).toFlow().map { it.leaguename }.toList()
        val totalCount = AtomicInteger(0)
        val entries = league.persistentData.replayDataStore.data.entries
        val maxGameday: Int = entries.maxOfOrNull { it.key } ?: 1
        val gameday = call.queryParameters["gameday"]?.toIntOrNull() ?: maxGameday
        val data = entries.asSequence().filter { it.key <= gameday }.flatMap { it.value.values }
            .onEach { totalCount.incrementAndGet() }.flatMap { it.mons.flatten() }.groupingBy { it }
            .eachCount().entries.map { (mon, count) ->
                UsageData(
                    mon = NameConventionsDB.getDiscordTranslation(mon, league.guild)?.tlName ?: mon, count = count
                )
            }.sortedWith(compareByDescending<UsageData> { it.count }.thenBy { it.mon })
        call.respond(
            UsageDataTotal(
                total = totalCount.get(), maxGameday = maxGameday, allLeagues = allLeagues, data = data
            )
        )
    }
}

@Serializable
data class UsageDataTotal(val total: Int, val maxGameday: Int, val allLeagues: List<String>, val data: List<UsageData>)

@Serializable
data class UsageData(val mon: String, val count: Int)

suspend fun generateFinalMessage(league: League, idxs: List<Int>, data: List<Map<String, KD>>): String {
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
                        it.value.d, " X"
                    )
                }.surroundWith(if (spoiler) "||" else "")
            }"
        }.joinToString("\n\n")
    }"
}

@Serializable
data class KD(val k: Int, val d: Boolean)

val secureWebJSON = Json {
    ignoreUnknownKeys = false
    isLenient = false
    serializersModule = SerializersModule {
        contextual(Long::class, LongAsStringSerializer)
    }
}

data class RouteDataProvider(val user: Long, val gid: Long, val ctx: RoutingContext)

inline fun <reified T : Any> Route.delta(
    path: String,
    collection: CoroutineCollection<T>,
    requiresGuild: Boolean = true,
    noinline filter: RouteDataProvider.() -> Bson,
    descriptor: SerialDescriptor = T::class.serializer().descriptor,
) {
    configOption<T>(
        path, descriptor,
        ConfigOptionHandler.Delta(
            descriptor, collection, filter,
        ),
        dataProvider = {
            collection.findOne(filter())!!
        },
        requiresGuild = requiresGuild
    )
}

inline fun <reified T : Any> Route.full(
    path: String,
    requiresGuild: Boolean = true,
    submitString: String? = null,
    noinline dataHandler: suspend (T, RouteDataProvider) -> Unit,
    crossinline dataProvider: suspend RouteDataProvider.() -> T
) {
    val descriptor = T::class.serializer().descriptor
    configOption<T>(
        path, descriptor,
        ConfigOptionHandler.Full(
            descriptor, dataHandler, T::class,
            submitString = submitString
        ),
        requiresGuild = requiresGuild,
        dataProvider = dataProvider
    )
}

sealed class ConfigOptionHandler(val delta: Boolean) {
    open val submitString: String? = null

    class Delta<T : Any>(
        val descriptor: SerialDescriptor,
        val collection: CoroutineCollection<T>,
        val filter: RouteDataProvider.() -> Bson,
    ) : ConfigOptionHandler(delta = true) {
        override suspend fun RoutingContext.handle(provider: RouteDataProvider) {
            val asText = call.receiveText()
            val resultJson =
                runCatching { secureWebJSON.decodeFromString<JsonObject>(asText) }.onFailure { it.printStackTrace() }
                    .getOrNull() ?: return call.respond(HttpStatusCode.BadRequest)
            val update =
                EmolgaConfigHelper.parseRemoteDelta(descriptor, resultJson) ?: return call.respond(
                    HttpStatusCode.BadRequest
                )
            logger.info("Updating with delta: ${update.json}")
            collection.updateOne(provider.filter(), update)
            call.respond(HttpStatusCode.Accepted)
        }
    }

    class Full<T : Any>(
        val descriptor: SerialDescriptor,
        val dataHandler: suspend (T, RouteDataProvider) -> Unit,
        val kClass: KClass<T>,
        override val submitString: String? = null,
    ) : ConfigOptionHandler(delta = false) {
        override suspend fun RoutingContext.handle(provider: RouteDataProvider) {
            val result = runCatching {
                secureWebJSON.decodeFromString(
                    kClass.serializer(), call.receiveText()
                )
            }.onFailure { it.printStackTrace() }.getOrNull() ?: return call.respond(
                HttpStatusCode.BadRequest
            )
            dataHandler(result, provider)
            call.respond(HttpStatusCode.Accepted)
        }
    }

    abstract suspend fun RoutingContext.handle(provider: RouteDataProvider)
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> Route.configOption(
    path: String,
    descriptor: SerialDescriptor,
    configOptionHandler: ConfigOptionHandler,
    requiresGuild: Boolean = true,
    crossinline dataProvider: suspend RouteDataProvider.() -> T,
) {
    route(path) {
        get("/struct") {
            if (requiresGuild) call.requireGuild() ?: return@get
            call.respond(
                EmolgaConfigHelper.buildFromDescriptor(
                    descriptor,
                    configOptionHandler.delta,
                    configOptionHandler.submitString
                )
            )
        }
        get("/content") {
            val provider = buildProvider(requiresGuild) ?: return@get
            call.respond(dataProvider(provider))
        }
        post("/save") {
            with(configOptionHandler) {
                val provider = buildProvider(requiresGuild) ?: return@post
                handle(provider)
            }
        }
    }
}

suspend fun RoutingContext.buildProvider(requiresGuild: Boolean): RouteDataProvider? {
    return if (requiresGuild) {
        val gid = call.requireGuild() ?: return null
        RouteDataProvider(call.userId, gid, this)
    } else {
        RouteDataProvider(call.userId, -1, this)
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
            "No UserID provided", status = HttpStatusCode.Unauthorized
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
