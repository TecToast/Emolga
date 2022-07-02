package de.tectoast.emolga.utils.draft

class DraftPokemon(var name: String, var tier: String) {

    override fun toString(): String {
        return "DraftPokemon{" +
                "name='" + name + '\'' +
                ", tier='" + tier + '\'' +
                '}'
    }
}