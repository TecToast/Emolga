package de.tectoast.emolga.features.flo

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.database.exposed.CmdManager
import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.OneTimeCache
import de.tectoast.emolga.utils.filterContainsIgnoreCase
import de.tectoast.emolga.utils.k18n

object CmdManageCommand : CommandFeature<NoArgs>(NoArgs(), CommandSpec("cmdmanage", "CmdManage".k18n)) {

    object GuildGroup : CommandFeature<GuildGroup.Args>(
        ::Args, CommandSpec("guildgroup", "GuildGroup verwalten".k18n)
    ) {
        class Args : Arguments() {
            var guildId by long("guildid", "Die ID des Servers".k18n)
            var group by string("group", "Der Name der Gruppe".k18n)
            var action by enumBasic<AddRemove>("action", "Hinzufügen oder Entfernen".k18n) {
                default = AddRemove.ADD
            }
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            iData.deferReply(true)
            CmdManager.modifyGuildGroup(e.guildId, e.group, e.action)
            iData.done()
        }
    }

    object GuildCommand :
        CommandFeature<GuildCommand.Args>(::Args, CommandSpec("guildcommand", "GuildCommand verwalten".k18n)) {
        class Args : Arguments() {
            var guildId by long("guildid", "Die ID des Servers".k18n)
            var command by commandArg()
            var action by enumBasic<AddRemove>("action", "Hinzufügen oder Entfernen".k18n) {
                default = AddRemove.ADD
            }
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            iData.deferReply(true)
            CmdManager.modifyGuildCommand(e.guildId, e.command, e.action)
            iData.done()
        }
    }

    object GroupCommand :
        CommandFeature<GroupCommand.Args>(::Args, CommandSpec("groupcommand", "GroupCommand verwalten".k18n)) {
        class Args : Arguments() {
            var group by string("group", "Der Name der Gruppe".k18n)
            var command by commandArg()
            var action by enumBasic<AddRemove>("action", "Hinzufügen oder Entfernen".k18n) {
                default = AddRemove.ADD
            }
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            iData.deferReply(true)
            CmdManager.modifyGroupCommand(e.group, e.command, e.action)
            iData.done()
        }
    }

    private val featureNames = OneTimeCache {
        EmolgaMain.featureManager().registeredFeatureList.map { it.spec.name }
    }

    private fun Arguments.commandArg() = string("command", "Der Name des Commands".k18n) {
        slashCommand { string, _ -> featureNames().filterContainsIgnoreCase(string) }
    }


    context(data: InteractionData)
    override suspend fun exec(e: NoArgs) {

    }
}

enum class AddRemove {
    ADD, REMOVE;

    fun add() = this == ADD
}