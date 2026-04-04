package de.tectoast.emolga.database

import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import java.sql.SQLIntegrityConstraintViolationException


suspend fun <T> dbTransaction(statement: suspend Transaction.() -> T) =
    suspendTransaction { statement() }


suspend fun dbTransactionWithUniqueHandler(
    statement: suspend Transaction.() -> Unit,
    uniqueHandler: suspend () -> Unit
) {
    try {
        dbTransaction { statement() }
    } catch (e: ExposedSQLException) {
        if (e.cause !is SQLIntegrityConstraintViolationException) throw e
        uniqueHandler()
    }
}
