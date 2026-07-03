package de.tectoast.emolga.features.league.transaction

import de.tectoast.emolga.domain.league.transaction.repository.TransactionCodesRepository
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.league.K18n_Transaction
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.k18n
import org.koin.core.annotation.Single
import kotlin.uuid.ExperimentalUuidApi

@Single(binds = [ListenerProvider::class])
class TransactionForCommand(private val repo: TransactionCodesRepository) :
    CommandFeature<TransactionForCommand.Args>(::Args, CommandSpec("transactionfor", K18n_Transaction.CommandHelp)) {

    class Args : Arguments() {
        val leaguename by string("leaguename", "leaguename".k18n)
        val idx by int("idx", "idx".k18n)
    }

    @OptIn(ExperimentalUuidApi::class)
    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        val transactionId = repo.add(e.leaguename, e.idx)
        iData.reply(K18n_Transaction.Created(botConstants.webBaseUrl, transactionId.toString()), ephemeral = true)
    }
}