package ppbundle.features.smallshipsvariants;

import java.util.Locale;

/**
 * This enum is the single source of truth for every Small Ships runtime ship type
 * supported by the current Small Ships build.

 * Only kinds that have real item classes, entity classes, recipes and renderers
 * in the target mod should be listed here.
 */
public enum VariantShipKind {
    COG("cog", "Cog"),
    BRIGG("brigg", "Brigg"),
    GALLEY("galley", "Galley"),
    DRAKKAR("drakkar", "Drakkar");

    private final String serializedName;
    private final String englishDisplayName;

    VariantShipKind(String serializedName, String englishDisplayName) {
        this.serializedName = serializedName;
        this.englishDisplayName = englishDisplayName;
    }

    public String serializedName() {
        return serializedName;
    }

    public String englishDisplayName() {
        return englishDisplayName;
    }

    public String recipeGroup() {
        return serializedName.toLowerCase(Locale.ROOT);
    }
}