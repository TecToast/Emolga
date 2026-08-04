package de.tectoast.emolga.features.league.prediction

import de.tectoast.emolga.domain.league.prediction.model.PredictionActionSource
import de.tectoast.emolga.domain.league.prediction.service.PredictionGameService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.league.K18n_PredictionGame
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.ButtonSpec
import de.tectoast.emolga.features.system.types.ButtonFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class PredictionGameVoteButton(private val service: PredictionGameService) :
    ButtonFeature<PredictionGameVoteButton.Args>(::Args, ButtonSpec("predictiongame").apply { aliases += "tipgame" }) {
    class Args : Arguments() {
        var leaguename by string("leaguename")
        var week by int("week")
        var battleIndex by int("battleIndex")
        var idx by int("idx")
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.ephemeralDefault()
        iData.deferEdit()
        val result = service.addVote(
            iData.user,
            e.leaguename,
            e.week,
            e.battleIndex,
            e.idx,
            PredictionActionSource.WithInteractionData(iData)
        )
        if (result) {
            iData.reply(K18n_PredictionGame.PredictionSaved)
        } else {
            iData.reply(K18n_PredictionGame.PredictionGameMissing)
        }
    }

}