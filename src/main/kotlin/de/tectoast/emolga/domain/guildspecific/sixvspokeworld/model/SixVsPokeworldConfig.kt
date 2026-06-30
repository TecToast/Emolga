package de.tectoast.emolga.domain.guildspecific.sixvspokeworld.model

import kotlinx.serialization.Serializable

@Serializable
data class SixVsPokeworldConfig(
    val challenges: List<SixVsPokeworldMilestone> = listOf()
) {

    @Serializable
    data class SixVsPokeworldMilestone(
        val title: String,
        val info: String,
        val infoReward: String,
        val easy: ExerciseData,
        val medium: ExerciseData,
        val hard: ExerciseData,
    )

    @Serializable
    data class ExerciseData(
        val title: String,
        val text: String,
        val fileKey: String = "",
        val fileKeyEn: String = "",
        val reward: String
    )
}
