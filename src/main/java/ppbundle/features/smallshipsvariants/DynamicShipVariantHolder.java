package ppbundle.features.smallshipsvariants;

/**
 * This interface exposes the logical wood family id stored on compat boat and ship entities.

 * The value uses the canonical namespaced key format such as "twilightforest:canopy"
 * or "minecraft:oak". It is written by compat items, stored by entity mixins and
 * read back by renderer and drop logic.
 */
public interface DynamicShipVariantHolder {
    /**
     * This returns the canonical namespaced wood family id carried by the entity.
     */
    String ppbundle$getWoodTypeId();

    /**
     * This stores the canonical namespaced wood family id on the entity.
     */
    void ppbundle$setWoodTypeId(String woodTypeId);
}