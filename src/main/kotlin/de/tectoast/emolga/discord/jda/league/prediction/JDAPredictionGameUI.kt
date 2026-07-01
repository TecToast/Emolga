package de.tectoast.emolga.discord.jda.league.prediction

import de.tectoast.emolga.domain.league.prediction.model.PredictionMatchViewState
import de.tectoast.emolga.domain.league.prediction.service.bridge.PredictionGameUI
import de.tectoast.emolga.features.league.prediction.PredictionGameVoteButton
import de.tectoast.emolga.features.system.model.ArgBuilder
import de.tectoast.emolga.utils.k18n
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.generics.getChannel
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.editMessage
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


@Single
class JDAPredictionGameUI(private val jda: JDA) : PredictionGameUI, KoinComponent {

    private val btn: PredictionGameVoteButton by inject()

    private fun JDA.getMessageChannel(id: Long) = getChannel<MessageChannel>(id)

    override suspend fun sendInitialMessage(channelId: Long, title: String, color: Int) {
        val channel = jda.getMessageChannel(channelId) ?: return
        channel.send(embeds = Embed(title = title, color = color).into()).queue()
    }

    override suspend fun sendPredictionGameMessage(state: PredictionMatchViewState): Long {
        val channel = jda.getMessageChannel(state.channelId) ?: return -1
        return channel.send(
            embeds = renderEmbed(state),
            components = renderComponents(state)
        ).await().idLong
    }

    override suspend fun updatePredictionGameMessage(state: PredictionMatchViewState, messageId: Long) {
        val channel = jda.getMessageChannel(state.channelId) ?: return
        channel.editMessage(messageId.toString(), embeds = renderEmbed(state), components = renderComponents(state))
            .queue()
    }

    override suspend fun sendRolePing(channelId: Long, roleId: Long) {
        val channel = jda.getMessageChannel(channelId) ?: return
        channel.send("<@&$roleId>").queue()
    }

    private fun renderComponents(
        state: PredictionMatchViewState,
    ): List<ActionRow> {
        val base: ArgBuilder<PredictionGameVoteButton.Args> = {
            this.leaguename = state.leagueName
            this.week = state.week
            this.battleIndex = state.battleIndex
        }

        return ActionRow.of(btn.withoutIData(label = state.player1Name.k18n, disabled = state.isLocked) {
            base()
            this.idx = state.idx1
        }, btn.withoutIData(label = state.player2Name.k18n, disabled = state.isLocked) {
            base()
            this.idx = state.idx2
        }).into()
    }

    private fun renderEmbed(state: PredictionMatchViewState): List<MessageEmbed> {
        return Embed(
            title = "${state.player1Name} vs. ${state.player2Name}",
            color = state.embedColor,
            description = state.embedDescription
        ).into()
    }
}