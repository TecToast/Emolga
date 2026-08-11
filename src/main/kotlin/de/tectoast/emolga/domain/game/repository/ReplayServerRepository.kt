package de.tectoast.emolga.domain.game.repository

import de.tectoast.emolga.di.StartupTask
import de.tectoast.emolga.domain.game.model.analysis.ReplayServerData
import de.tectoast.emolga.domain.game.model.analysis.ReplayServerMode
import de.tectoast.emolga.utils.suspendTransaction
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.associate
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.koin.core.annotation.Single

@Single
class ReplayServerRepository(private val db: R2dbcDatabase, baseScope: CoroutineScope) : StartupTask {
    private val scope = baseScope + CoroutineName("ReplayServerRepository")
    val modeByServer = scope.async(start = CoroutineStart.LAZY) {
        suspendTransaction(db, ReplayServerTable) {
            selectAll().associate { row -> row[url] to ReplayServerData(row[mode], row[identifier]) }
        }
    }

    override suspend fun onStartup() {
        modeByServer.start()
    }
}

object ReplayServerTable : Table("replay_server") {
    val identifier = text("identifier")
    val mode = enumerationByName<ReplayServerMode>("mode", 64)
    val url = text("url")

    override val primaryKey = PrimaryKey(identifier)
}