package de.tectoast.emolga.features.showdown

import de.tectoast.emolga.domain.game.repository.ReplayChannelRepository
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.NoArgs
import de.tectoast.emolga.features.system.model.InvalidArgumentException
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class ReplayChannelCommand(add: Add, remove: Remove) :
    CommandFeature<NoArgs>(NoArgs(), CommandSpec("replaychannel", K18n_ReplayChannel.Help)) {

    override val children = listOf(add, remove)

    @Single
    class Add(private val replayChannelRepo: ReplayChannelRepository) : CommandFeature<Add.Args>(
        ::Args,
        CommandSpec("add", K18n_ReplayChannel.AddHelp)
    ) {
        class Args : Arguments() {
            var channel by channel("Channel", K18n_ReplayChannel.AddArgChannel) {
                validate { if (it is MessageChannel) it else throw InvalidArgumentException(K18n_ReplayChannel.ErrorChannelNotMessage) }
            }.nullable()
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            val resultChannel = e.channel?.idLong ?: iData.tc
            val result = replayChannelRepo.insertChannel(iData.tc, resultChannel, iData.gid)
            iData.reply(
                when (result) {
                    ReplayChannelRepository.ReplayChannelResult.Created -> {
                        if (iData.tc == resultChannel) K18n_ReplayChannel.CreatedSameChannel
                        else K18n_ReplayChannel.CreatedOtherChannel(iData.tc, resultChannel)
                    }

                    is ReplayChannelRepository.ReplayChannelResult.Existed -> {
                        if (result.channel == resultChannel) K18n_ReplayChannel.ExistedSameChannel(result.channel)
                        else K18n_ReplayChannel.ExistedOtherChannel(result.channel)
                    }
                }
            )
        }
    }

    @Single
    class Remove(private val replayChannelRepo: ReplayChannelRepository) :
        CommandFeature<NoArgs>(NoArgs(), CommandSpec("remove", K18n_ReplayChannel.RemoveHelp)) {
        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            if (replayChannelRepo.deleteChannel(iData.tc)) {
                iData.reply(K18n_ReplayChannel.RemoveSuccess)
            } else {
                iData.reply(K18n_ReplayChannel.RemoveNotExist)
            }
        }
    }

    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {

    }
}
