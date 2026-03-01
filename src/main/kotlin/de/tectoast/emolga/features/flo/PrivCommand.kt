package de.tectoast.emolga.features.flo


import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.k18n
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.contextParameters
import kotlin.reflect.full.declaredMemberFunctions

object PrivCommand :
    CommandFeature<PrivCommand.Args>(::Args, CommandSpec("priv", "Führt einen privaten Command aus".k18n)) {
    init {
        restrict(flo)
    }

    class Args : Arguments() {
        var cmd by fromListCommand("cmd", "cmd".k18n, privCommands.keys, useContainsAutoComplete = true)
        var arguments by list("args", "args".k18n, 20, 0, startAt = 0)
    }

    @OptIn(ExperimentalContextParameters::class)
    private val privCommands by lazy {
        PrivateCommands::class.declaredMemberFunctions.filter { it.returnType.classifier == Unit::class && it.contextParameters.isNotEmpty() }
            .associateBy { it.name }
    }

    val scope = createCoroutineScope("PrivCommand")

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        privCommands[e.cmd]?.let { method ->
            withTimeoutOrNull(2000) {
                scope.launch {
                    if (method.parameters.run { size <= 2 }) method.callSuspend(PrivateCommands, iData)
                    else method.callSuspend(
                        PrivateCommands, iData, PrivateData(e.arguments)
                    )
                }.join()
            }
            if (iData.replied || iData.deferred) return
            iData.reply("Command ausgeführt!", ephemeral = true)
        } ?: iData.reply("Command nicht gefunden!", ephemeral = true)
    }
}
