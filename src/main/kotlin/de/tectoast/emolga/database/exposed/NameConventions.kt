package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.utils.json.Emolga
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object NameConventions : Table("nameconventions") {
    val guild = long("guild")
    val german = varchar("german", 50)
    val english = varchar("english", 50)
    val specified = varchar("specified", 50)
    val specifiedenglish = varchar("specifiedenglish", 50)
    val hashypheninname = bool("hashypheninname")

    suspend fun insertDefault(english: String, name: String) {
        newSuspendedTransaction {
            insert {
                it[guild] = 0
                it[german] = name
                it[this.english] = english
                it[specified] = name
            }
        }
    }

    suspend fun checkIfExists(name: String): Boolean {
        return newSuspendedTransaction {
            select(specified eq name).firstOrNull() != null
        }
    }

    suspend fun addName(tlName: String, germanName: String, guildId: Long) {
        newSuspendedTransaction {
            insert {
                it[guild] = guildId
                it[german] = germanName
                val row = select(german eq germanName).first()
                it[english] = row[english]
                it[specified] = tlName
                it[specifiedenglish] = run {
                    val baseName = row[german].split("-").let { base ->
                        if (row[hashypheninname]) base.take(2).joinToString("-") else base[0]
                    }
                    tlName.replace(baseName, row[english].split("-").first())
                }
            }
        }
    }

    fun getTranslation(s: String, guildId: Long): DraftName? {
        val list = mutableListOf<String>()
        Emolga.get.nameconventions[guildId]?.entries?.firstNotNullOfOrNull {
            it.value.find(s)?.let { mr -> mr to it.key }
        }?.also { (mr, key) ->
            list += mr.groupValues[1] + "-" + key
        }
        list += s
        return transaction {
            for (test in list) {
                select((specified eq test or (specifiedenglish eq test)) and (guild eq 0 or (guild eq guildId))).orderBy(
                    guild to SortOrder.DESC
                ).firstOrNull()
                    ?.let {
                        return@transaction DraftName(it[specified], it[german])
                    }

            }
            null
        }
    }
}

data class DraftName(val tlName: String, val official: String) {
    fun toArgumentString() = "$tlName###$official"
}
