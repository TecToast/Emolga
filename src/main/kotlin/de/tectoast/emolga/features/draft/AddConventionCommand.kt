package de.tectoast.emolga.features.draft

import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.dconfigurator.impl.TierlistBuilderConfigurator
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object AddConventionCommand : CommandFeature<AddConventionCommand.Args>(
    ::Args,
    CommandSpec(
        "addconvention",
        "Fügt eine Naming Convention von einem Mon hinzu, nur im Kontext von /preparetierlist zu verwenden",
        *TierlistBuilderConfigurator.enabledGuilds.toLongArray()
    )
) {

    class Args : Arguments() {
        var name by string("Name", "Der Name des Pokemons") {
            slashCommand { s, _ ->
                newSuspendedTransaction {
                    NameConventionsDB.run {
                        select { GUILD eq 0 and (GERMAN like "$s%") and (COMMON eq false) }.toList()
                            .takeIf { it.size <= 25 }
                            ?.map { it[GERMAN] }
                    }
                }
            }
        }
    }

    context(InteractionData)
    override suspend fun exec(e: Args) {
        // handled in de/tectoast/emolga/utils/dconfigurator/impl/TierlistBuilderConfigurator.kt
    }
}
