package de.tectoast.emolga.features.flo

import de.tectoast.emolga.database.exposed.CommandManagementRepository
import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.filterContainsIgnoreCase
import de.tectoast.emolga.utils.k18n
import org.koin.core.annotation.Single
import org.koin.core.component.inject

@Single(binds = [ListenerProvider::class])
class CmdManageCommand(
    guildGroup: GuildGroup,
    guildCommand: GuildCommand,
    groupCommand: GroupCommand,
    update: Update
) : CommandFeature<NoArgs>(NoArgs(), CommandSpec("cmdmanage", "CmdManage".k18n)) {
    override val children = listOf(guildGroup, guildCommand, groupCommand, update)

    @Single(binds = [ListenerProvider::class])
    class GuildGroup(val cmdRegistry: CommandRegistryService) : CommandFeature<GuildGroup.Args>(
        ::Args, CommandSpec("guildgroup", "GuildGroup verwalten".k18n)
    ) {
        class Args : Arguments() {
            val cmdRepo by inject<CommandManagementRepository>()

            var guildId by long("guildid", "Die ID des Servers".k18n)
            var group by string("group", "Der Name der Gruppe".k18n) {
                slashCommand { s, _ ->
                    cmdRepo.getGroups().filterContainsIgnoreCase(s)
                }
            }
            var action by enumBasic<AddRemove>("action", "Hinzufügen oder Entfernen".k18n) {
                default = AddRemove.ADD
            }
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            iData.deferReply(true)
            cmdRegistry.modifyGuildGroup(e.guildId, e.group, e.action)
            iData.done()
        }
    }

    @Single(binds = [ListenerProvider::class])
    class GuildCommand(val cmdRegistry: CommandRegistryService) :
        CommandFeature<GuildCommand.Args>(::Args, CommandSpec("guildcommand", "GuildCommand verwalten".k18n)) {
        class Args : Arguments() {
            val registry by inject<FeatureRegistry>()

            var guildId by long("guildid", "Die ID des Servers".k18n)
            var command by this.string("command", "Der Name des Commands".k18n) {
                slashCommand { string, _ -> registry.featureNames.filterContainsIgnoreCase(string) }
            }
            var action by enumBasic<AddRemove>("action", "Hinzufügen oder Entfernen".k18n) {
                default = AddRemove.ADD
            }
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            iData.deferReply(true)
            cmdRegistry.modifyGuildCommand(e.guildId, e.command, e.action)
            iData.done()
        }
    }

    @Single(binds = [ListenerProvider::class])
    class GroupCommand(val cmdRegistry: CommandRegistryService) :
        CommandFeature<GroupCommand.Args>(::Args, CommandSpec("groupcommand", "GroupCommand verwalten".k18n)) {
        class Args : Arguments() {
            val cmdRepo by inject<CommandManagementRepository>()
            val registry by inject<FeatureRegistry>()

            var group by string("group", "Der Name der Gruppe".k18n) {
                slashCommand { s, _ ->
                    cmdRepo.getGroups().filterContainsIgnoreCase(s)
                }
            }
            var command by this.string("command", "Der Name des Commands".k18n) {
                slashCommand { string, _ -> registry.featureNames.filterContainsIgnoreCase(string) }
            }
            var action by enumBasic<AddRemove>("action", "Hinzufügen oder Entfernen".k18n) {
                default = AddRemove.ADD
            }
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            iData.deferReply(true)
            cmdRegistry.modifyGroupCommand(e.group, e.command, e.action)
            iData.done()
        }
    }

    @Single(binds = [ListenerProvider::class])
    class Update(val cmdRegistry: CommandRegistryService) :
        CommandFeature<Update.Args>(::Args, CommandSpec("update", "Update".k18n)) {
        class Args : Arguments() {
            var guildId by long("guildid", "Die ID des Servers".k18n)
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            iData.deferReply(true)
            cmdRegistry.updateCommandsForGuild(e.guildId)
            iData.done()
        }
    }

    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {

    }
}

enum class AddRemove {
    ADD, REMOVE;

    fun add() = this == ADD
}