package de.tectoast.emolga.domain.league.transaction.service

import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.transaction.repository.TransactionCodesRepository
import de.tectoast.emolga.features.league.draft.generic.K18n_NoLeagueForGuildFound
import de.tectoast.emolga.utils.CalcResult
import de.tectoast.emolga.utils.error
import de.tectoast.emolga.utils.success
import org.koin.core.annotation.Single

@Single
class TransactionStartService(
    private val leagueCoreRepo: LeagueCoreRepository,
    private val transactionCodesRepo: TransactionCodesRepository
) {
    suspend fun startTransaction(guild: Long, user: Long): CalcResult<String> {
        val (leagueName, idx) = leagueCoreRepo.getLeagueNameAndIdxByGuildUser(guild, user)
            ?: return K18n_NoLeagueForGuildFound.error()
        val transactionId = transactionCodesRepo.add(leagueName, idx)
        return transactionId.toString().success()
    }
}