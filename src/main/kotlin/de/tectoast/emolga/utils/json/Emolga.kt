package de.tectoast.emolga.utils.json

import de.tectoast.emolga.commands.condAppend
import de.tectoast.emolga.commands.ifTrue
import de.tectoast.emolga.utils.json.emolga.ASLS11
import de.tectoast.emolga.utils.json.emolga.Soullink
import de.tectoast.emolga.utils.json.emolga.Statistics
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.NDS
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.entities.Member
import org.bson.conversions.Bson
import org.litote.kmongo.Id
import org.litote.kmongo.all
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.coroutine.updateOne
import org.litote.kmongo.eq
import org.litote.kmongo.newId
import org.litote.kmongo.reactivestreams.KMongo
import org.litote.kmongo.serialization.configuration as mongoConfiguration

val db: MongoEmolga get() = delegateDb ?: error("MongoDB not initialized!")

private const val DEFAULT_DB_URL = "mongodb://floritemp.fritz.box:27017/"
private var delegateDb: MongoEmolga? = null

fun initMongo(dbUrl: String = DEFAULT_DB_URL) {
    delegateDb?.let { error("MongoDB already initialized!") }
    delegateDb = MongoEmolga(dbUrl)
}

class MongoEmolga(dbUrl: String) {
    val db = run {
        /*registerModule(Json {

        }.serializersModule)*/
        mongoConfiguration = mongoConfiguration.copy(classDiscriminator = "type", encodeDefaults = false)
        KMongo.createClient(dbUrl).coroutine.getDatabase("emolga")
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

    val shinycount by lazy { db.getCollection<Shinycount>() }

    val asls11: ASLS11 get() = error("ASLS11 is not available atm!")
    val asls11nametoid: List<Long> get() = error("ASLS11 is not available atm!")
    val defaultNameConventions: Map<String, String> by lazy {
        runBlocking {
            nameconventions.find(NameConventions::guild eq 0).first()!!
        }.data
    }

    suspend fun league(name: String) = drafts.findOne(League::leaguename eq name)!!
    suspend fun nds() = (league("NDS") as NDS).also { println("NAME: ${it.leaguename}") }

    suspend fun leagueByGuild(gid: Long, vararg uids: Long) =
        drafts.findOne(League::guild eq gid, League::table all uids.toList())
}

@Serializable
data class TypeIcon(
    val typename: String,
    val url: String
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
