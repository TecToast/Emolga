package de.tectoast.emolga.domain.scheduling.repeat.service.league

import de.tectoast.emolga.domain.league.config.model.LeagueConfig
import de.tectoast.emolga.domain.league.transaction.service.TransactionExecutionService
import de.tectoast.emolga.domain.scheduling.repeat.model.RepeatTask
import de.tectoast.emolga.domain.scheduling.repeat.model.RepeatTaskType
import de.tectoast.emolga.domain.scheduling.repeat.service.RepeatTaskScheduler
import org.koin.core.annotation.Single

@Single
class TransactionRepeatTask(private val service: TransactionExecutionService) : LeagueRepeatTask {
    override suspend fun setup(
        scheduler: RepeatTaskScheduler, leagueName: String, config: LeagueConfig
    ) {
        val transactionConfig = config.transaction ?: return
        val lastDocInsert = transactionConfig.lastDocInsert ?: return
        scheduler.schedule(
            RepeatTask(
                RepeatTaskType.TransactionDocInsert(leagueName),
                lastDocInsert,
                transactionConfig.maxWeek,
                transactionConfig.interval
            )
        ) {
            service.registerTransactions(leagueName, it, onlyIndices = null)
        }
    }
}

