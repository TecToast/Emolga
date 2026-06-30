package de.tectoast.emolga.domain.guildspecific.nominate.model

import de.tectoast.emolga.domain.league.core.model.LeagueAttribute

object NominationCurrentWeek : LeagueAttribute<Int>("nominationCurrentWeek", Int::class, defaultValue = 1)