package ppbundle.features.smallshipsvariants;

import net.minecraft.resources.ResourceLocation;

/**
 * This describes one detected wood family and the ids derived from it.

 * The namespaced wood key is the canonical logical key used across registry lookups,
 * compat item registration, recipes and render-time texture resolution.

 * The flattened name is the namespace-safe path fragment used for generated Small Ships
 * fallback ids and runtime-generated asset paths.
 */
public record VariantWoodType(
        ResourceLocation planksId,
        String namespacedWoodKey,
        String flattenedName,
        String englishDisplayName,
        ResourceLocation boatItemId,
        ResourceLocation chestBoatItemId
) {
    /**
     * This returns the namespace of the source wood family.
     */
    public String sourceNamespace() {
        return planksId.getNamespace();
    }

    /**
     * This returns the full block path of the plank block.
     */
    public String plankBlockPath() {
        return planksId.getPath();
    }

    /**
     * This returns the logical wood path without the "_planks" suffix when present.
     */
    public String woodPath() {
        String path = planksId.getPath();
        return path.endsWith("_planks")
                ? path.substring(0, path.length() - "_planks".length())
                : path;
    }
}