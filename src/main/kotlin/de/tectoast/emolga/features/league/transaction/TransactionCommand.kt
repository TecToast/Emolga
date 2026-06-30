package de.tectoast.emolga.features.league.transaction

import de.tectoast.emolga.domain.league.transaction.service.TransactionStartService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.league.K18n_Transaction
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.NoArgs
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.onFailureReply
import org.koin.core.annotation.Single
import kotlin.uuid.ExperimentalUuidApi

@Single(binds = [ListenerProvider::class])
class TransactionCommand(private val service: TransactionStartService) :
    CommandFeature<NoArgs>(NoArgs(), CommandSpec("transaction", K18n_Transaction.CommandHelp)) {

    @OptIn(ExperimentalUuidApi::class)
    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        val transactionId = service.startTransaction(iData.gid, iData.user).onFailureReply() ?: return
        iData.reply(K18n_Transaction.Created(transactionId), ephemeral = true)
    }
}