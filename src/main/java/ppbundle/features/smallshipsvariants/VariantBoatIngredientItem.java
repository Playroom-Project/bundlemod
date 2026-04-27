package ppbundle.features.smallshipsvariants;

import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

/**
 * This item is a real placeable boat item used as a compat fallback when a wood
 * family does not provide its own boat or chest boat item.

 * The spawned vanilla boat entity receives the canonical PPBundle wood family id
 * immediately so sync, save data, renderer lookups and drop logic all operate on
 * the same stable value.
 */
public final class VariantBoatIngredientItem extends BoatItem {
    private final String woodTypeId;
    private final boolean chestBoat;

    public VariantBoatIngredientItem(String woodTypeId, boolean chestBoat, Properties properties) {
        super(chestBoat, Boat.Type.OAK, properties);
        this.woodTypeId = woodTypeId;
        this.chestBoat = chestBoat;
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

        Boat boat = createCompatBoat(level, hitResult);
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
        String suffix = chestBoat ? " Chest Boat" : " Boat";

        if (wood == null) {
            return Component.literal(woodTypeId + suffix);
        }

        return Component.literal(wood.englishDisplayName() + suffix);
    }

    /**
     * This creates the correct vanilla boat entity type and writes the compat wood id
     * onto it before the entity is spawned into the world.
     */
    private @NotNull Boat createCompatBoat(@NotNull Level level, @NotNull HitResult hitResult) {
        Boat boat = chestBoat ? createChestBoat(level) : createBoat(level);
        boat.setPos(hitResult.getLocation().x, hitResult.getLocation().y, hitResult.getLocation().z);

        if (boat instanceof DynamicShipVariantHolder holder) {
            holder.ppbundle$setWoodTypeId(woodTypeId);
        }

        return boat;
    }

    private static @NotNull Boat createBoat(@NotNull Level level) {
        Boat boat = EntityType.BOAT.create(level);
        if (boat == null) {
            throw new IllegalStateException("Failed to create vanilla boat entity");
        }
        return boat;
    }

    private static @NotNull ChestBoat createChestBoat(@NotNull Level level) {
        ChestBoat boat = EntityType.CHEST_BOAT.create(level);
        if (boat == null) {
            throw new IllegalStateException("Failed to create vanilla chest boat entity");
        }
        return boat;
    }
}