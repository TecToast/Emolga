package de.tectoast.emolga.features.draft

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.selectAll

object AddConventionCommand : CommandFeature<AddConventionCommand.Args>(
    ::Args,
    CommandSpec(
        "addconvention",
        K18n_AddConvention.Help,
    )
) {

    class Args : Arguments() {
        var name by string("Name", K18n_AddConvention.ArgPokemon) {
            slashCommand { s, _ ->
                dbTransaction {
                    NameConventionsDB.run {
                        selectAll().where { GUILD eq 0 and (GERMAN like "$s%") and (COMMON eq false) }.toList()
                            .takeIf { it.size <= 25 }
                            ?.map { it[GERMAN] }
                    }
                }
            }
        }
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        // handled in de/tectoast/emolga/utils/dconfigurator/impl/TierlistBuilderConfigurator.kt
        iData.acknowledged.complete(Unit)
    }
}
