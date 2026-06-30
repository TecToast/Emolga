package de.tectoast.emolga.domain.scheduling.repeat.model

sealed interface RepeatTaskType {
    data class PredictionGameSending(val leagueName: String) : RepeatTaskType
    data class PredictionGameLockButtons(val leagueName: String) : RepeatTaskType
    data class PredictionGameLeaderboard(val leagueName: String) : RepeatTaskType
    data class RegisterInDoc(val leagueName: String, val battleIndex: Int) : RepeatTaskType
    data class YTSendAfterGrace(val leagueName: String, val battleIndex: Int) : RepeatTaskType
    data class YTEnable(val leagueName: String, val battleIndex: Int) : RepeatTaskType
    data class SendReminderToParticipants(val leagueName: String, val battleIndex: Int) : RepeatTaskType
    data class TransactionDocInsert(val leagueName: String) : RepeatTaskType
    data class Other(val descriptor: String) : RepeatTaskType
}
