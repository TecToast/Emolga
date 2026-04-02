package de.tectoast.emolga.features.league

import de.tectoast.emolga.database.exposed.TransactionCodesRepository
import de.tectoast.emolga.features.*
import de.tectoast.emolga.features.league.Transaction.TransactionFor.Args
import de.tectoast.emolga.features.league.draft.generic.K18n_NoLeagueForGuildFound
import de.tectoast.emolga.utils.dependency
import de.tectoast.emolga.utils.json.mdb
import de.tectoast.emolga.utils.k18n
import kotlin.uuid.ExperimentalUuidApi

object Transaction {
    object Transaction : CommandFeature<NoArgs>(NoArgs(), CommandSpec("transaction", K18n_Transaction.CommandHelp)) {

        @OptIn(ExperimentalUuidApi::class)
        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            val league = mdb.leagueByGuild(iData.gid, iData.user) ?: return iData.reply(
                K18n_NoLeagueForGuildFound,
                ephemeral = true
            )
            val idx = league(iData.user)
            val transactionid = dependency<TransactionCodesRepository>().add(league.leaguename, idx)
            iData.reply(K18n_Transaction.Created(transactionid.toString()), ephemeral = true)
        }
    }

    object TransactionFor : CommandFeature<Args>(::Args, CommandSpec("transactionfor", K18n_Transaction.CommandHelp)) {

        class Args : Arguments() {
            val leaguename by string("leaguename", "leaguename".k18n)
            val idx by int("idx", "idx".k18n)
        }

        @OptIn(ExperimentalUuidApi::class)
        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            val transactionid = dependency<TransactionCodesRepository>().add(e.leaguename, e.idx)
            iData.reply(K18n_Transaction.Created(transactionid.toString()), ephemeral = true)
        }
    }
}
