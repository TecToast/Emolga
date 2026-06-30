package de.tectoast.emolga.features.league

import de.tectoast.emolga.database.exposed.*
import de.tectoast.emolga.domain.guildspecific.nominate.service.NominateService
import de.tectoast.emolga.domain.statestore.service.NominateStateStoreHandler
import de.tectoast.emolga.domain.statestore.service.StateStoreDispatcher
import de.tectoast.emolga.features.*
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.ButtonSpec
import de.tectoast.emolga.features.system.types.ButtonFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.json.*
import org.koin.core.annotation.Single


@Single(binds = [ListenerProvider::class])
class NominateButton(private val stateStore: StateStoreDispatcher, private val nominateService: NominateService) :
    ButtonFeature<NominateButton.Args>(::Args, ButtonSpec("nominate")) {

    override val label = EmptyMessage

    init {
        registerDMListener("!nominate") { e ->
            nominateService.handleStart(
                e.author.idLong,
                e.message.contentRaw.split(" ").getOrNull(1)?.toIntOrNull(),
                this
            )
        }
    }

    enum class Mode {
        NOMINATE, UNNOMINATE, FINISH, FINISH_NOW, CANCEL
    }

    class Args : Arguments() {
        var mode by enumBasic<Mode>()
        var data by showdownIDArg()
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        stateStore.process<_, NominateStateStoreHandler>(iData.user) {
            when (e.mode) {
                Mode.NOMINATE -> {
                    nominate(e.data)
                    render()
                }

                Mode.UNNOMINATE -> {
                    unnominate(e.data)
                    render()
                }

                Mode.FINISH, Mode.FINISH_NOW -> {
                    finish(now = e.mode == Mode.FINISH_NOW)
                }

                Mode.CANCEL -> {
                    render()
                }
            }
        }

    }
}
