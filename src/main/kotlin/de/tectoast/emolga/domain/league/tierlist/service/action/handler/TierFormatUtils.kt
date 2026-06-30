package de.tectoast.emolga.domain.league.tierlist.service.action.handler

fun tierAmountToString(tier: String, amount: Int) = buildString {
    append(amount)
    append("x **")
    if (tier.toIntOrNull() != null) {
        append("Tier ")
    }
    append(tier)
    append("**")
}