package ppbundle.features.smallshipsvariants;

import net.minecraft.resources.ResourceLocation;

/**
 * This stores the intermediate discovery state for one wood family until it is
 * promoted into the final Small Ships compat registry.
 */
final class PendingWoodFamily {
    private final String woodTypeId;
    private final String woodPath;
    private final String flattenedName;
    private final String englishDisplayName;
    private final ResourceLocation boatItemId;
    private final ResourceLocation chestBoatItemId;

    private ResourceLocation planksId;
    private boolean finalized;

    PendingWoodFamily(
            String woodTypeId,
            String woodPath,
            String flattenedName,
            String englishDisplayName,
            ResourceLocation boatItemId,
            ResourceLocation chestBoatItemId
    ) {
        this.woodTypeId = woodTypeId;
        this.woodPath = woodPath;
        this.flattenedName = flattenedName;
        this.englishDisplayName = englishDisplayName;
        this.boatItemId = boatItemId;
        this.chestBoatItemId = chestBoatItemId;
    }

    /**
     * This returns the normalized namespaced wood key such as "modid:driftwood".
     */
    public String woodTypeId() {
        return woodTypeId;
    }

    /**
     * This returns the raw wood path such as "driftwood".
     */
    public String woodPath() {
        return woodPath;
    }

    /**
     * This returns the flattened compat-safe name used in fallback ids and generated assets.
     */
    public String flattenedName() {
        return flattenedName;
    }

    /**
     * This returns the human readable display name used in generated item names.
     */
    public String englishDisplayName() {
        return englishDisplayName;
    }

    /**
     * This returns the expected native boat item id for the wood family.
     */
    public ResourceLocation boatItemId() {
        return boatItemId;
    }

    /**
     * This returns the expected native chest boat item id for the wood family.
     */
    public ResourceLocation chestBoatItemId() {
        return chestBoatItemId;
    }

    /**
     * This stores the plank block id that anchors this wood family.
     */
    public void setPlanksId(ResourceLocation planksId) {
        this.planksId = planksId;
    }

    /**
     * This returns the discovered plank block id.
     */
    public ResourceLocation planksId() {
        return planksId;
    }

    /**
     * This indicates whether the family has enough information to become a final variant.
     */
    public boolean hasPlanks() {
        return planksId != null;
    }

    /**
     * This indicates whether the family has already been promoted into the final registry.
     */
    public boolean isFinalized() {
        return finalized;
    }

    /**
     * This marks the family as already promoted so it cannot be registered twice.
     */
    public void markFinalized() {
        this.finalized = true;
    }

    /**
     * This converts the pending state into the final normalized wood record.
     */
    public VariantWoodType toVariantWoodType() {
        if (planksId == null) {
            throw new IllegalStateException("Cannot create VariantWoodType without planks for " + woodTypeId);
        }

        return new VariantWoodType(
                planksId,
                woodTypeId,
                flattenedName,
                englishDisplayName,
                boatItemId,
                chestBoatItemId
        );
    }
}