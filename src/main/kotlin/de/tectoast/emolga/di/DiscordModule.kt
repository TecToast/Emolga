package de.tectoast.emolga.di

import de.tectoast.emolga.utils.dependency
import dev.minn.jda.ktx.jdabuilder.default
import dev.minn.jda.ktx.jdabuilder.intents
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module(includes = [ConfigModule::class])
class DiscordModule {
    @Single
    fun emolgaJDA(): JDA {
        return default(dependency("discordToken")) {
            intents -= GatewayIntent.MESSAGE_CONTENT
            setMemberCachePolicy(MemberCachePolicy.DEFAULT)
        }
    }

    @Single
    @Named("flegmon")
    fun flegmonJDA(@Named("discordFlegmonToken") token: String?): JDA? = token?.let { flegmon ->
        default(flegmon) {
            intents += listOf(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
            setMemberCachePolicy(MemberCachePolicy.ALL)
        }
    }
}
