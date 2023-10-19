package de.tectoast.emolga.utils.json

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.condAppend
import de.tectoast.emolga.commands.ifTrue
import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.utils.json.emolga.ASLCoachData
import de.tectoast.emolga.utils.json.emolga.Soullink
import de.tectoast.emolga.utils.json.emolga.Statistics
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.NDS
import de.tectoast.emolga.utils.json.showdown.Pokemon
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import dev.minn.jda.ktx.interactions.components.primary
import dev.minn.jda.ktx.messages.into
import kotlinx.coroutines.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Member
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

private const val DEFAULT_DB_URL = "mongodb://floritemp.fritz.box:27017/"
private const val DEFAULT_DB_NAME = "emolga"
private var delegateDb: MongoEmolga? = null

fun initMongo(dbUrl: String = DEFAULT_DB_URL, dbName: String = DEFAULT_DB_NAME) {
    delegateDb?.let { error("MongoDB already initialized!") }
    delegateDb = MongoEmolga(dbUrl, dbName)
}

class MongoEmolga(dbUrl: String, dbName: String) {
    private val logger = KotlinLogging.logger {}
    val db = run {
        /*registerModule(Json {

        }.serializersModule)*/
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

    val shinycount by lazy { db.getCollection<Shinycount>() }

    val aslcoach by lazy { db.getCollection<ASLCoachData>("aslcoachdata") }
    val matchresults by lazy { db.getCollection<MatchResult>("matchresults") }
    val defaultNameConventions: Map<String, String> by lazy {
        runBlocking {
            nameconventions.find(NameConventions::guild eq 0).first()!!
        }.data
    }

    suspend fun league(name: String) = drafts.findOne(League::leaguename eq name)!!
    suspend fun nds() = (league("NDS") as NDS).also { println("NAME: ${it.leaguename}") }

    suspend fun leagueByGuild(gid: Long, vararg uids: Long) =
        drafts.findOne(League::guild eq gid, League::table all uids.toList())


    private val scanScope =
        CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineName("ScanScope") + CoroutineExceptionHandler { _, t ->
            logger.error("ERROR IN ScanScope SCOPE", t)
            Command.sendToMe("Error in scanScope scope, look in console")
        })

    suspend fun leagueByGuildAdvanced(gid: Long, game: List<List<DraftName>>, vararg uids: Long): LeagueResult? {
        val (leagueResult, duration) = measureTimedValue {
            val allOtherFormesGerman = ConcurrentHashMap<String, List<String>>()
            val filterNotNull = uids.mapIndexed { index, uid ->
                scanScope.async {
                    val mons = game[index]
                    val otherFormesEngl = mutableMapOf<DraftName, List<String>>()
                    val (possibleOtherForm, noOtherForm) = mons.partition {
                        (
                                (it.data?.otherFormes?.filterNot { forme -> "-Alola" in forme || "-Galar" in forme || "-Hisui" in forme }
                                    .orEmpty() + it.data?.baseSpecies).filterNotNull()
                                    .also { list ->
                                otherFormesEngl[it] = list
                                    }.size) > 0
                    }
                    val allSDTranslations =
                        NameConventionsDB.getAllSDTranslationOnlyOfficialGerman(possibleOtherForm.flatMap { otherFormesEngl[it].orEmpty() })
                    val otherFormesGerman = otherFormesEngl.map { (k, v) ->
                        k.official to v.map { allSDTranslations[it]!! }
                    }.toMap()
                    allOtherFormesGerman.putAll(otherFormesGerman)
                    val filters = possibleOtherForm.map {
                        or(
                            PickedMonsData::mons contains it.official,
                            otherFormesGerman[it.official].orEmpty()
                                .let { mega -> PickedMonsData::mons `in` mega }
                        )
                    }.toTypedArray()
                    val query = and(
                        PickedMonsData::mons all noOtherForm.map { it.official }, *filters
                    )
                    val finalQuery = and(PickedMonsData::guild eq gid, query)
                    val possible = pickedMons.find(finalQuery).toList()
                    possible.singleOrNull() ?: possible.firstOrNull { it.user == uid }
                }
            }.awaitAll().filterNotNull()
            if (filterNotNull.size != uids.size) return null
            var currentLeague: String? = null
            for (pickedMon in filterNotNull) {
                if (currentLeague == null) currentLeague = pickedMon.leaguename
                else if (currentLeague != pickedMon.leaguename) return null
            }
            val league = league(currentLeague!!)
            LeagueResult(league, filterNotNull.map { it.user }, allOtherFormesGerman)
        }
        println("DURATION: ${duration.inWholeMilliseconds}")
        return leagueResult
    }
}

@Serializable
data class MatchResult(
    val data: List<Int>,
    val uids: List<Long>,
    val leaguename: String,
    val gameday: Int,
) {
    val winnerIndex get() = if (data[0] > data[1]) 0 else 1
}

@Serializable
data class PickedMonsData(val leaguename: String, val guild: Long, val user: Long, val mons: List<String>)
data class LeagueResult(val league: League, val uids: List<Long>, val otherForms: Map<String, List<String>>)

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
    val participantRole: Long? = null
) {
    val maxUsersAsString
        get() = (maxUsers.takeIf { it > 0 } ?: "?").toString()

    fun conferenceSelectMenus(uid: Long, initial: Boolean) =
        StringSelectMenu("cselect;${initial.ifTrue("initial")}:$uid",
            options = conferences.map { SelectOption(it, it) })

    fun getDataByUser(uid: Long) = users[uid] ?: users.values.firstOrNull { it.teammates.contains(uid) }
    fun getOwnerByUser(uid: Long) = users.entries.firstOrNull { it.key == uid || it.value.teammates.contains(uid) }?.key

    suspend fun save() = db.signups.updateOne(this)
    fun giveParticipantRole(member: Member) {
        participantRole?.let {
            member.guild.addRoleToMember(member, member.guild.getRoleById(it)!!).queue()
        }
    }

    fun updateSignupMessage(setMaxUsersToCurrentUsers: Boolean = false) {
        EmolgaMain.emolgajda.getTextChannelById(announceChannel)!!.editMessageById(
            announceMessageId,
            "$signupMessage\n\n**Teilnehmer: ${users.size}/${if (setMaxUsersToCurrentUsers) users.size else maxUsersAsString}**"
        ).queue()
    }

    fun closeSignup(forced: Boolean = false) {
        val channel = jda.getTextChannelById(announceChannel)!!
        channel.editMessageComponentsById(
            announceMessageId, primary("signupclosed", "Anmeldung geschlossen", disabled = true).into()
        ).queue()
        channel.sendMessage("_----------- Anmeldung geschlossen -----------_").queue()
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
    val teammates: MutableSet<Long> = mutableSetOf()
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

suspend fun <T : Any> CoroutineCollection<T>.only() = find().first()!!
suspend fun <T : Any> CoroutineCollection<T>.updateOnly(update: Bson) = updateOne("{}", update)

@Suppress("unused") // used in other projects
suspend fun <T : Any> CoroutineCollection<T>.updateOnly(update: String) = updateOne("{}", update)

@JvmName("getLigaStartData")
suspend fun CoroutineCollection<LigaStartData>.get(guild: Long) = find(LigaStartData::guild eq guild).first()

@JvmName("getNameConventions")
suspend fun CoroutineCollection<NameConventions>.get(guild: Long) =
    find(NameConventions::guild eq guild).first()?.data ?: db.defaultNameConventions

@JvmName("getPokedex")
suspend fun CoroutineCollection<Pokemon>.get(id: String) = find(Pokemon::id eq id).first()
