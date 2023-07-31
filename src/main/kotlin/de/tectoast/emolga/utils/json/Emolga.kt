package de.tectoast.emolga.utils.json

import com.mongodb.client.model.Filters.*
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import de.tectoast.emolga.commands.condAppend
import de.tectoast.emolga.commands.ifTrue
import de.tectoast.emolga.utils.json.emolga.ASLS11
import de.tectoast.emolga.utils.json.emolga.Soullink
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.NDS
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.BsonDocument
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import kotlin.reflect.KProperty1


val db: MongoEmolga = MongoEmolga()

private const val DB_URL = "mongodb://floritemp.fritz.box:27017/"

class MongoEmolga {
    val db = run {
//        mongoConfiguration = mongoConfiguration.copy(classDiscriminator = "type", encodeDefaults = false)
//        KMongo.createClient(DB_URL).coroutine.getDatabase("emolga")

        MongoClient.create(DB_URL).getDatabase("emolga")
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

    val shinycount by lazy { db.getCollection<Shinycount>("shinycount") }

    val asls11: ASLS11 get() = error("ASLS11 is not available atm!")
    val asls11nametoid: List<Long> get() = error("ASLS11 is not available atm!")
    val defaultNameConventions: Map<String, String> by lazy {
        runBlocking {
//            nameconventions.find(NameConventions::guild eq 0).first()!!
            nameconventions.findOne(NameConventions::guild eq 0)!!
        }.data
    }

    suspend fun league(name: String) = drafts.find(eq(League::leaguename.name, name)).first()
    suspend fun nds() = (league("NDS") as NDS).also { println("NAME: ${it.leaguename}") }

    suspend fun leagueByGuild(gid: Long, vararg uids: Long) =
        drafts.findOne(and(League::guild eq gid, League::table all uids.toList()))
}

@Serializable
data class Config(val teamgraphicShinyOdds: Int, val guildsToUpdate: List<Long> = listOf())

@Serializable
data class Statistics(var drampaCounter: Int)

@Serializable
data class Configuration(
    val guild: Long,
    @SerialName("_id") @Contextual val id: ObjectId = ObjectId(),
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
    @SerialName("_id") @Contextual val id: ObjectId = ObjectId(),
    val guild: Long,
    val users: MutableMap<Long, SignUpData> = mutableMapOf(),
    val signupChannel: Long,
    val logoChannel: Long,
    var conferences: List<String> = listOf(),
    var shiftMessageIds: List<Long> = listOf(),
    var shiftChannel: Long? = null,
    val maxUsers: Int,
    val announceChannel: Long,
    val announceMessageId: Long,
    var extended: Boolean = false
) {
    val maxUsersAsString
        get() = (maxUsers.takeIf { it > 0 } ?: "?").toString().also { println("MAXUSERSASSTRING: $it") }

    fun conferenceSelectMenus(uid: Long, initial: Boolean) =
        StringSelectMenu("cselect;${initial.ifTrue("initial")}:$uid",
            options = conferences.map { SelectOption(it, it) })

    fun getDataByUser(uid: Long) = users[uid] ?: users.values.firstOrNull { it.teammates.contains(uid) }
    fun getOwnerByUser(uid: Long) = users.entries.firstOrNull { it.key == uid || it.value.teammates.contains(uid) }?.key

    suspend fun save() = db.signups.replaceOne(eq(id), this)

}

@Serializable
data class SignUpData(
    var teamname: String,
    var sdname: String,
    var signupmid: Long? = null,
    var logomid: Long? = null,
    var logoUrl: String = "",
    var conference: String? = null,
    val teammates: MutableSet<Long> = mutableSetOf()
) {
    fun toMessage(user: Long) = "Anmeldung von <@${user}>".condAppend(teammates.isNotEmpty()) {
        " (mit ${teammates.joinToString { "<@$it>" }})"
    } + ":\n" + "Teamname: **$teamname**\n" + "Showdown-Name: **$sdname**"
}

@Serializable
class Shinycount(
    val names: Map<Long, String>,
    val counter: Map<String, MutableMap<String, Long>>,
    val methodorder: List<String>,
    val userorder: List<Long>
)

suspend fun <T : Any> MongoCollection<T>.only() = find().first()
suspend fun <T : Any> MongoCollection<T>.updateOnly(update: Bson) = updateOne(empty(), update)

@Suppress("unused") // used in other projects
suspend fun <T : Any> MongoCollection<T>.updateOnly(update: String) = updateOne(empty(), BsonDocument.parse(update))

@JvmName("getLigaStartData")
suspend fun MongoCollection<LigaStartData>.get(guild: Long) = find(LigaStartData::guild eq guild).firstOrNull()

@JvmName("getNameConventions")
suspend fun MongoCollection<NameConventions>.get(guild: Long) =
    find(NameConventions::guild eq guild).firstOrNull()?.data ?: db.defaultNameConventions

suspend fun <T : Any> MongoCollection<T>.findOne(bson: Bson) = find(bson).firstOrNull()
infix fun <T : Any> KProperty1<T, *>.eq(value: Any?): Bson = eq(this.name, value)
infix fun <T : Any> KProperty1<T, *>.all(value: Any?): Bson = all(this.name, value)
infix fun <T : Any> KProperty1<T, *>.contains(value: Any?): Bson = `in`(this.name, value)
