package de.tectoast.emolga.utils.draft;

public class DraftPokemon {

    public String name;
    public String tier;

    public DraftPokemon(String name, String tier) {
        this.name = name;
        this.tier = tier;
    }

    public String getName() {
        return name;
    }

    public String getTier() {
        return tier;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public DraftPokemon copy() {
        return new DraftPokemon(name, tier);
    }

    @Override
    public String toString() {
        return "DraftPokemon{" +
                "name='" + name + '\'' +
                ", tier='" + tier + '\'' +
                '}';
    }
}
