package de.tectoast.emolga.domain.league.signup.model.form

data class SignupSubmissionRequest(
    val guildId: Long,
    val userId: Long,
    val identifier: String,
    val isChange: Boolean,
    val fieldData: Map<String, String>,
    val teammates: List<Long>, val logoAttachment: FileSubmission?
)