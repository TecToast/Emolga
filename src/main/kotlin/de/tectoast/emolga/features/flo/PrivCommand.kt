package de.tectoast.emolga.features.flo


import de.tectoast.emolga.features.*
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredMemberFunctions

object PrivCommand : CommandFeature<PrivCommand.Args>(::Args, CommandSpec("priv", "Führt einen privaten Command aus")) {
    init {
        restrict(flo)
    }

    class Args : Arguments() {
        var cmd by fromListCommand("cmd", "cmd", privCommands.keys, useContainsAutoComplete = true)
        var arguments by list("args", "args", 20, 0, startAt = 0)
    }

    private val privCommands by lazy {
        PrivateCommands::class.declaredMemberFunctions.filter { it.returnType.classifier == Unit::class }
            .associateBy { it.name }
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        privCommands[e.cmd]?.let { method ->
            if (method.parameters.run { size <= 2 }) method.callSuspend(PrivateCommands, iData)
            else method.callSuspend(
                PrivateCommands, iData, PrivateData(e.arguments)
            )
            if (!iData.replied)
                iData.reply("Command ausgeführt!", ephemeral = true)
        } ?: iData.reply("Command nicht gefunden!", ephemeral = true)
    }
}
