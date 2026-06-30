package de.tectoast.emolga.domain.league.showdownnames.repository

import de.tectoast.emolga.domain.league.showdownnames.model.ShowdownUserID
import de.tectoast.emolga.utils.toShowdownUserId
import kotlinx.coroutines.flow.associate
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.vendors.currentDialect
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single


@Single
class SDNamesRepository(
    private val db: R2dbcDatabase
) {

    /**
     * Gets the discord user ids by the showdown usernames (see [toShowdownUserId])
     * @param userNames the showdown names to look for
     * @return the corresponding user ids
     */
    suspend fun getIDsByUsernames(userNames: Collection<ShowdownUserID>) = suspendTransaction(db) {
        val result = SDNamesTable.selectAll().where { SDNamesTable.name inList userNames }
            .associate { it[SDNamesTable.name] to it[SDNamesTable.id] }
        userNames.map { result[it] }
    }


    suspend fun getCurrentOwner(showdownUserID: ShowdownUserID) = suspendTransaction(db) {
        SDNamesTable.select(SDNamesTable.id).where { SDNamesTable.name eq showdownUserID }.firstOrNull()
            ?.get(SDNamesTable.id)
    }

    suspend fun tryInsertName(userId: ShowdownUserID, owner: Long) {
        return suspendTransaction(db) {
            SDNamesTable.insertIgnore {
                it[name] = userId
                it[id] = owner
            }
        }
    }

    /**
     * Set the owner of the sd name to the given user, regardless of a potential former owner
     * @param username the sd name (already as username)
     * @param id the discord user id
     */
    suspend fun setOwnerOfName(username: ShowdownUserID, id: Long) {
        suspendTransaction(db) {
            SDNamesTable.upsert {
                it[name] = username
                it[this.id] = id
            }
        }
    }
}


class ShowdownUserIdColumnType : ColumnType<ShowdownUserID>() {
    override fun sqlType(): String = currentDialect.dataTypeProvider.textType()
    override fun valueFromDB(value: Any): ShowdownUserID = when (value) {
        is String -> ShowdownUserID(value)
        else -> error("Unexpected value of type Int: $value of ${value::class.qualifiedName}")
    }

    override fun notNullValueToDB(value: ShowdownUserID): Any {
        return value.value
    }
}

object SDNamesTable : Table("sdnames") {
    val name = showdownUserIDColumn("name")
    val id = long("id")

    override val primaryKey = PrimaryKey(name)
}

fun Table.showdownUserIDColumn(name: String) = registerColumn(name, ShowdownUserIdColumnType())