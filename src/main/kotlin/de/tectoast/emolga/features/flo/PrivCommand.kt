package de.tectoast.emolga.features.flo


import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.commands.PrivateCommandEvent
import de.tectoast.emolga.commands.PrivateCommands
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredMemberFunctions

object PrivCommand : CommandFeature<PrivCommand.Args>(::Args, CommandSpec("priv", "Führt einen privaten Command aus")) {
    init {
        restrict(flo)
    }

    class Args : Arguments() {
        var cmd by fromList("cmd", "cmd", privCommands.keys.toList())
        var arguments by string("args", "args") {
            default = ""
        }
    }

    private val privCommands by lazy {
        PrivateCommands::class.declaredMemberFunctions.filter { it.returnType.classifier == Unit::class }
            .associateBy { it.name }
    }

    context(InteractionData) override suspend fun exec(e: Args) = slashEvent {
        privCommands[e.cmd]?.let { method ->
            if (method.parameters.run { isEmpty() || size == 1 }) method.callSuspend(PrivateCommands)
            else method.callSuspend(
                PrivateCommands, PrivateCommandEvent(this)
            )
            if (!isAcknowledged)
                reply("Command ausgeführt!", ephemeral = true)
        } ?: reply("Command nicht gefunden!", ephemeral = true)
    }
}
