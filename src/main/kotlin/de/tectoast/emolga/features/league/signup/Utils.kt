package de.tectoast.emolga.features.league.signup

import de.tectoast.emolga.domain.league.signup.model.SignupResult
import de.tectoast.emolga.domain.league.signup.model.form.SignupFormField
import de.tectoast.emolga.domain.league.signup.model.form.SignupFormField.*
import de.tectoast.emolga.domain.league.signup.model.form.SignupFormState
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.league.K18n_Signup
import de.tectoast.emolga.utils.t
import de.tectoast.k18n.generated.K18nMessage
import dev.minn.jda.ktx.interactions.components.*
import net.dv8tion.jda.api.components.attachmentupload.AttachmentUpload
import net.dv8tion.jda.api.components.label.LabelChildComponent
import net.dv8tion.jda.api.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.components.textinput.TextInputStyle

context(iData: InteractionData)
internal fun SignupResult.toMessageContent(): K18nMessage {
    return when (this) {
        is SignupResult.Success -> this.message
        is SignupResult.SuccessWithLogoError -> K18n_Signup.SignupSuccessWithLogoError(this.message.t())
        is SignupResult.ErrorValidation -> K18n_Signup.SignupFailure(errors.joinToString("\n\n") { it.t() })
        is SignupResult.ErrorSignup -> K18n_Signup.SignupFailure(this.message.t())
    }
}

context(iData: InteractionData)
internal fun SignupFormState.toModal() = Modal(id = this.customId, title = this.title.t()) {
    for (field in fields) {
        label(label = field.label.t(), description = field.description?.t(), child = field.toComponent())
    }
}

context(iData: InteractionData)
internal fun SignupFormField.toComponent(): LabelChildComponent = when (this) {
    is TextInputState -> TextInput(
        style = TextInputStyle.SHORT,
        customId = this.id,
        value = this.value,
        placeholder = this.placeholder?.t(),
        required = this.inputRequired
    )

    is SelectInputState -> StringSelectMenu(
        customId = this.id,
        placeholder = this.placeholder?.t(),
        options = this.list.map { opt -> SelectOption(opt, opt) },
    ) {
        this.isRequired = inputRequired
    }

    is UserSelectState -> EntitySelectMenu(
        customId = this.id,
        types = listOf(EntitySelectMenu.SelectTarget.USER)
    ) {
        this.isRequired = inputRequired
    }

    is FileUploadState -> AttachmentUpload.create(this.id).let {
        if (inputRequired) it.setRequiredRange(1, 1).setRequired(true)
        else it.setRequiredRange(0, 1)
            .setRequired(false)
    }.build()
}