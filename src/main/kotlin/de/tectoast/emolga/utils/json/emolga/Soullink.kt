package de.tectoast.emolga.utils.json.emolga

import de.tectoast.emolga.utils.json.db
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.litote.kmongo.Id
import org.litote.kmongo.coroutine.updateOne

@Serializable
class Soullink(
    @SerialName("_id")
    @Contextual
    val id: Id<Soullink>,
    val order: MutableList<String>, val mons: MutableMap<String, MutableMap<String, String>>
) {
    suspend fun save() = db.soullink.updateOne(this)
}
