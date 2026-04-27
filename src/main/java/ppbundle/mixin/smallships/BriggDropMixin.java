package ppbundle.mixin.smallships;

import com.talhanation.smallships.world.entity.ship.BriggEntity;
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
@Mixin(BriggEntity.class)
public abstract class BriggDropMixin {
    @Inject(method = "getDropItem", at = @At("HEAD"), cancellable = true)
    private void ppbundle$replaceDrop(CallbackInfoReturnable<Item> cir) {
        BriggEntity self = (BriggEntity) (Object) this;
        String woodTypeId = ((DynamicShipVariantHolder) self).ppbundle$getWoodTypeId();
        Item item = SmallShipsVariantRegistry.shipItem(VariantShipKind.BRIGG, woodTypeId);

        if (item != null) {
            cir.setReturnValue(item);
        }
    }
}