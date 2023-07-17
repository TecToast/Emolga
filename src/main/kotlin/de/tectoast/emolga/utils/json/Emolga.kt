package de.tectoast.emolga.utils.json

import de.tectoast.emolga.commands.condAppend
import de.tectoast.emolga.commands.ifTrue
import de.tectoast.emolga.utils.json.emolga.ASLS11
import de.tectoast.emolga.utils.json.emolga.Soullink
import de.tectoast.emolga.utils.json.emolga.customcommand.CCData
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.NDS
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
class Emolga(
    val config: Config,
    val statistics: Statistics,
    val signups: MutableMap<Long, LigaStartData> = mutableMapOf(),
    val drafts: Map<String, League>,
    val soullink: Soullink,
    val emolgachannel: MutableMap<Long, MutableList<Long>>,
    val cooldowns: MutableMap<Long, MutableMap<String, Long>>,
    val moderatorroles: Map<Long, Long>,
    val movefilter: Map<String, List<String>>,
    val mutedroles: Map<Long, Long>,
    val customcommands: MutableMap<String, CCData>,
    val configuration: MutableMap<Long, MutableMap<String, MutableMap<String, Int>>>,
    val asls11nametoid: List<Long>,
    val nameconventions: MutableMap<Long, MutableMap<String, @Serializable(RegexSerializer::class) Regex>> = mutableMapOf()
) {

    val asls11: ASLS11 get() = error("ASLS11 is not available atm!")
    val defaultNameConventions: Map<String, Regex> by lazy { nameconventions[0]!! }
    fun league(name: String) = drafts[name]!!
    fun nds() = drafts["NDS"]!! as NDS

    fun leagueByGuild(gid: Long, vararg uids: Long) = drafts.values.firstOrNull {
        it.guild == gid && it.table.containsAll(uids.toList())
    }

    companion object {
        lateinit var get: Emolga
    }
}

@Serializable
class Config(val teamgraphicShinyOdds: Int)

@Serializable
class Statistics(var drampaCounter: Int)

@Serializable
data class LigaStartData(
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
