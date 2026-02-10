package de.tectoast.emolga.features.flo

import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.k18n
import de.tectoast.emolga.utils.toK18nLanguage
import de.tectoast.emolga.utils.translateTo
import de.tectoast.generic.K18n_SelectLanguage
import de.tectoast.generic.K18n_WelcomeMessage
import de.tectoast.k18n.generated.K18nLanguage
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.guild.GuildJoinEvent

object GuildJoin {

    object Button : ButtonFeature<Button.Args>(::Args, ButtonSpec("guildinvite")) {
        override val buttonStyle = ButtonStyle.PRIMARY
        override val label = "Invite".k18n
        override val emoji = Emoji.fromUnicode("✉️")

        class Args : Arguments() {
            var gid by long()
        }

        init {
            registerListener<GuildJoinEvent> { e ->
                val g = e.guild
                val lang = g.locale.toK18nLanguage()
                val owner = g.ownerIdLong
                e.jda.openPrivateChannelById(owner).flatMap {
                    val (msg, menu) = buildWelcomeMessage(lang, owner, g.name, g.idLong)
                    it.send(
                        content = msg, components = menu
                    )
                }.queue()
                e.jda.openPrivateChannelById(Constants.FLOID).flatMap {
                    it.send(
                        "${e.guild.name} (${e.guild.id})",
                        components = withoutIData { gid = e.guild.idLong }.into()
                    )
                }.queue()
            }
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            iData.jda.getGuildById(e.gid)?.let { g ->
                iData.reply(g.defaultChannel!!.createInvite().setMaxUses(1).await().url)
            } ?: iData.reply("Invite failed, server not found")
        }
    }

    private fun buildWelcomeMessage(lang: K18nLanguage, owner: Long, guildName: String, gid: Long) =
        K18n_WelcomeMessage(owner, guildName, Constants.MYTAG).translateTo(lang) to LanguageMenu(
            placeholder = K18n_SelectLanguage.translateTo(lang),
            options = K18nLanguage.entries.map { en -> SelectOption(en.translateTo(lang), en.name) }
        ) {
            this.guild = gid
        }.into()

    object LanguageMenu : SelectMenuFeature<LanguageMenu.Args>(::Args, SelectMenuSpec("languagemenu")) {

        class Args : Arguments() {
            var selection by singleOption()
            var guild by long().compIdOnly()
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            val lang = enumValueOf<K18nLanguage>(e.selection)
            val guild = iData.jda.getGuildById(e.guild) ?: return
            val (msg, menu) = buildWelcomeMessage(lang, iData.user, guild.name, guild.idLong)
            iData.edit(content = msg, components = menu)
        }
    }
}
