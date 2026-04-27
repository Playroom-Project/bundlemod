package ppbundle.features.smallshipsvariants;

import com.talhanation.smallships.world.entity.ship.GalleyEntity;
import com.talhanation.smallships.world.item.GalleyItem;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

/**
 * This item keeps native Small Ships galley item behavior at the item level while
 * spawning the compat ship entity directly so the canonical wood id is applied
 * to the exact entity instance that enters the world.
 */
public final class VariantGalleyCompatItem extends GalleyItem {
    private final String woodTypeId;

    public VariantGalleyCompatItem(String woodTypeId, Properties properties) {
        super(Boat.Type.OAK, properties);
        this.woodTypeId = woodTypeId;
    }

    /**
     * This returns the canonical namespaced wood family id carried by this compat item.
     */
    public String woodTypeId() {
        return woodTypeId;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
            @NotNull Level level,
            @NotNull Player player,
            @NotNull InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }

        Boat boat = createCompatShip(level, hitResult);
        boat.setYRot(player.getYRot());

        if (!level.noCollision(boat, boat.getBoundingBox())) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            level.addFreshEntity(boat);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, hitResult.getLocation());

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        VariantWoodType wood = SmallShipsVariantRegistry.wood(woodTypeId);
        if (wood == null) {
            return Component.literal(woodTypeId + " Galley");
        }

        return Component.literal(wood.englishDisplayName() + " Galley");
    }

    /**
     * This creates the galley entity and writes the compat wood id before the entity
     * is spawned into the world.
     */
    private @NotNull Boat createCompatShip(@NotNull Level level, @NotNull HitResult hitResult) {
        GalleyEntity entity = GalleyEntity.summon(
                level,
                hitResult.getLocation().x,
                hitResult.getLocation().y,
                hitResult.getLocation().z
        );

        if (entity instanceof DynamicShipVariantHolder holder) {
            holder.ppbundle$setWoodTypeId(woodTypeId);
        }

        return entity;
    }
}