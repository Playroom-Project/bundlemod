package ppbundle.mixin.smallships;

import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ppbundle.features.smallshipsvariants.DynamicShipVariantHolder;
import ppbundle.features.smallshipsvariants.SmallShipsVariantRegistry;
import ppbundle.features.smallshipsvariants.VariantWoodType;

/**
 * This replaces the vanilla boat drop item when the entity carries a compat wood id.

 * The replacement only happens for known compat wood families so native vanilla
 * and native modded boats keep their original drop behavior.
 */
@Mixin(Boat.class)
public abstract class BoatDropMixin {
    @Inject(method = "getDropItem", at = @At("HEAD"), cancellable = true)
    private void ppbundle$replaceDrop(CallbackInfoReturnable<Item> cir) {
        Boat self = (Boat) (Object) this;

        if (!(self instanceof DynamicShipVariantHolder holder)) {
            return;
        }

        String woodTypeId = holder.ppbundle$getWoodTypeId();
        if (woodTypeId == null || woodTypeId.isBlank()) {
            return;
        }

        VariantWoodType wood = SmallShipsVariantRegistry.wood(woodTypeId);
        if (wood == null) {
            return;
        }

        Item item = SmallShipsVariantRegistry.resolvedBoatIngredient(wood);
        if (item != null && item != Items.AIR) {
            cir.setReturnValue(item);
        }
    }
}