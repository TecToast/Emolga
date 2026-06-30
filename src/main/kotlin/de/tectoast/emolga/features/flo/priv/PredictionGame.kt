package de.tectoast.emolga.features.flo.priv

import de.tectoast.emolga.domain.league.config.repository.LeagueConfigRepository
import de.tectoast.emolga.domain.league.prediction.service.PredictionGameAnalyseTextService
import de.tectoast.emolga.domain.league.prediction.service.PredictionGameService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.NoArgs
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.utils.k18n
import org.koin.core.annotation.Single

@Single
class PrivPredictionGame(lockButtons: LockButtons, print: Print) :
    CommandFeature<NoArgs>(NoArgs(), CommandSpec("predictiongame", "predictiongame".k18n)) {

    override val children = listOf(lockButtons, print)

    @Single
    class LockButtons(
        private val predictionGameService: PredictionGameService,
        private val leagueConfigRepo: LeagueConfigRepository
    ) :
        CommandFeature<LockButtons.Args>(::Args, CommandSpec("lockbuttons", "lockbuttons".k18n)) {
        class Args : Arguments() {
            var leagueName by string("leagueName", "leagueName".k18n)
            var week by int("week", "week".k18n)
            var battleIndex by int("battleIndex", "battleIndex".k18n)
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            predictionGameService.lockButtonsIndividual(e.leagueName, e.week, e.battleIndex)
        }
    }

    @Single
    class Print(private val predictionGameAnalyse: PredictionGameAnalyseTextService) :
        CommandFeature<OnlyLeagueNameArgs>(::OnlyLeagueNameArgs, CommandSpec("print", "print".k18n)) {
        context(iData: InteractionData)
        override suspend fun exec(e: OnlyLeagueNameArgs) {
            iData.replyRaw(predictionGameAnalyse.getFullResultsSummary(e.leagueName))
        }
    }

    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {

    }
}

class OnlyLeagueNameArgs : Arguments() {
    var leagueName by string("leagueName", "leagueName".k18n)
}
