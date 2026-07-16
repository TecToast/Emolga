package de.tectoast.emolga.domain.league.signup.service

import de.tectoast.emolga.di.TransactionRunner
import de.tectoast.emolga.discord.ChannelInterface
import de.tectoast.emolga.discord.GuildMemberRepository
import de.tectoast.emolga.discord.editMessage
import de.tectoast.emolga.discord.sendMessage
import de.tectoast.emolga.domain.config.repository.GuildConfigRepository
import de.tectoast.emolga.domain.league.showdownnames.service.ShowdownNameInsertService
import de.tectoast.emolga.domain.league.signup.model.*
import de.tectoast.emolga.domain.league.signup.model.form.*
import de.tectoast.emolga.domain.league.signup.repository.SignupEntryTable
import de.tectoast.emolga.domain.league.signup.repository.SignupRepository
import de.tectoast.emolga.domain.league.signup.service.input.SignupInputDispatcher
import de.tectoast.emolga.domain.league.signup.service.logo.LogoService
import de.tectoast.emolga.domain.league.signup.service.logo.settings.LogoSettingsDispatcher
import de.tectoast.emolga.domain.ytgeneric.repository.YouTubeChannelsRepository
import de.tectoast.emolga.domain.ytgeneric.service.YouTubeChannelIdService
import de.tectoast.emolga.features.league.K18n_AddTeammate
import de.tectoast.emolga.features.league.K18n_Logo
import de.tectoast.emolga.features.league.K18n_Signup
import de.tectoast.emolga.features.league.draft.generic.K18n_NoSignupInGuild
import de.tectoast.emolga.features.league.signup.SignoutButton
import de.tectoast.emolga.features.league.signup.SignupButton
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.json.K18n_SignupInput
import de.tectoast.generic.K18n_SignupNoun
import de.tectoast.k18n.generated.K18nLanguage
import de.tectoast.k18n.generated.K18nMessage
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.MessageEdit
import dev.minn.jda.ktx.messages.into
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import mu.KotlinLogging
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.update
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.get


@Single
class SignupService(
    private val signupRepo: SignupRepository,
    private val languageRepo: GuildConfigRepository,
    private val tx: TransactionRunner,
    private val channelInterface: ChannelInterface,
    private val guildMemberRepo: GuildMemberRepository,
    private val signUpInputDispatcher: SignupInputDispatcher,
    private val logoSettingsDispatcher: LogoSettingsDispatcher,
    private val logoService: LogoService,
    private val sdNameInsertService: ShowdownNameInsertService,
    private val youTubeChannelsRepository: YouTubeChannelsRepository,
    private val messageSyncWorker: SignupMessageSyncWorker,
    private val ytChannelIdService: YouTubeChannelIdService,
    baseScope: CoroutineScope,
) : KoinComponent {

    private val logger = KotlinLogging.logger {}

    val scope = baseScope + CoroutineName("SignupService")


    suspend fun createSignup(
        gid: Long,
        config: LeagueSignupConfig
    ): Boolean {
        val identifier = config.identifier
        if (signupRepo.exists(gid, identifier)) return false
        val language = languageRepo.getLanguage(gid)
        val messageid =
            channelInterface.sendMessage(
                config.announceChannel,
                MessageCreate(
                    buildString {
                        append(config.signupMessage)
                        if (!config.hideUserCount) {
                            append("\n\n")
                            append(
                                K18n_Signup.SignupMessageData(
                                    "0",
                                    config.maxUsers.takeIf { it > 0 }?.toString() ?: "?"
                                ).translateTo(language)
                            )
                        }
                    },
                    components = getSignupButtons(identifier, language)
                )
            ) ?: return false
        signupRepo.createNewSignup(gid, identifier, config, messageid)
        return true
    }

    private fun getSignupButtons(identifier: String, language: K18nLanguage, disabled: Boolean = false) = listOf(
        get<SignupButton>().withoutIData(
            language = language,
            disabled = disabled
        ) {
            this.identifier = identifier
        }, get<SignoutButton>().withoutIData(language = language)
    ).into()

    suspend fun handleSignupClick(guild: Long, identifier: String, user: Long): CalcResult<SignupButtonResult> {
        val signup = signupRepo.getLeagueSignup(guild, identifier) ?: return K18n_Signup.SignUpClosedError.error()
        val signupChangeForm = trySignupChange(signup, user).getOrReturn { return it }
        signupChangeForm?.let { return SignupButtonResult.Form(it).success() }
        val form = buildForm(signup.config, oldData = null)
        if (form == null) {
            val signupResult =
                signupUser(guild = guild, identifier = identifier, users = setOf(user), data = emptyMap())
            return SignupButtonResult.DirectSignup(signupResult).success()
        }
        return SignupButtonResult.Form(form).success()
    }

    suspend fun trySignupChange(guild: Long, user: Long): CalcResult<SignupFormState> {
        val signup = signupRepo.getLeagueSignupOfUser(guild, user) ?: return K18n_Signup.NotSignedUp.error()
        val formState = trySignupChange(signup, user).getOrReturn { return it }
        return formState?.success() ?: K18n_Signup.NotSignedUp.error()
    }

    private suspend fun trySignupChange(signup: LeagueSignup, user: Long): CalcResult<SignupFormState?> {
        signupRepo.getSignupEntryByUserId(signup.id, user)?.let { (_, signUpData) ->
            val form = buildForm(signup.config, signUpData) ?: return K18n_Signup.SignupChangeNoData.error()
            return form.success()
        }
        return CalcResult.Success(null)
    }

    suspend fun removeUser(guild: Long, user: Long): K18nMessageOrError {
        return when (val result = signupRepo.removeUser(guild, user)) {
            SignupRemoveUserResult.NotFound -> K18n_Signup.NotSignedUp.error()
            is SignupRemoveUserResult.Removed -> {
                val leagueSignup = signupRepo.getLeagueSignup(result.signupId) ?: return K18n_NoSignupInGuild.error()
                takeParticipantRole(leagueSignup.config.participantRole, guild, user)
                val entry = result.entry
                if (result.deletedEntry) {
                    entry.signupMessageId?.let {
                        channelInterface.deleteMessage(leagueSignup.config.signupChannel, it)
                    }
                    leagueSignup.config.logoSettings?.let { logoSettings ->
                        logoSettingsDispatcher.handleSignupRemoved(logoSettings, leagueSignup, data = entry)
                    }
                    messageSyncWorker.notifySignupChange()
                } else {
                    handleSignupChange(leagueSignup, entry)
                }
                K18n_Signup.UnsignupSuccess.success()
            }
        }
    }

    suspend fun addTeammate(guild: Long, user: Long, teammate: Long): K18nMessageOrError {
        signupRepo.getLeagueSignupOfUser(guild, teammate)?.let {
            return K18n_AddTeammate.PartnerAlreadySignedUp(teammate).error()
        }
        val signup = signupRepo.getLeagueSignupOfUser(guild, user) ?: return K18n_Signup.NotSignedUp.error()
        val (entryId, entry) = signupRepo.getSignupEntryByUserId(signup.id, user)
            ?: return K18n_Signup.NotSignedUp.error()
        entry.users.add(teammate)
        signupRepo.editSignupEntryUsers(entryId, entry.users)
        handleSignupChange(signup, entry)
        giveParticipantRole(signup.config.participantRole, guild, teammate)
        return K18n_AddTeammate.Success(teammate).success()
    }

    private fun buildForm(config: LeagueSignupConfig, oldData: SignupEntry?): SignupFormState? {
        if (config.signupStructure.isEmpty() && !config.allowTeams && config.logoSettings == null) {
            return null
        }
        val customIdSuffix = if (oldData != null) "change" else ""
        val formId = "signup;${config.identifier};$customIdSuffix"
        val fields = mutableListOf<SignupFormField>()
        config.signupStructure.forEach { structure ->
            val options = signUpInputDispatcher.getModalInputOptions(structure)
            val state = options.list?.let { list ->
                SignupFormField.SelectInputState(
                    id = structure.id,
                    label = options.label,
                    inputRequired = options.required,
                    description = options.description,
                    placeholder = options.placeholder,
                    list = list
                )
            } ?: SignupFormField.TextInputState(
                id = structure.id,
                label = options.label,
                inputRequired = options.required,
                placeholder = options.placeholder,
                value = oldData?.data?.get(structure.id)?.let {
                    signUpInputDispatcher.mapValueForDisplay(structure, it)
                }
            )
            fields.add(state)
        }
        if (config.allowTeams) {
            fields.add(
                SignupFormField.UserSelectState(
                    id = SignupInput.TEAMMATE_ID,
                    label = K18n_SignupInput.TeammateLabel,
                    description = K18n_SignupInput.TeammateDescription
                )
            )
        }
        if (config.logoSettings != null) {
            fields.add(
                SignupFormField.FileUploadState(
                    id = SignupInput.LOGO_ID,
                    label = K18n_SignupInput.LogoLabel,
                    description = K18n_SignupInput.LogoDescription
                )
            )
        }
        return SignupFormState(
            customId = formId, title = K18n_SignupNoun, fields = fields
        )
    }


    suspend fun processSubmission(request: SignupSubmissionRequest): SignupResult {
        val config = signupRepo.getConfig(request.guildId, request.identifier) ?: return SignupResult.ErrorSignup(
            K18n_NoSignupInGuild
        )
        val errors = mutableListOf<K18nMessage>()
        val validatedData = mutableMapOf<String, String>()
        request.fieldData.forEach { (id, rawValue) ->
            val inputConfig = config.getInputConfig(id) ?: return@forEach
            when (val result = signUpInputDispatcher.validate(inputConfig, rawValue)) {
                is SignupValidateResult.Error -> {
                    errors += b {
                        K18n_SignupInput.Error(
                            signUpInputDispatcher.getModalInputOptions(inputConfig).label(), result.message()
                        )()
                    }
                }

                is SignupValidateResult.Success -> {
                    validatedData[id] = result.data
                }
            }
        }
        if (errors.isNotEmpty()) {
            return SignupResult.ErrorValidation(errors)
        }
        return signupUser(
            guild = request.guildId, identifier = request.identifier, users = buildSet {
                add(request.userId)
                addAll(request.teammates)
            }, data = validatedData, logoAttachment = request.logoAttachment, isChange = request.isChange
        )
    }

    private suspend fun signupUser(
        guild: Long,
        identifier: String,
        users: Set<Long>,
        data: Map<String, String>,
        logoAttachment: FileSubmission? = null,
        isChange: Boolean = false
    ): SignupResult {
        var isNewSignup = false
        val signupResult = tx {
            val firstUser = users.first()
            val leagueSignup =
                signupRepo.getLeagueSignup(guild, identifier, locking = true) ?: return@tx SignupResult.ErrorSignup(
                    K18n_NoSignupInGuild
                )
            val config = leagueSignup.config
            val maxUsers = config.maxUsers
            val signupId = leagueSignup.id
            val currentSignupCount = signupRepo.getCurrentSignupCount(signupId)
            if (!isChange && maxUsers > 0 && currentSignupCount >= maxUsers) {
                return@tx SignupResult.ErrorSignup(K18n_Signup.SignupFull)
            }
            val existingEntryIds = signupRepo.getSignupEntryIdsOfUsers(signupId, users)
            val entryIdOfFirst = existingEntryIds[firstUser]
            for (user in users) {
                val entryOfUser = existingEntryIds[user]
                if (entryOfUser != null && entryIdOfFirst != entryOfUser) {
                    return@tx SignupResult.ErrorSignup(K18n_AddTeammate.PartnerAlreadySignedUp(user))
                }
            }
            if (isChange) {
                if (entryIdOfFirst == null) {
                    return@tx SignupResult.ErrorSignup(K18n_Signup.NotSignedUp)
                }
                val signupEntry = signupRepo.getSignupEntryById(entryIdOfFirst) ?: return@tx SignupResult.ErrorSignup(
                    K18n_Signup.NotSignedUp
                )
                signupEntry.users.addAll(users)
                signupEntry.data.putAll(data)
                signupRepo.editSignupEntry(entryIdOfFirst, signupEntry)
                scope.launch { handleSignupChange(leagueSignup, signupEntry) }
                logoAttachment?.handleLogoOnSignup(entryIdOfFirst, leagueSignup)?.let { return@tx it }
                return@tx SignupResult.Success(K18n_Signup.DataChangeSuccessful)
            }
            val messageId = channelInterface.sendMessage(
                config.signupChannel,
                getSignupEntryMessage(config, users, data).translateTo(languageRepo.getLanguage(guild))
            )
            val signupEntry = SignupEntry(users.toMutableSet(), data.toMutableMap(), signupMessageId = messageId)
            val entryId = signupRepo.saveNewSignupEntry(signupId, signupEntry)
            isNewSignup = true
            if (currentSignupCount + 1 >= maxUsers) {
                closeSignup(leagueSignup)
            }
            logoAttachment?.handleLogoOnSignup(entryId, leagueSignup)?.let { return@tx it }
            scope.launch {
                data[SignupInput.SDNAME_ID]?.let {
                    sdNameInsertService.addIfAbsent(it, firstUser)
                }
                data[SignupInput.YT_CHANNEL_ID]?.let {
                    try {
                        youTubeChannelsRepository.insertSingle(firstUser, ytChannelIdService.mapToChannelId(it))
                    } catch (ex: IllegalArgumentException) {
                        logger.warn("Failed to parse YouTube channel id from $it for user $firstUser", ex)
                    }
                }
                users.forEach { uid ->
                    giveParticipantRole(config.participantRole, guild, uid)
                }
            }
            return@tx SignupResult.Success(K18n_Signup.SignupSuccess)
        }
        if (isNewSignup) messageSyncWorker.notifySignupChange()
        return signupResult
    }

    suspend fun closeSignup(leagueSignup: LeagueSignup) {
        val messageId = leagueSignup.announceMessageId ?: return
        val lang = languageRepo.getLanguage(leagueSignup.guild)
        val config = leagueSignup.config
        channelInterface.editMessage(
            config.announceChannel,
            messageId,
            MessageEdit(components = getSignupButtons(leagueSignup.identifier, lang, disabled = true))
        )
        val msg = "_----------- ${K18n_Signup.SignupClosed.translateTo(lang)} -----------_"
        channelInterface.sendMessage(config.announceChannel, msg)
        if(config.announceChannel != config.signupChannel)
            channelInterface.sendMessage(config.signupChannel, msg)
    }


    private suspend fun giveParticipantRole(participantRole: Long?, guildId: Long, uid: Long) {
        participantRole?.let {
            guildMemberRepo.addRole(guildId, uid, it)
        }
    }

    private suspend fun takeParticipantRole(participantRole: Long?, guildId: Long, uid: Long) {
        participantRole?.let {
            guildMemberRepo.removeRole(guildId, uid, it)
        }
    }

    private suspend fun FileSubmission.handleLogoOnSignup(entryId: Int, leagueSignup: LeagueSignup): SignupResult? {
        return when (val result = insertLogo(entryId, this, leagueSignup)) {
            is CalcResult.Success<*> -> null
            is CalcResult.Error<*> -> SignupResult.SuccessWithLogoError(result.message)
        }
    }

    suspend fun insertLogo(guild: Long, userId: Long, logo: FileSubmission): K18nMessageOrError {
        val signup = signupRepo.getLeagueSignupOfUser(guild, userId) ?: return K18n_Signup.NotSignedUp.error()
        val (entryId, _) = signupRepo.getSignupEntryByUserId(signup.id, userId)
            ?: return K18n_Signup.NotSignedUp.error()
        return insertLogo(entryId, logo, signup).map { K18n_Logo.Success }
    }

    private suspend fun insertLogo(entryId: Int, logo: FileSubmission, leagueSignup: LeagueSignup): CalcResult<Unit> =
        tx {
            val logoSettings = leagueSignup.config.logoSettings ?: return@tx K18n_Signup.NoOwnLogos.error()
            val signupEntry = signupRepo.getSignupEntryById(entryId) ?: return@tx K18n_Signup.NotSignedUp.error()
            val logoData = logoService.fromAttachment(logo, teamName = signupEntry.data[SignupInput.TEAMNAME_ID])
            if (logoData.isError()) {
                return@tx logoData.message.error()
            }
            logoSettingsDispatcher.handleLogo(logoSettings, leagueSignup, signupEntry, logoData.value)
            logoService.uploadLogo(logoData.value)
            SignupEntryTable.update({ SignupEntryTable.id eq entryId }) {
                it[SignupEntryTable.logoIdentifier] = logoData.value.fileName
            }
            Unit.success()
        }

    private suspend fun handleSignupChange(leagueSignup: LeagueSignup, data: SignupEntry) {
        channelInterface.editMessage(
            leagueSignup.config.signupChannel, data.signupMessageId ?: return, getSignupEntryMessage(
                leagueSignup.config, data.users, data.data
            ).translateTo(languageRepo.getLanguage(leagueSignup.guild))
        )
    }

    private fun getSignupEntryMessage(
        config: LeagueSignupConfig,
        users: Set<Long>,
        data: Map<String, String>
    ): K18nMessage {
        return b {
            K18n_Signup.SignupConfirmMessage(users.joinToTeammates(), data.entries.mapNotNull { (k, v) ->
                val inputConfig = config.getInputConfig(k) ?: return@mapNotNull null
                val displayTitle = signUpInputDispatcher.getDisplayTitle(inputConfig) ?: return@mapNotNull null
                val displayValue = signUpInputDispatcher.mapValueForDisplay(inputConfig, v)
                "${displayTitle()}: **$displayValue**"
            }.joinToString("\n"))()
        }
    }


}
