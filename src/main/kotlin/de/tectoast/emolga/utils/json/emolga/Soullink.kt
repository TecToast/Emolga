package de.tectoast.emolga.utils.json.emolga

import com.mongodb.client.model.Filters.eq
import de.tectoast.emolga.utils.json.db
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
class Soullink(
    @SerialName("_id")
    @Contextual
    val id: ObjectId = ObjectId(),
    val order: MutableList<String>, val mons: MutableMap<String, MutableMap<String, String>>
) {
    suspend fun save() = db.soullink.replaceOne(eq(id), this)
}
