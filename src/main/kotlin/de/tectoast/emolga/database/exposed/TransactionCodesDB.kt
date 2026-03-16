package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object TransactionCodesDB : Table("transactioncodes") {
    val CODE = uuid("code")
    val LEAGUENAME = varchar("leaguename", 32)
    val IDX = integer("idx")

    override val primaryKey = PrimaryKey(CODE)

    suspend fun getDataByCode(transactionid: String) = dbTransaction {
        val uuid = Uuid.parseHexDashOrNull(transactionid) ?: return@dbTransaction null
        selectAll().where { CODE eq uuid }.map { it[LEAGUENAME] to it[IDX] }.singleOrNull()
    }

    suspend fun add(leaguename: String, idx: Int): Uuid {
        val code: Uuid = Uuid.generateV7()
        dbTransaction {
            insert {
                it[CODE] = code
                it[LEAGUENAME] = leaguename
                it[IDX] = idx
            }
        }
        return code
    }


    suspend fun deleteCode(transactionid: String) {
        val uuid = Uuid.parseHexDashOrNull(transactionid) ?: return
        dbTransaction {
            deleteWhere { CODE eq uuid }
        }
    }

}