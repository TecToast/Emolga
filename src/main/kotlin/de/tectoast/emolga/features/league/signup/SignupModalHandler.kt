package de.tectoast.emolga.features.league.signup

import de.tectoast.emolga.discord.jda.features.JDAInteractionData
import de.tectoast.emolga.discord.toFileSubmission
import de.tectoast.emolga.domain.language.repository.GuildLanguageRepository
import de.tectoast.emolga.domain.league.signup.model.SignupInput
import de.tectoast.emolga.domain.league.signup.model.form.FileSubmission
import de.tectoast.emolga.domain.league.signup.model.form.SignupSubmissionRequest
import de.tectoast.emolga.domain.league.signup.service.SignupService
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.t
import net.dv8tion.jda.api.components.Component
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class SignupModalHandler(private val signupService: SignupService, private val languageRepo: GuildLanguageRepository) :
    ListenerProvider() {
    init {
        registerListener<ModalInteractionEvent> {
            val modalId = it.modalId
            if (!modalId.startsWith("signup;")) return@registerListener
            val gid = it.guild?.idLong ?: return@registerListener
            it.deferReply(true).queue()
            val split = modalId.split(';')
            val identifier = split[1]
            val change = split[2].isNotBlank()
            val fieldData = mutableMapOf<String, String>()
            var logoAttachment: FileSubmission? = null
            var teammates: List<Long>? = null
            for (entry in it.values) {
                val id = entry.customId
                if (id == SignupInput.LOGO_ID) {
                    logoAttachment = entry.asAttachmentList.firstOrNull()?.toFileSubmission()
                    continue
                }
                if (id == SignupInput.TEAMMATE_ID) {
                    teammates = entry.asMentions.users.filter { u -> !u.isBot }.map { u -> u.idLong }
                    continue
                }
                fieldData[id] = when (entry.type) {
                    Component.Type.TEXT_INPUT -> entry.asString
                    Component.Type.STRING_SELECT -> entry.asStringList.first()
                    else -> error("Unsupported component type in signup modal ${it.type}")
                }
            }
            val result = signupService.processSubmission(
                SignupSubmissionRequest(
                    guildId = gid,
                    userId = it.user.idLong,
                    identifier = identifier,
                    isChange = change,
                    fieldData = fieldData,
                    teammates = teammates.orEmpty(),
                    logoAttachment = logoAttachment
                )
            )
            val lang = languageRepo.getLanguage(gid)
            val iData = JDAInteractionData(it, lang)
            with(iData) {
                it.reply(result.toMessageContent().t()).setEphemeral(true).queue()
            }
        }
    }
}