package de.tectoast.emolga.features.flo

import de.tectoast.emolga.database.exposed.FlorixControlRepository
import de.tectoast.emolga.features.*
import de.tectoast.emolga.features.flo.FlorixButton.Action.*
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class FlorixButton(val repository: FlorixControlRepository) :
    ButtonFeature<FlorixButton.Args>(::Args, ButtonSpec("florix")) {
    class Args : Arguments() {
        var pc by string()
        var action by enumBasic<Action>()
    }

    enum class Action {
        START, STOP, STATUS, POWEROFF;
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        val data = repository.getByName(e.pc) ?: return iData.reply("Ungültiger PC! (${e.pc})")
        val on = data.isOn()
        iData.ephemeralDefault()
        when (e.action) {
            START -> {
                if (on) return iData.reply("Der Server ist bereits an!")
                data.startServer()
                iData.reply("Der Server wurde gestartet!")
            }

            STATUS -> iData.reply("Der Server ist ${if (on) "an" else "aus"}!")

            STOP -> {
                if (!on) return iData.reply("Der Server ist bereits aus!")
                data.stopServer()
                iData.reply("Der Server wurde gestoppt!")
            }

            POWEROFF -> {
                if (!on) return iData.reply("Der Server ist bereits aus!")
                data.powerOff()
                iData.reply("Der Server wurde poweroffed!")
            }

        }
    }
}
