package ppbundle.mixin.smallships;

import com.talhanation.smallships.world.entity.ship.BriggEntity;
import com.talhanation.smallships.world.entity.ship.CogEntity;
import com.talhanation.smallships.world.entity.ship.DrakkarEntity;
import com.talhanation.smallships.world.entity.ship.GalleyEntity;
import com.talhanation.smallships.world.entity.ship.Ship;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ppbundle.features.smallshipsvariants.DynamicShipVariantHolder;
import ppbundle.features.smallshipsvariants.SmallShipsVariantRegistry;
import ppbundle.features.smallshipsvariants.VariantShipKind;
import ppbundle.features.smallshipsvariants.VariantWoodType;

/**
 * This replaces the native Small Ships drop item on compat ship entities.

 * The mixin targets the concrete ship entity classes because the shared Ship base
 * declares getDropItem as abstract and therefore has no instructions to inject into.

 * Only known PPBundle compat variants are overridden. Native Small Ships content
 * keeps its original drop behavior.
 */
@Mixin({
        CogEntity.class,
        BriggEntity.class,
        GalleyEntity.class,
        DrakkarEntity.class
})
public abstract class ShipDropMixin {
    @Inject(method = "getDropItem", at = @At("HEAD"), cancellable = true)
    private void ppbundle$replaceDrop(CallbackInfoReturnable<Item> cir) {
        Ship self = (Ship) (Object) this;

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

        VariantShipKind kind = ppbundle$resolveShipKind(self);
        if (kind == null) {
            return;
        }

        Item item = SmallShipsVariantRegistry.resolvedShipItem(wood, kind);
        if (item != null && item != Items.AIR) {
            cir.setReturnValue(item);
        }
    }

    /**
     * This maps the live Small Ships entity subtype to the matching compat ship kind.
     */
    @Unique
    private static VariantShipKind ppbundle$resolveShipKind(Ship ship) {
        if (ship instanceof CogEntity) {
            return VariantShipKind.COG;
        }
        if (ship instanceof BriggEntity) {
            return VariantShipKind.BRIGG;
        }
        if (ship instanceof GalleyEntity) {
            return VariantShipKind.GALLEY;
        }
        if (ship instanceof DrakkarEntity) {
            return VariantShipKind.DRAKKAR;
        }
        return null;
    }
}