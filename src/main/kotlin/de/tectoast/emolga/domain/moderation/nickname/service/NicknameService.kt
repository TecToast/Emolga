package de.tectoast.emolga.domain.moderation.nickname.service

import de.tectoast.emolga.discord.InteractableChecker
import de.tectoast.emolga.discord.NicknameSetter
import de.tectoast.emolga.domain.moderation.nickname.repository.NicknameCooldownsRepository
import de.tectoast.emolga.domain.util.service.TimeFormatService
import de.tectoast.emolga.features.various.K18n_Nickname
import de.tectoast.emolga.features.various.NicknameCommand.Companion.MAX_NICKNAME_LENGTH
import de.tectoast.emolga.utils.*
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

@Single
class NicknameService(
    private val nicknameCooldownsRepo: NicknameCooldownsRepository,
    private val interactableChecker: InteractableChecker,
    private val nicknameSetter: NicknameSetter,
    private val timeFormatService: TimeFormatService,
    private val clock: Clock
) {
    suspend fun changeNicknameRequest(guild: Long, userId: Long, nickname: String): K18nMessageOrError {
        if (!interactableChecker.canInteractWithUser(guild, userId)) {
            return K18n_Nickname.NotInteractable.error()
        }
        if (nickname.length > MAX_NICKNAME_LENGTH) {
            return K18n_Nickname.NicknameTooLong(MAX_NICKNAME_LENGTH).error()
        }
        nicknameCooldownsRepo.getCooldown(guild, userId)?.let {
            return b {
                K18n_Nickname.TooSoon(
                    userId,
                    timeFormatService.durationToPrettyLong((it - clock.now()))()
                )()
            }.error()
        }
        nicknameSetter.setNickname(guild, userId, nickname)
        nicknameCooldownsRepo.setCooldown(guild, userId, clock.now() + 7.days)
        return K18n_Nickname.Success(userId, nickname).success()
    }
}
