package de.tectoast.emolga.features.showdown

import de.tectoast.emolga.database.exposed.AnalysisDB
import de.tectoast.emolga.features.*
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel

object ReplayChannelCommand :
    CommandFeature<NoArgs>(NoArgs(), CommandSpec("replaychannel", K18n_ReplayChannel.Help)) {

    object Add : CommandFeature<Add.Args>(
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
            val result = AnalysisDB.insertChannel(iData.tc, resultChannel, iData.gid)
            iData.reply(
                when (result) {
                    AnalysisDB.AnalysisResult.CREATED -> {
                        if (iData.tc == resultChannel) K18n_ReplayChannel.CreatedSameChannel
                        else K18n_ReplayChannel.CreatedOtherChannel(iData.tc, resultChannel)
                    }

                    is AnalysisDB.AnalysisResult.Existed -> {
                        if (result.channel == resultChannel) K18n_ReplayChannel.ExistedSameChannel(result.channel)
                        else K18n_ReplayChannel.ExistedOtherChannel(result.channel)
                    }
                }
            )
        }
    }

    object Remove : CommandFeature<NoArgs>(NoArgs(), CommandSpec("remove", K18n_ReplayChannel.RemoveHelp)) {
        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            if (AnalysisDB.deleteChannel(iData.tc)) {
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
