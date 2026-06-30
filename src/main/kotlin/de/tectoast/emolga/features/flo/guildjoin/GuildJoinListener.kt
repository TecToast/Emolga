package de.tectoast.emolga.features.flo.guildjoin

import de.tectoast.emolga.domain.discord.service.GuildWelcomeViewService
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.BotConstants
import de.tectoast.emolga.utils.toK18nLanguage
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class GuildJoinListener(
    private val service: GuildWelcomeViewService,
    private val menu: LanguageMenu,
    private val button: GuildJoinButton,
    private val botConstants: BotConstants
) :
    ListenerProvider() {
    init {
        registerListener<GuildJoinEvent> { e ->
            val g = e.guild
            val lang = g.locale.toK18nLanguage()
            val owner = g.ownerIdLong
            val viewState = service.buildWelcomeViewState(lang, owner, g.name, g.idLong)
            e.jda.openPrivateChannelById(owner).flatMap {
                val menu = service.buildWelcomeMessageMenu(viewState, menu)
                it.send(
                    content = viewState.messageContent, components = menu
                )
            }.queue()
            e.jda.openPrivateChannelById(botConstants.botOwnerId).flatMap {
                it.send(
                    "${e.guild.name} (${e.guild.id})",
                    components = button.withoutIData { gid = e.guild.idLong }.into()
                )
            }.queue()
        }
    }
}