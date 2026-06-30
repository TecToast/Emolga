package de.tectoast.emolga.domain.league.tierlist.repository

import de.tectoast.emolga.di.CleanupTask
import de.tectoast.emolga.domain.league.tierlist.model.setup.TierlistWizardState
import de.tectoast.emolga.utils.jsonb
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Single
class TierlistSetupRepository(private val db: R2dbcDatabase, private val clock: Clock) : CleanupTask {
    override suspend fun cleanup(now: Instant) {
        suspendTransaction(db) {
            TierlistSetupTable.deleteWhere { TierlistSetupTable.timestamp lessEq now - 7.days }
        }
    }

    suspend fun create() = suspendTransaction(db) {
        val uuid = Uuid.random()
        TierlistSetupTable.insert {
            it[TierlistSetupTable.code] = uuid
            it[TierlistSetupTable.timestamp] = clock.now()
            it[TierlistSetupTable.state] = TierlistWizardState.AwaitingPokemonList
        }
        uuid
    }

    suspend fun getState(uuid: Uuid) = suspendTransaction(db) {
        TierlistSetupTable.select(TierlistSetupTable.state).where { TierlistSetupTable.code eq uuid }
            .map { it[TierlistSetupTable.state] }
            .singleOrNull()
    }

    suspend fun updateState(uuid: Uuid, state: TierlistWizardState) = suspendTransaction(db) {
        TierlistSetupTable.update({ TierlistSetupTable.code eq uuid }) {
            it[TierlistSetupTable.state] = state
            it[TierlistSetupTable.timestamp] = clock.now()
        }
    }

    suspend fun deleteState(uuid: Uuid) = suspendTransaction(db) {
        TierlistSetupTable.deleteWhere { TierlistSetupTable.code eq uuid }
    }
}

object TierlistSetupTable : Table("tierlist_setup") {
    val code = uuid("uuid")
    val timestamp = timestamp("timestamp")
    val state = jsonb<TierlistWizardState>("state")

    override val primaryKey = PrimaryKey(code)
}