package de.tectoast.emolga.utils

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.json.jsonb
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction


val exposedJson = Json {
    encodeDefaults = false
}

inline fun <reified T : Any> Table.jsonb(name: String) = jsonb<T>(name, exposedJson)

fun <T : Any> arrayAgg(expr: Expression<T>, columnType: ColumnType<T>) =
    CustomFunction<List<T>>("array_agg", ArrayColumnType(columnType), expr)

context(t: Table)
fun <T : Any, S : T, C : Column<S>> C.referencesCascade(ref: Column<T>): C = with(t) {
    references(ref, onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE)
}

suspend fun <T : Table, R> suspendTransaction(db: R2dbcDatabase, primaryTable: T, block: suspend T.() -> R) =
    suspendTransaction(db) {
        primaryTable.block()
    }