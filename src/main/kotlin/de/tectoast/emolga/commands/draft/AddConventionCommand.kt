package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.utils.dconfigurator.impl.TierlistBuilderConfigurator
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object AddConventionCommand : Command(
    "addconvention",
    "Fügt eine Naming Convention von einem Mon hinzu, nur im Kontext von /preparetierlist zu verwenden",
    CommandCategory.Draft
) {

    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            add("name", "Name", "Der Name des Pokemons", ArgumentManagerTemplate.Text.withAutocomplete { s, _ ->
                transaction {
                    NameConventionsDB.run {
                        select { GUILD eq 0 and (GERMAN like "$s%") }.toList().takeIf { it.size <= 25 }
                            ?.map { it[GERMAN] }
                    }
                }
            })
        }
        slash(true, *TierlistBuilderConfigurator.enabledGuilds.toLongArray())
    }

    override suspend fun process(e: GuildCommandEvent) {

    }
}
