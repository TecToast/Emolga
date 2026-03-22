@file:OptIn(InternalSerializationApi::class)

package de.tectoast.emolga.ktor

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.exposed.*
import de.tectoast.emolga.database.exposed.GuildManagerDB.getGuildsForUser
import de.tectoast.emolga.features.league.K18n_Transaction
import de.tectoast.emolga.features.league.SignupManager
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.RPL
import de.tectoast.emolga.league.config.TransactionAmounts
import de.tectoast.emolga.league.config.TransactionEntry
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.json.*
import de.tectoast.emolga.utils.json.EmolgaConfigHelper.findConfig
import de.tectoast.emolga.utils.repeat.RepeatTask
import de.tectoast.emolga.utils.repeat.RepeatTaskType
import de.tectoast.emolga.utils.teamgraphics.TeamGraphicGenerator
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.send
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.cachingheaders.*
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
import net.dv8tion.jda.api.utils.DiscordAssets
import net.dv8tion.jda.api.utils.ImageFormat
import org.bson.conversions.Bson
import org.litote.kmongo.contains
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import org.litote.kmongo.json
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.reflect.KClass
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val logger = KotlinLogging.logger {}
private val defaultDataCache = SizeLimitedMap<String, String>(maxSize = 10)
private val discordUserCache = SizeLimitedMap<Long, DiscordUserData>(maxSize = 1000)
private val participantDataCache = mutableMapOf<Long, Pair<String, String>>()
internal val pickedDataCache = SizeLimitedMap<Long, List<PokemonPickedData>>()

@Serializable
data class DiscordUserData(val name: String, val avatar: String)

@OptIn(InternalSerializationApi::class, ExperimentalUuidApi::class)
fun Route.emolgaAPI() {
    route("/") {
        install(apiGuard)
        get("/validateuser") {
            call.respond(GuildManagerDB.isUserAuthorized(call.userId))
        }
        route("/sixvspokeworld") {
            sixVsPokeworld()
        }
        Ktor.injectedRouteHandlers.forEach { (path, handler) ->
            get(path) {
                handler(call)
            }
        }
        @Suppress("UNCHECKED_CAST") get("/defaultdata") {
            val path = call.request.queryParameters["path"]?.replace("?", "") ?: return@get call.bad()
            if (!path.startsWith("de.tectoast")) return@get call.bad()
            defaultDataCache[path]?.let { return@get call.respondText(it, ContentType.Application.Json) }
            val split = path.split("#")
            var parentSerializer: KSerializer<Any>? = null
            val serializer = runCatching {
                if (split.size == 2) Class.forName(split[0]).kotlin.also {
                    parentSerializer = it.serializerOrNull() as KSerializer<Any>?
                }.sealedSubclasses.mapNotNull { it.serializerOrNull() }
                    .sortedBy { it.descriptor.annotations.findConfig()?.prio ?: Int.MAX_VALUE }
                    .first { split[1] == "" || it.descriptor.serialName == split[1] } else Class.forName(path).kotlin.serializerOrNull()
            }.getOrNull() as? KSerializer<Any>? ?: return@get call.bad()
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
                GuildMeta(
                    id = g.id,
                    name = g.name,
                    icon = g.iconUrl ?: "",
                    mdb.signups.findOne(LigaStartData::guild eq it) != null,
                    TeamGraphicsMetaDB.getShape(it)
                )
            })
        }
        route("{guild}") {
            route("/teamgraphics") {
                get("/new") {
                    val gid = call.requireGuild() ?: return@get
                    PokemonCropService.getNewPokemonToCrop(gid)?.let {
                        call.respond(it)
                    } ?: call.respond(HttpStatusCode.NotFound)
                }
                post("/data") {
                    val gid = call.requireGuild() ?: return@post
                    val data = call.receive<PokemonCropData>()
                    PokemonCropService.insertPokemonCropData(gid, data, call.userId)
                    call.respond(HttpStatusCode.Accepted)
                }
                staticFiles("/img", File("/teamgraphics/sprites"), index = null)
            }
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
                        val lsData = mdb.signups.get(gid, "") ?: return@get call.respond(HttpStatusCode.NotFound)
                        val allUsers = lsData.users.flatMap { it.users }
                        var newUsers = allUsers.filter { !participantDataCache.containsKey(it) }
                        while (newUsers.isNotEmpty()) {
                            val newVals = jda.getGuildById(gid)!!.retrieveMembersByIds(newUsers.take(100)).await()
                                .associateBy { it.idLong }.mapValues { (_, mem) ->
                                    mem.user.effectiveName to mem.user.effectiveAvatarUrl.replace(
                                        ".gif", ".png"
                                    )
                                }
                            participantDataCache.putAll(
                                newVals
                            )
                            newUsers = newUsers.drop(100)
                        }
                        val result = lsData.users.map {
                            ParticipantData(
                                it.users.map { u ->
                                    UserData(
                                        u.toString(),
                                        participantDataCache[u]?.first ?: "UNKNOWN",
                                        participantDataCache[u]?.second
                                            ?: "https://cdn.discordapp.com/embed/avatars/0.png"
                                    )
                                },
                                it.data, it.logoChecksum != null, it.conference,
                            )
                        }
                        call.respond(ParticipantDataGet(lsData.conferences, result))
                    }
                    post {
                        val gid = call.requireGuild() ?: return@post
                        val (conferences, data) = call.receive<ParticipantDataSet>()
                        val lsData = mdb.signups.get(gid, "") ?: return@post call.respond(HttpStatusCode.NotFound)
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
                    },
                    dataProvider = {
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
                val leagues = mdb.league.find(League::guild eq gid).toFlow().map { it.leaguename }.toList()
                call.respond(leagues)
            }
            route("/league/{leaguename}") {
                delta("/delta", mdb.league, filter = {
                    League::leaguename eq ctx.call.parameters["leaguename"]
                }, descriptor = RPL::class.serializer().descriptor) // TODO: make dynamic
                get("/users") {
                    val gid = call.requireGuild() ?: return@get
                    val leaguename = call.parameters["leaguename"] ?: return@get call.bad()
                    val league = mdb.getLeague(leaguename) ?: return@get call.respond(HttpStatusCode.NotFound)
                    val userIds = league.table
                    val toFetch = userIds.filter { !discordUserCache.containsKey(it) }
                    if (toFetch.isNotEmpty()) {
                        jda.getGuildById(gid)!!.retrieveMembersByIds(toFetch).await().forEach {
                            discordUserCache[it.idLong] = DiscordUserData(
                                name = it.user.effectiveName, avatar = it.effectiveAvatarUrl.replace(".gif", ".png")
                            )
                        }
                    }
                    call.respond(userIds.map {
                        discordUserCache[it] ?: DiscordUserData(
                            it.toString(), DiscordAssets.userDefaultAvatar(ImageFormat.PNG, "0").url
                        )
                    })
                }
            }
        }
    }
    route("/result/{resultid}") {
        get {
            val resultId = call.parameters["resultid"] ?: return@get call.bad()
            ResultCodesDB.getResultDataForUser(resultId)?.let { resultData ->
                call.respond(resultData)
            } ?: call.respond(HttpStatusCode.NotFound)
        }
        post {
            val resultId = call.parameters["resultid"] ?: return@post call.bad()
            val resData = ResultCodesDB.getEntryByCode(resultId) ?: return@post call.respond(HttpStatusCode.NotFound)
            val body = call.receive<List<List<Map<String, KD>>>>()
            if (body.size > 3 || body.isEmpty()) return@post call.bad()
            // TODO: Maybe combine with ResultEntry? and clean up
            val idx1 = resData[ResultCodesDB.P1]
            val idx2 = resData[ResultCodesDB.P2]
            val idxs = listOf(idx1, idx2)
            ResultCodesDB.delete(resData[ResultCodesDB.CODE])
            League.executeOnFreshLock(resData[ResultCodesDB.LEAGUENAME]) {
                val channel = jda.getTextChannelById(resultChannel!!)!!
                // TODO: refactor DraftPlayer GamedayData etc
                val (gamedayData, u1IsSecond) = getGamedayData(idx1, idx2)
                val officialNameCache = mutableMapOf<String, String>()
                val games = body.map { singleGame ->
                    if (singleGame.size != 2 || singleGame.any { it.size > 6 }) return@post call.bad()
                    for (i in 0..1) {
                        if (singleGame[i].values.any { it.deaths !in 0..1 }) return@post call.bad()
                        if (singleGame[i].values.sumOf { it.kills } != singleGame[1 - i].values.sumOf { it.deaths }) return@post call.respond(
                            HttpStatusCode.BadRequest
                        )
                    }
                    ReplayData(
                        kd = singleGame.reversedIf(u1IsSecond).map { p ->
                            p.map {
                                NameConventionsDB.getDiscordTranslation(
                                    it.key, guild
                                )!!.official.also { official ->
                                    officialNameCache[it.key] = official
                                } to it.value
                            }.toMap()
                        },
                        url = "WIFI",
                        winnerIndex = singleGame.reversedIf(u1IsSecond)
                            .indexOfFirst { p -> p.values.sumOf { it.deaths } < p.size }
                        // TODO: implement UI element to select winner on "draws"
                    )
                }
                val fullGameData =
                    FullGameData(idxs.reversedIf(u1IsSecond), gamedayData.gameday, gamedayData.battleIndex, games)
                if (config.replayDataStore != null) {
                    channel.sendResultEntryMessage(
                        resData[ResultCodesDB.GAMEDAY],
                        ResultEntryDescription.MatchPresent(fullGameData.uindices.map { this[it] })
                    )
                } else {
                    channel.sendResultEntryMessage(
                        gamedayData.gameday, ResultEntryDescription.Bo3(fullGameData)
                    )
                }
                docEntry?.analyse(fullGameData)
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
    get("/usage/{league}") {
        val league = call.parameters["league"]?.let { mdb.getLeague(it) } ?: return@get call.respond(
            HttpStatusCode.BadRequest
        )
        val allLeagues = mdb.league.find(League::guild eq league.guild).toFlow().map { it.leaguename }.toList()
        val totalCount = AtomicInteger(0)
        val entries = dependency<ReplayDataStoreRepository>().getAll(league.leaguename)
        val maxGameday: Int = entries.maxOfOrNull { it.gameday } ?: 1
        val gameday = call.queryParameters["gameday"]?.toIntOrNull() ?: maxGameday
        val data = entries.asSequence().filter { it.gameday <= gameday }
            .onEach { totalCount.incrementAndGet() }
            .flatMap { it.games.flatMap { g -> g.kd.flatMap { kd -> kd.keys } } }.groupingBy { it }
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
    get("/picked/{guild}") {
        val gid = call.parameters["guild"]?.toLongOrNull() ?: return@get call.bad()
        val tierlist = Tierlist[gid] ?: return@get call.respond(HttpStatusCode.NotFound)
        val pickedAmount = pickedDataCache.getOrPut(gid) {
            val allLeagues = mdb.league.find(League::guild eq gid).toList()
            val language = allLeagues.firstOrNull()?.tierlist?.language ?: return@get call.bad()
            val allEntries = allLeagues.flatMap { league ->
                val mons = league.picks.values.flatten().filterNot { it.quit }
                mons.map { league.displayName to it }
            }
            val lookUp = allEntries.groupBy { it.second.name }
            val allMonsTranslations = NameConventionsDB.getAllData(
                lookUp.keys, NameConventionsDB.GERMAN, gid
            )
            val tierLookup = tierlist.retrieve(allMonsTranslations.values.map { it.tlForLanguage(language) })
            lookUp.map {
                val nameData = allMonsTranslations[it.key]!!
                val value = it.value
                val amount = value.size
                val name = nameData.tlForLanguage(language)
                val tier = tierLookup[name] ?: "N/A"
                val englishOfficial = nameData.otherOfficial!!
                val spriteName = if ("-" in englishOfficial) {
                    mdb.pokedex.get(englishOfficial.toSDName())!!.calcSpriteName()
                } else englishOfficial.toSDName()
                PokemonPickedData(
                    name,
                    tier,
                    value.map { v -> DivisionPickedData(v.first, v.second.tera) },
                    spriteName,
                    amount
                )
            }.sortedByDescending { it.amount }
        }
        call.respond(pickedAmount)
    }
    get("/liveteam") {
        val token = call.request.queryParameters["token"] ?: return@get call.bad()
        val uuid = Uuid.parseHexDashOrNull(token) ?: return@get call.bad()
        val leaguename = LiveTeamDB.getByCode(uuid) ?: return@get call.respond(HttpStatusCode.NotFound)
        val numRaw = call.request.queryParameters["num"]?.toIntOrNull() ?: return@get call.bad()
        val league = mdb.getLeague(leaguename) ?: return@get call.respond(HttpStatusCode.NotFound)
        val style = league.config.teamgraphics?.style ?: return@get call.respond(
            HttpStatusCode.NotFound
        )
        if (numRaw == -1) {
            call.caching = CachingOptions(
                CacheControl.MaxAge(60 * 60 * 5)
            )
            return@get call.respondBytes(
                Files.readAllBytes(Path(style.backgroundPath)), contentType = ContentType.Image.PNG
            )
        }
        val num = numRaw / 2
        val withMons = numRaw % 2
        val tableSize = league.table.size
        val roundIndex = (num / tableSize)
        val takePicks = roundIndex + withMons
        val indexInRound = num % tableSize
        val idx = league.originalorder[roundIndex + 1]?.getOrNull(indexInRound) ?: return@get call.respond(
            HttpStatusCode.NotFound
        )
        call.respondTeamGraphic(league, idx, takePicks, blankBackground = true)
    }
    get("/teamgraphic") {
        val token = call.request.queryParameters["token"] ?: return@get call.bad()
        val uuid = Uuid.parseHexDashOrNull(token) ?: return@get call.bad()
        val leaguename = LiveTeamDB.getByCode(uuid) ?: return@get call.respond(HttpStatusCode.NotFound)
        val idx = call.request.queryParameters["idx"]?.toIntOrNull() ?: return@get call.bad()
        val mons = call.request.queryParameters["mons"]?.toIntOrNull()
        val league = mdb.getLeague(leaguename) ?: return@get call.respond(HttpStatusCode.NotFound)
        call.respondTeamGraphic(league, idx, mons, blankBackground = false)
    }
    route("/transaction/{transactionid}") {
        get {
            val transactionid = call.parameters["transactionid"] ?: return@get call.bad()
            val (leaguename, idx) = TransactionCodesDB.getDataByCode(transactionid) ?: return@get call.respond(
                HttpStatusCode.NotFound
            )
            val league = mdb.getLeague(leaguename) ?: return@get call.respond(HttpStatusCode.NotFound)
            val maxTransactionPoints = league.config.transaction?.maxPoints ?: return@get call.bad()
            val guild = league.guild
            val teraPickConfig = league.config.teraPick
            val allMons = Tierlist.getAllPokemonWithTera(guild, teraPickConfig?.tlIdentifier ?: "TERA")
            val lookup = allMons.associateBy { it.name }
            val alreadyPicked = league.picks.values.flatten().filter { !it.quit }.map { it.name }
            val officialToTL = NameConventionsDB.convertAllOfficialToTL(alreadyPicked, guild)
            val alreadyPickedTL = alreadyPicked.mapTo(mutableSetOf()) { officialToTL[it] }
            val myPicks = league.picks(idx).filter { !it.quit }.map {
                val tlName = officialToTL[it.name] ?: error("Could not find TL name for ${it.name} in guild $guild")
                val data = lookup[tlName] ?: error("Could not find ${it.name} in tierlist")
                data.copy(tera = it.tera)
            }
            call.respond(
                APITransactionData(
                    picked = myPicks,
                    available = allMons.map {
                        it.copy(picked = alreadyPickedTL.contains(it.name))
                    },
                    teraCount = teraPickConfig?.amount ?: 0,
                    teraMaxPoints = teraPickConfig?.maxPoints,
                    monMaxPoints = league.tierlist.withPointBasedPriceManager { it.globalPoints },
                    transactionPoints = league.persistentData.transaction.amounts[idx]?.remaining(maxTransactionPoints)
                        ?: maxTransactionPoints,
                    maxTransactionPoints = maxTransactionPoints
                )
            )
        }
        post {
            val transactionid = call.parameters["transactionid"] ?: return@post call.bad()
            val (leaguename, idx) = TransactionCodesDB.getDataByCode(transactionid) ?: return@post call.respond(
                HttpStatusCode.NotFound
            )
            val data = call.receive<TransactionRequestData>()
            if (data.picks.size != data.drops.size) return@post call.bad()
            League.executeOnFreshLock(leaguename) {
                val currentPicks = picks[idx] ?: return@post call.bad()
                val transactionConfig = config.transaction ?: return@post call.bad()
                val teraConfig = config.teraPick ?: return@post call.bad()
                if (data.teraUsers.size != teraConfig.amount) return@post call.bad()
                val transactionData = persistentData.transaction
                val dropsOfficial = data.drops.map { drop ->
                    val result =
                        NameConventionsDB.getDiscordTranslation(drop, guild)?.official ?: return@post call.bad()
                    if (!currentPicks.any { it.name == result && !it.quit }) return@post call.bad()
                    result
                }
                val picksOfficial = data.picks.map { pick ->
                    val result =
                        NameConventionsDB.getDiscordTranslation(pick, guild)?.official ?: return@post call.bad()
                    if (isPicked(result)) return@post call.bad()
                    result
                }
                val teraUsersOfficial = data.teraUsers.map {
                    NameConventionsDB.getDiscordTranslation(it, guild)?.official ?: return@post call.bad()
                }
                val amounts = transactionData.amounts.getOrPut(idx) { TransactionAmounts() }
                val gameday = RepeatTask.getTask(leaguename, RepeatTaskType.TransactionDocInsert)?.findGamedayOfWeek()
                    ?: return@post call.bad()
                val currentEntry =
                    transactionData.running.getOrPut(gameday) { mutableMapOf() }.getOrPut(idx) { TransactionEntry() }
                val currentTeraUsers = currentPicks.filter { it.tera }.mapTo(mutableSetOf()) { it.name }
                if (data.picks.isEmpty() && teraUsersOfficial == currentTeraUsers) return@post call.bad() // No changes
                val newTeraUsers = teraUsersOfficial - currentTeraUsers
                val oldTeraUsers = currentTeraUsers - teraUsersOfficial.toSet()
                val teraUserDiscount = dropsOfficial.intersect(currentTeraUsers).size
                val newTransactionPoints =
                    amounts.remaining(transactionConfig.maxPoints) - dropsOfficial.size - newTeraUsers.size + teraUserDiscount
                if (newTransactionPoints < 0) return@post call.bad()
                dropsOfficial.forEachIndexed { index, drop ->
                    val old = currentPicks.firstOrNull { it.name == drop } ?: return@post call.bad()
                    val newName = picksOfficial[index]
                    old.name = newName
                    currentPicks += DraftPokemon(drop, tier = "N/A", quit = true)
                }
                currentPicks.forEach {
                    if (!it.quit)
                        it.tera = teraUsersOfficial.contains(it.name)
                }
                invalidatePicksCache()
                if (tierlist.withLeague {
                        it.checkLegalityOfQueue(
                            idx, emptyList()
                        )
                    } != null) return@post call.bad()

                currentEntry.picks += (picksOfficial - currentEntry.picks.toSet())
                currentEntry.drops += (data.drops - currentEntry.drops.toSet())
                amounts.mons += picksOfficial.size
                amounts.extraTeras += newTeraUsers.size - teraUserDiscount
                save()
                call.respond(HttpStatusCode.NoContent)
                TransactionCodesDB.deleteCode(transactionid)
                tc.send(
                    K18n_Transaction.Done(
                        this[idx],
                        data.drops.joinToString("\n"),
                        data.picks.joinToString("\n"),
                        oldTeraUsers.zip(newTeraUsers).map { (old, new) ->
                            "${
                                NameConventionsDB.convertOfficialToTL(
                                    old, guild
                                )
                            } -> ${NameConventionsDB.convertOfficialToTL(new, guild)}"
                        }.joinToString("\n").ifNotEmpty { "Tera:\n$it\n" },
                        newTransactionPoints,
                        gameday + 1
                    ).translateToLeague()
                ).queue()
                mdb.matchresults.findOne(
                    LeagueEvent::leaguename eq leaguename,
                    LeagueEvent::gameday eq gameday,
                    LeagueEvent::indices contains idx
                )?.let {
                    executeTransactionDocInsert(gameday, listOf(idx))
                }
            }
        }
    }
}

suspend fun RoutingCall.bad() = this.respond(HttpStatusCode.BadRequest)


suspend fun RoutingCall.respondTeamGraphic(league: League, idx: Int, takePicks: Int?, blankBackground: Boolean) {
    val actualPickSize = league.picks(idx).size
    if (takePicks != null && takePicks > actualPickSize) {
        this.caching = CachingOptions(CacheControl.NoCache(null))
        return this.respond(HttpStatusCode.NotFound)
    }
    val actualTakePicks = takePicks ?: actualPickSize
    this.caching = CachingOptions(
        CacheControl.MaxAge(60 * 60 * 5)
    )
    respondBytes(teamGraphicCache.getOrPut("${league.leaguename}#$idx#$actualTakePicks") {
        val img = TeamGraphicGenerator.generate(
            TeamGraphicGenerator.TeamData.singleFromLeague(
                league, idx, takePickCount = actualTakePicks
            ), league.config.teamgraphics!!.style, TeamGraphicGenerator.Options(blankBackground)
        )
        ByteArrayOutputStream().use {
            ImageIO.write(img, "png", it)
            it.toByteArray()
        }
    }, contentType = ContentType.Image.PNG)
}

private val teamGraphicCache = SizeLimitedMap<String, ByteArray>(maxSize = 100)

@Serializable
data class APITransactionData(
    val picked: List<TransactionPokemonData>,
    val available: List<TransactionPokemonData>,
    val teraCount: Int,
    val teraMaxPoints: Int? = null,
    val monMaxPoints: Int? = null,
    val transactionPoints: Int,
    val maxTransactionPoints: Int
)

@Serializable
data class TransactionRequestData(
    val picks: List<String>,
    val drops: List<String>,
    val teraUsers: List<String>,
)

@Serializable
data class TransactionPokemonData(
    val name: String, val tier: String, val teraTier: String?, var tera: Boolean = false, var picked: Boolean = false
)

@Serializable
data class PokemonPickedData(
    val name: String,
    val tier: String,
    val divs: List<DivisionPickedData>,
    val spriteName: String,
    val amount: Int
)

@Serializable
data class DivisionPickedData(val name: String, val tera: Boolean)

@Serializable
data class UsageDataTotal(val total: Int, val maxGameday: Int, val allLeagues: List<String>, val data: List<UsageData>)

@Serializable
data class UsageData(val mon: String, val count: Int)

suspend fun generateFinalMessage(league: League, idxs: List<Int>, data: List<Map<String, KD>>): String {
    val spoiler = SpoilerTagsDB.contains(league.guild)
    return "${
        data.mapIndexed { index, sdPlayer ->
            mutableListOf<Any>("<@${league[idxs[index]]}>", sdPlayer.count { it.value.deaths == 0 }).apply {
                if (spoiler) add(
                    1, "||"
                )
            }.let { if (index % 2 > 0) it.asReversed() else it }
        }.joinToString(":") { it.joinToString(" ") }
    }\n\n${
        data.mapIndexed { index, monData ->
            "<@${league[idxs[index]]}>:\n${
                monData.entries.joinToString("\n") {
                    "${it.key} ${it.value.kills}".condAppend(
                        it.value.deaths > 0, " X"
                    )
                }.surroundWith(if (spoiler) "||" else "")
            }"
        }.joinToString("\n\n")
    }"
}

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
        path, descriptor, ConfigOptionHandler.Delta(
            descriptor, collection, filter,
        ), dataProvider = {
            collection.findOne(filter())!!
        }, requiresGuild = requiresGuild
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
        path, descriptor, ConfigOptionHandler.Full(
            descriptor, dataHandler, T::class, submitString = submitString
        ), requiresGuild = requiresGuild, dataProvider = dataProvider
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
                    .getOrNull() ?: return call.bad()
            val update = EmolgaConfigHelper.parseRemoteDelta(descriptor, resultJson) ?: return call.respond(
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
                    descriptor, configOptionHandler.delta, configOptionHandler.submitString
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
    val hasLogo: Boolean,
    val conference: String? = null,
)

@Serializable
data class UserData(val id: String, val name: String, val avatar: String)

@Serializable
data class GuildMeta(
    val id: String,
    val name: String,
    val icon: String,
    val runningSignup: Boolean,
    val teamgraphicsShape: TeamgraphicsShape? = null
)

@Serializable
enum class TeamgraphicsShape {
    CIRCLE, PENTAGON
}

@Serializable
enum class TeamgraphicsSpriteStyle {
    SUGIMORI, HOME
}

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
