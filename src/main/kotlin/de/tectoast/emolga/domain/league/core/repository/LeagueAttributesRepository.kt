package de.tectoast.emolga.domain.league.core.repository

import de.tectoast.emolga.domain.league.core.model.LeagueAttribute
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializerOrNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single


@Single
class LeagueAttributesRepository(private val db: R2dbcDatabase) {
    private val json = Json

    @OptIn(InternalSerializationApi::class)
    suspend fun <T : Any> get(leagueName: String, attribute: LeagueAttribute<T>): T? = suspendTransaction(db) {
        val stringRepresentation = LeagueAttributesTable.select(LeagueAttributesTable.value)
            .where { (LeagueAttributesTable.league eq leagueName) and (LeagueAttributesTable.attribute eq attribute.name) }
            .firstOrNull()?.get(LeagueAttributesTable.value) ?: return@suspendTransaction null
        val serializer = attribute.clazz.serializerOrNull() ?: error("No serializer found for ${attribute.clazz}")
        json.decodeFromString(serializer, stringRepresentation)
    }

    suspend fun <T : Any> getOrDefault(leagueName: String, attribute: LeagueAttribute<T>): T =
        get(leagueName, attribute) ?: attribute.defaultValue ?: error("No default value found for ${attribute.name}")

    @OptIn(InternalSerializationApi::class)
    suspend fun <T : Any> set(leagueName: String, attribute: LeagueAttribute<T>, value: T) = suspendTransaction(db) {
        val stringRepresentation = json.encodeToString(
            attribute.clazz.serializerOrNull() ?: error("No serializer found for ${attribute.clazz}"), value
        )
        LeagueAttributesTable.upsert {
            it[LeagueAttributesTable.league] = leagueName
            it[LeagueAttributesTable.attribute] = attribute.name
            it[LeagueAttributesTable.value] = stringRepresentation
        }
    }

    suspend fun delete(leagueName: String, attribute: LeagueAttribute<*>) = suspendTransaction(db) {
        LeagueAttributesTable.deleteWhere { (LeagueAttributesTable.league eq leagueName) and (LeagueAttributesTable.attribute eq attribute.name) }
    }
}

object LeagueAttributesTable : Table("league_attributes") {
    val league = text("league").referencesLeagueName()
    val attribute = text("attribute")
    val value = text("value")
}