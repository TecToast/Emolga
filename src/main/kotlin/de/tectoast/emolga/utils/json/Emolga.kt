package de.tectoast.emolga.utils.json

import de.tectoast.emolga.commands.condAppend
import de.tectoast.emolga.commands.ifTrue
import de.tectoast.emolga.utils.json.emolga.ASLS11
import de.tectoast.emolga.utils.json.emolga.Soullink
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.NDS
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bson.conversions.Bson
import org.litote.kmongo.Id
import org.litote.kmongo.all
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.coroutine.updateOne
import org.litote.kmongo.eq
import org.litote.kmongo.newId
import org.litote.kmongo.reactivestreams.KMongo

val db: MongoEmolga = MongoEmolga()

class MongoEmolga(

) {
    private val DB_URL = "mongodb://floritemp.fritz.box:27017/"
    val db = KMongo.createClient(DB_URL).coroutine.getDatabase("sixvspokeworld")

    val config by lazy { db.getCollection<Config>() }
    val statistics by lazy { db.getCollection<Statistics>() }
    val signups by lazy { db.getCollection<LigaStartData>("signups") }
    val drafts by lazy { db.getCollection<League>() }
    val soullink by lazy { db.getCollection<Soullink>() }
    val emolgachannel by lazy { db.getCollection<EmolgaChannelConfig>() }
    val cooldowns by lazy { db.getCollection<Cooldown>() }
    val movefilter: Map<String, List<String>> = error("Movefilter is not available atm!")
    val configuration by lazy { db.getCollection<Configuration>() }
    val nameconventions by lazy { db.getCollection<NameConventions>() }

    val asls11: ASLS11 get() = error("ASLS11 is not available atm!")
    val asls11nametoid: List<Long> get() = error("ASLS11 is not available atm!")
    val defaultNameConventions: Map<String, Regex> by lazy {
        runBlocking {
            nameconventions.find(NameConventions::guild eq 0).first()!!
        }.data
    }

    suspend fun league(name: String) = drafts.findOne(League::name eq name)!!
    suspend fun nds() = league("NDS") as NDS

    suspend fun leagueByGuild(gid: Long, vararg uids: Long) =
        drafts.findOne(League::guild eq gid, League::table all uids.toList())
}

@Serializable
data class Config(val teamgraphicShinyOdds: Int)

@Serializable
data class Statistics(var drampaCounter: Int)

data class Configuration(
    val guild: Long,
    @SerialName("_id")
    @Contextual
    val id: Id<Configuration> = newId(),
    val data: MutableMap<String, MutableMap<String, Int>> = mutableMapOf()
)

data class EmolgaChannelConfig(
    val guild: Long,
    val channels: List<Long>
)

data class Cooldown(
    val guild: Long,
    val user: Long,
    val timestamp: Long
)

data class NameConventions(
    val guild: Long,
    val data: MutableMap<String, @Serializable(with = RegexSerializer::class) Regex> = mutableMapOf()
)

@Serializable
data class LigaStartData(
    @SerialName("_id")
    @Contextual
    val id: Id<LigaStartData> = newId(),
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
    fun conferenceSelectMenus(uid: Long, initial: Boolean) = StringSelectMenu(
        "cselect;${initial.ifTrue("initial")}:$uid",
        options = conferences.map { SelectOption(it, it) })

    suspend fun save() = db.signups.updateOne(this)

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
    } + ":\n" +
            "Teamname: **$teamname**\n" +
            "Showdown-Name: **$sdname**"
}

object RegexSerializer : KSerializer<Regex> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Regex", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Regex = Regex(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: Regex) = encoder.encodeString(value.pattern)
}

suspend fun <T : Any> CoroutineCollection<T>.only() = find().first()!!
suspend fun <T : Any> CoroutineCollection<T>.updateOnly(update: Bson) = updateOne("{}", update)

@JvmName("getLigaStartData")
suspend fun CoroutineCollection<LigaStartData>.get(guild: Long) = find(LigaStartData::guild eq guild).first()

@JvmName("getNameConventions")
suspend fun CoroutineCollection<NameConventions>.get(guild: Long) =
    find(NameConventions::guild eq guild).first()?.data ?: db.defaultNameConventions
