package de.tectoast.emolga.utils.json

import de.tectoast.emolga.utils.json.emolga.ASLS11
import de.tectoast.emolga.utils.json.emolga.Soullink
import de.tectoast.emolga.utils.json.emolga.customcommand.CCData
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.NDS
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

object RegexSerializer : KSerializer<Regex> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Regex", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Regex = Regex(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: Regex) = encoder.encodeString(value.pattern)
}
