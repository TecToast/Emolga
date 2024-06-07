package de.tectoast.emolga.utils.json

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.draft.SignupManager
import de.tectoast.emolga.features.various.ShiftUser
import de.tectoast.emolga.ktor.InstantAsDateSerializer
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.json.emolga.ASLCoachData
import de.tectoast.emolga.utils.json.emolga.Soullink
import de.tectoast.emolga.utils.json.emolga.Statistics
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.NDS
import de.tectoast.emolga.utils.json.showdown.Pokemon
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.messages.into
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import org.bson.BsonDocument
import org.bson.conversions.Bson
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.coroutine.updateOne
import org.litote.kmongo.reactivestreams.KMongo
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.measureTimedValue
import org.litote.kmongo.serialization.configuration as mongoConfiguration

val db: MongoEmolga get() = delegateDb ?: error("MongoDB not initialized!")

private const val DEFAULT_DB_URL = "mongodb://florirp5.fritz.box:27017/"
private const val DEFAULT_DB_NAME = "emolga"
private var delegateDb: MongoEmolga? = null

private val logger = KotlinLogging.logger {}

fun initMongo(dbUrl: String = DEFAULT_DB_URL, dbName: String = DEFAULT_DB_NAME) {
    delegateDb?.let { error("MongoDB already initialized!") }
    delegateDb = MongoEmolga(dbUrl, dbName)
}

class MongoEmolga(dbUrl: String, dbName: String) {
    private val logger = KotlinLogging.logger {}
    val db = run {
        mongoConfiguration = mongoConfiguration.copy(classDiscriminator = "type", encodeDefaults = false)
        KMongo.createClient(dbUrl).coroutine.getDatabase(dbName)
    }

    val config by lazy { db.getCollection<Config>("config") }
    val statistics by lazy { db.getCollection<Statistics>("statistics") }
    val signups by lazy { db.getCollection<LigaStartData>("signups") }
    val drafts by lazy { db.getCollection<League>("league") }
    val soullink by lazy { db.getCollection<Soullink>("soullink") }
    val emolgachannel by lazy { db.getCollection<EmolgaChannelConfig>("emolgachannel") }
    val cooldowns by lazy { db.getCollection<Cooldown>("cooldowns") }
    val configuration by lazy { db.getCollection<Configuration>("configuration") }
    val nameconventions by lazy { db.getCollection<NameConventions>("nameconventions") }
    val typeicons by lazy { db.getCollection<TypeIcon>("typeicons") }
    val pokedex by lazy { db.getCollection<Pokemon>("pokedex") }
    val pickedMons by lazy { db.getCollection<PickedMonsData>("pickedmons") }
    val tierlist by lazy { db.getCollection<Tierlist>("tierlist") }
    val shinycount by lazy { db.getCollection<Shinycount>() }
    val shinyEvent by lazy { db.getCollection<ShinyEvent>("shinyevent") }
    val aslcoach by lazy { db.getCollection<ASLCoachData>("aslcoachdata") }
    val matchresults by lazy { db.getCollection<MatchResult>("matchresults") }
    val logochecksum by lazy { db.getCollection<LogoChecksum>("logochecksum") }
    val ytchannel by lazy { db.getCollection<YTChannel>("ytchannel") }
    val ytvideos by lazy { db.getCollection<YTVideo>("ytvideos") }
    val tipgameuserdata by lazy { db.getCollection<TipGameUserData>("tipgameuserdata") }
    val statestore by lazy { db.getCollection<StateStore>("statestore") }
    val intervaltaskdata by lazy { db.getCollection<IntervalTaskData>("intervaltaskdata") }
    val defaultNameConventions = OneTimeCache {
        nameconventions.find(NameConventions::guild eq 0).first()!!.data
    }

    suspend fun league(name: String) = getLeague(name)!!
    suspend fun getLeague(name: String) = drafts.findOne(League::leaguename eq name)
    suspend fun nds() = (league("NDS") as NDS)

    suspend fun leagueByGuild(gid: Long, vararg uids: Long) =
        drafts.findOne(
            League::guild eq gid,
            *(if (uids.isEmpty()) emptyArray() else arrayOf(League::table all uids.toList()))
        )

    suspend fun leagueForAutocomplete(tc: Long, gid: Long, user: Long) =
        drafts.find(or(League::tcid eq tc, and(League::guild eq gid, League::table contains user))).toList()
            .maxByOrNull { it.tcid == tc }

    context(InteractionData)
    suspend fun leagueByCommand() = leagueByGuild(gid, user)

    suspend fun getDataObject(mon: String, guild: Long = 0): Pokemon {
        return pokedex.get(NameConventionsDB.getDiscordTranslation(mon, guild, true)!!.official.toSDName())!!
    }


    private val scanScope = createCoroutineScope("ScanScope")

    suspend fun leagueByGuildAdvanced(gid: Long, game: List<List<DraftName>>, vararg uids: Long): LeagueResult? {
        val (leagueResult, duration) = measureTimedValue {
            val allOtherFormesGerman = ConcurrentHashMap<String, List<String>>()
            val filterNotNull = uids.mapIndexed { index, uid ->
                scanScope.async {
                    val mons = game[index]
                    val otherFormesEngl = mutableMapOf<DraftName, List<String>>()
                    val (possibleOtherForm, noOtherForm) = mons.partition {
                        ((it.data?.otherFormes?.filterNot { forme -> "-Alola" in forme || "-Galar" in forme || "-Hisui" in forme }
                            .orEmpty() + it.data?.baseSpecies).filterNotNull().also { list ->
                            otherFormesEngl[it] = list
                        }.size) > 0
                    }
                    val allSDTranslations =
                        NameConventionsDB.getAllSDTranslationOnlyOfficialGerman(possibleOtherForm.flatMap { otherFormesEngl[it].orEmpty() })
                    val otherFormesGerman = otherFormesEngl.map { (k, v) ->
                        k.official to v.map { allSDTranslations[it] ?: it }
                    }.toMap()
                    allOtherFormesGerman.putAll(otherFormesGerman)
                    val filters = possibleOtherForm.map {
                        or(PickedMonsData::mons contains it.official,
                            otherFormesGerman[it.official].orEmpty().let { mega -> PickedMonsData::mons `in` mega })
                    }.toTypedArray()
                    val query = and(
                        *(if (noOtherForm.isNotEmpty()) arrayOf((PickedMonsData::mons all noOtherForm.map { it.official })) else emptyArray()),
                        *filters
                    )
                    val finalQuery = and(PickedMonsData::guild eq gid, query)
                    val possible = pickedMons.find(finalQuery).toList()
                    possible.singleOrNull()
                }
            }.awaitAll().filterNotNull()
            if (filterNotNull.size != uids.size) return null
            var currentLeague: String? = null
            for (pickedMon in filterNotNull) {
                if (currentLeague == null) currentLeague = pickedMon.leaguename
                else if (currentLeague != pickedMon.leaguename) return null
            }
            val league = league(currentLeague!!)
            LeagueResult(league, filterNotNull.map { it.idx }, allOtherFormesGerman)
        }
        logger.debug { "DURATION: ${duration.inWholeMilliseconds}" }
        return leagueResult
    }
}

@Serializable
data class TipGameUserData(
    val user: Long,
    val league: String,
    val orderGuesses: MutableMap<Int, Int> = mutableMapOf(),
    val correctGuesses: MutableMap<Int, MutableSet<Int>> = mutableMapOf(),
    val topkiller: String? = null,
    val correctTopkiller: Boolean = false,
    val correctOrderGuesses: Set<Int> = setOf()
) {
    companion object {
        suspend fun addCorrectBattle(user: Long, league: String, gameday: Int, battle: Int) {
            update(user, league, addToSet(TipGameUserData::correctGuesses.keyProjection(gameday), battle))
        }

        suspend fun setOrderGuess(user: Long, league: String, rank: Int, userindex: Int) {
            update(user, league, set(TipGameUserData::orderGuesses.keyProjection(rank) setTo userindex))
        }

        suspend fun setTopKiller(user: Long, league: String, mon: String) {
            update(user, league, set(TipGameUserData::topkiller setTo mon))
        }

        private suspend fun update(user: Long, league: String, update: Bson) = db.tipgameuserdata.updateOne(
            and(TipGameUserData::user eq user, TipGameUserData::league eq league), update, upsert()
        )

    }
}

@Serializable
data class IntervalTaskData(
    val name: String,
    val nextExecution: @Serializable(with = InstantAsDateSerializer::class) Instant,
    val notAfter: @Serializable(with = InstantAsDateSerializer::class) Instant = Instant.DISTANT_FUTURE,
)

@Serializable
data class YTVideo(
    val channelId: String,
    val videoId: String,
    val title: String,
    val publishedAt: @Serializable(with = InstantAsDateSerializer::class) Instant
)

@Serializable
data class YTChannel(
    val user: Long, val channelId: String
)

@Serializable
data class MatchResult(
    val data: List<Int>,
    val indices: List<Int>,
    val leaguename: String,
    val gameday: Int,
) {
    val winnerIndex get() = if (data[0] > data[1]) 0 else 1
}

@Serializable
data class PickedMonsData(val leaguename: String, val guild: Long, val idx: Int, val mons: List<String>)
data class LeagueResult(val league: League, val uids: List<Int>, val otherForms: Map<String, List<String>>)

@Serializable
data class TypeIcon(
    val typename: String, val formula: String
)

@Serializable
data class Config(val teamgraphicShinyOdds: Int, val guildsToUpdate: List<Long> = listOf())

@Serializable
data class Configuration(
    val guild: Long,
    @SerialName("_id") @Contextual val id: Id<Configuration> = newId(),
    val data: MutableMap<String, MutableMap<String, Int>> = mutableMapOf()
)

@Serializable
data class EmolgaChannelConfig(
    val guild: Long, val channels: List<Long>
)

@Serializable
data class Cooldown(
    val guild: Long, val user: Long, val timestamp: Long
)

@Serializable
data class NameConventions(
    val guild: Long, val data: MutableMap<String, String> = mutableMapOf()
)

@Serializable
data class LigaStartData(
    @SerialName("_id") @Contextual val id: Id<LigaStartData> = newId(),
    val guild: Long,
    val users: MutableMap<Long, SignUpData> = mutableMapOf(),
    val signupChannel: Long,
    val logoChannel: Long,
    val signupMessage: String,
    var conferences: List<String> = listOf(),
    var conferenceRoleIds: Map<String, Long> = mapOf(),
    var shiftMessageIds: List<Long> = listOf(),
    var shiftChannel: Long? = null,
    val maxUsers: Int,
    val announceChannel: Long,
    val announceMessageId: Long,
    var extended: Boolean = false,
    val noTeam: Boolean = false,
    val participantRole: Long? = null,
    val withExperiences: Boolean = false,
) {
    val maxUsersAsString
        get() = (maxUsers.takeIf { it > 0 } ?: "?").toString()

    fun conferenceSelectMenus(uid: Long, initial: Boolean): StringSelectMenu {
        return ShiftUser.SelectMenu(options = conferences.map { SelectOption(it, it) }) {
            this.mode = ShiftUser.SelectMenu.Mode.fromBoolean(initial)
            this.uid = uid
        }
    }

    fun getDataByUser(uid: Long) = users[uid] ?: users.values.firstOrNull { it.teammates.contains(uid) }
    fun getOwnerByUser(uid: Long) = users.entries.firstOrNull { it.key == uid || it.value.teammates.contains(uid) }?.key

    suspend fun save() = db.signups.updateOne(this)
    inline fun giveParticipantRole(memberfun: () -> Member) {
        participantRole?.let {
            val member = memberfun()
            member.guild.addRoleToMember(member, member.guild.getRoleById(it)!!).queue()
        }
    }

    fun giveParticipantRole(member: Member) = giveParticipantRole { member }


    fun updateSignupMessage(setMaxUsersToCurrentUsers: Boolean = false) {
        jda.getTextChannelById(announceChannel)!!.editMessageById(
            announceMessageId,
            "$signupMessage\n\n**Teilnehmer: ${users.size}/${if (setMaxUsersToCurrentUsers) users.size else maxUsersAsString}**"
        ).queue()
    }

    fun closeSignup(forced: Boolean = false) {
        val channel = jda.getTextChannelById(announceChannel)!!
        channel.editMessageComponentsById(
            announceMessageId, SignupManager.Button("Anmeldung geschlossen", disabled = true).into()
        ).queue()
        val msg = "_----------- Anmeldung geschlossen -----------_"
        channel.sendMessage(msg).queue()
        if (announceChannel != signupChannel) jda.getTextChannelById(signupChannel)!!.sendMessage(msg).queue()
        if (forced) updateSignupMessage(true)
    }

    val full get() = maxUsers > 0 && users.size >= maxUsers

}

@Serializable
data class SignUpData(
    var teamname: String?,
    var sdname: String,
    var signupmid: Long? = null,
    var logomid: Long? = null,
    var logoUrl: String = "",
    var conference: String? = null,
    val teammates: MutableSet<Long> = mutableSetOf(),
    var experiences: String? = null,
) {
    fun toMessage(user: Long, lsData: LigaStartData) = "Anmeldung von <@${user}>".condAppend(teammates.isNotEmpty()) {
        " (mit ${teammates.joinToString { "<@$it>" }})"
    } + ":\n".condAppend(!lsData.noTeam) {
        "Teamname: **$teamname**\n"
    } + "Showdown-Name: **$sdname**"
}

@Serializable
class Shinycount(
    val names: Map<Long, String>,
    val counter: Map<String, MutableMap<String, Long>>,
    val methodorder: List<String>,
    val userorder: List<Long>
)

@Serializable
data class ShinyEvent(
    val user: Long, val shinies: List<ShinyData>, val points: Int, val messageId: Long? = null
) {
    @Serializable
    data class ShinyData(
        val game: String, val method: String
    )
}

@Serializable
data class LogoChecksum(
    val checksum: String, val fileId: String
)

suspend fun <T : Any> CoroutineCollection<T>.only() = find().first()!!
suspend fun <T : Any> CoroutineCollection<T>.updateOnly(update: Bson) =
    updateOne(BsonDocument(), update.also { logger.debug { it.json } })

@Suppress("unused") // used in other projects
suspend fun <T : Any> CoroutineCollection<T>.updateOnly(update: String) = updateOne("{}", update)

@JvmName("getLigaStartData")
suspend fun CoroutineCollection<LigaStartData>.get(guild: Long) = find(LigaStartData::guild eq guild).first()

@JvmName("getNameConventions")
suspend fun CoroutineCollection<NameConventions>.get(guild: Long) =
    find(NameConventions::guild eq guild).first()?.data ?: db.defaultNameConventions()

@JvmName("getPokedex")
suspend fun CoroutineCollection<Pokemon>.get(id: String) = find(Pokemon::id eq id).first()

@JvmName("getYTChannelByUser")
suspend fun CoroutineCollection<YTChannel>.get(user: Long) = find(YTChannel::user eq user).first()

@JvmName("getYTChannelByChannelId")
suspend fun CoroutineCollection<YTChannel>.get(channelId: String) = find(YTChannel::channelId eq channelId).first()

@JvmName("getIntervalTaskData")
suspend fun CoroutineCollection<IntervalTaskData>.get(name: String) = find(IntervalTaskData::name eq name).first()
