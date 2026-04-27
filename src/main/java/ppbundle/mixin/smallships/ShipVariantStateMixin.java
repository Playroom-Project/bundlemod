package ppbundle.mixin.smallships;

import com.talhanation.smallships.world.entity.ship.Ship;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ppbundle.features.smallshipsvariants.DynamicShipVariantHolder;

/**
 * This stores the canonical PPBundle wood family id on Small Ships entities.

 * The value is synchronized through entity data so the client renderer can resolve
 * the generated runtime texture without requiring extra packets or reloads.

 * The same value is also written to NBT so ships preserve their compat wood family
 * across chunk saves and world reloads.
 */
@Mixin(Ship.class)
public abstract class ShipVariantStateMixin extends Entity implements DynamicShipVariantHolder {
    @Unique
    private static final String PPBUNDLE_WOOD_TYPE_TAG = "PPBundleWoodType";

    @Unique
    private static final EntityDataAccessor<String> PPBUNDLE_WOOD_TYPE =
            SynchedEntityData.defineId(Ship.class, EntityDataSerializers.STRING);

    protected ShipVariantStateMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * This allocates the synced string slot used by both server and client.
     */
    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void ppbundle$defineVariantState(CallbackInfo ci) {
        this.entityData.define(PPBUNDLE_WOOD_TYPE, "");
    }

    /**
     * This restores the persisted compat wood family id from saved entity data.
     */
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void ppbundle$readVariantState(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(PPBUNDLE_WOOD_TYPE_TAG, 8)) {
            this.ppbundle$setWoodTypeId(tag.getString(PPBUNDLE_WOOD_TYPE_TAG));
        }
    }

    /**
     * This persists the compat wood family id so ships keep their variant identity.
     */
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void ppbundle$writeVariantState(CompoundTag tag, CallbackInfo ci) {
        String woodTypeId = this.ppbundle$getWoodTypeId();
        if (!woodTypeId.isBlank()) {
            tag.putString(PPBUNDLE_WOOD_TYPE_TAG, woodTypeId);
        }
    }

    @Override
    public String ppbundle$getWoodTypeId() {
        return this.entityData.get(PPBUNDLE_WOOD_TYPE);
    }

    @Override
    public void ppbundle$setWoodTypeId(String woodTypeId) {
        this.entityData.set(PPBUNDLE_WOOD_TYPE, sanitizeWoodTypeId(woodTypeId));
    }

    @Unique
    private static String sanitizeWoodTypeId(String woodTypeId) {
        return woodTypeId == null ? "" : woodTypeId;
    }
}