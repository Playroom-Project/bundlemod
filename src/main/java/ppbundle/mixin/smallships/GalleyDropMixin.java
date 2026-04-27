package ppbundle.mixin.smallships;

import com.talhanation.smallships.world.entity.ship.GalleyEntity;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ppbundle.features.smallshipsvariants.DynamicShipVariantHolder;
import ppbundle.features.smallshipsvariants.SmallShipsVariantRegistry;
import ppbundle.features.smallshipsvariants.VariantShipKind;

/**
 * This replaces the vanilla Small Ships drop item when the entity was spawned from a compat wood type.
 */
@Mixin(GalleyEntity.class)
public abstract class GalleyDropMixin {
    @Inject(method = "getDropItem", at = @At("HEAD"), cancellable = true)
    private void ppbundle$replaceDrop(CallbackInfoReturnable<Item> cir) {
        GalleyEntity self = (GalleyEntity) (Object) this;
        String woodTypeId = ((DynamicShipVariantHolder) self).ppbundle$getWoodTypeId();
        Item item = SmallShipsVariantRegistry.shipItem(VariantShipKind.GALLEY, woodTypeId);

        if (item != null) {
            cir.setReturnValue(item);
        }
    }
}