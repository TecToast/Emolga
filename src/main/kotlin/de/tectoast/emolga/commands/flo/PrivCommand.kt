package de.tectoast.emolga.commands.flo

import de.tectoast.emolga.commands.*
import de.tectoast.emolga.utils.Constants
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredMemberFunctions

class PrivCommand : Command("priv", "Executet einen Priv Command", CommandCategory.Flo) {

    private val privCommands by lazy {
        PrivateCommands::class.declaredMemberFunctions.filter { it.returnType.classifier == Unit::class }
            .associateBy { it.name }
    }

    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            add(
                "cmd",
                "Command",
                "Der Command der ausgeführt werden soll",
                ArgumentManagerTemplate.Text.withAutocomplete { s, event ->
                    if (event.user.idLong != Constants.FLOID) return@withAutocomplete emptyList()
                    privCommands.keys.filter { it.startsWith(s, true) }.takeIf { it.size <= 25 }
                })
            add("args", "Argumente", "Die Argumente des Commands", ArgumentManagerTemplate.Text.any(), optional = true)
        }
        slash()
    }

    override suspend fun process(e: GuildCommandEvent) {
        privCommands[e.arguments.getText("cmd")]?.let { method ->
            if (method.parameters.run { isEmpty() || size == 1 }) method.callSuspend(PrivateCommands)
            else method.callSuspend(
                PrivateCommands, PrivateCommandEvent(e.slashCommandEvent!!)
            )
            if (!e.slashCommandEvent!!.isAcknowledged)
                e.reply("Command ausgeführt!", ephemeral = true)
        } ?: e.reply("Command nicht gefunden!", ephemeral = true)
    }
}
