package ppbundle.mixin.vinery;

import net.minecraft.world.entity.npc.VillagerTrades;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;

/**
 * This patches the wandering winemaker trade pool after Vinery creates it.

 * The mixin keeps the original trade structure and rebuilds the level one trade list
 * with the same trade type that Vinery already uses.
 */
@Pseudo
@Mixin(targets = "net.satisfy.vinery.core.entity.WanderingWinemakerEntity")
public abstract class WanderingWinemakerEntityMixin {

    @Shadow
    @Final
    @Mutable
    public static HashMap<Integer, VillagerTrades.ItemListing[]> TRADES;

    @Inject(method = "createTrades", at = @At("RETURN"), cancellable = true)
    private static void ppbundle$patchTrades(CallbackInfoReturnable<HashMap<Integer, VillagerTrades.ItemListing[]>> cir) {
        HashMap<Integer, VillagerTrades.ItemListing[]> trades = cir.getReturnValue();
        VillagerTrades.ItemListing[] levelOneTrades = trades.get(1);

        if (levelOneTrades == null || levelOneTrades.length == 0) {
            return;
        }

        trades.put(1, ppbundle$createFixedLevelOneTrades());
        cir.setReturnValue(trades);
        TRADES = trades;
    }

    /**
     * This recreates the level one wandering winemaker trade pool.

     * Trade value meanings:
     * - first int: emerald price paid by the player
     * - second int: amount sold to the player
     * - third int: maximum number of completed trades
     * - fourth int: trader experience gained per completed trade
     */
    @Unique
    private static VillagerTrades.ItemListing[] ppbundle$createFixedLevelOneTrades() {
        return new VillagerTrades.ItemListing[]{
                new VillagerTrades.ItemsForEmeralds(net.satisfy.vinery.core.registry.ObjectRegistry.RED_GRAPE_SEEDS.get(), 1, 1, 8, 1),
                new VillagerTrades.ItemsForEmeralds(net.satisfy.vinery.core.registry.ObjectRegistry.WHITE_GRAPE_SEEDS.get(), 1, 1, 8, 1),
                new VillagerTrades.ItemsForEmeralds(net.satisfy.vinery.core.registry.ObjectRegistry.TAIGA_RED_GRAPE_SEEDS.get(), 1, 1, 8, 1),
                new VillagerTrades.ItemsForEmeralds(net.satisfy.vinery.core.registry.ObjectRegistry.TAIGA_WHITE_GRAPE_SEEDS.get(), 1, 1, 8, 1),
                new VillagerTrades.ItemsForEmeralds(net.satisfy.vinery.core.registry.ObjectRegistry.SAVANNA_RED_GRAPE_SEEDS.get(), 1, 1, 8, 1),
                new VillagerTrades.ItemsForEmeralds(net.satisfy.vinery.core.registry.ObjectRegistry.SAVANNA_WHITE_GRAPE_SEEDS.get(), 1, 1, 8, 1),
                new VillagerTrades.ItemsForEmeralds(net.satisfy.vinery.core.registry.ObjectRegistry.JUNGLE_RED_GRAPE_SEEDS.get(), 1, 1, 8, 1),
                new VillagerTrades.ItemsForEmeralds(net.satisfy.vinery.core.registry.ObjectRegistry.JUNGLE_WHITE_GRAPE_SEEDS.get(), 1, 1, 8, 1),
                new VillagerTrades.ItemsForEmeralds(net.satisfy.vinery.core.registry.ObjectRegistry.DARK_CHERRY_SAPLING.get(), 3, 1, 8, 1),
                new VillagerTrades.ItemsForEmeralds(net.satisfy.vinery.core.registry.ObjectRegistry.APPLE_TREE_SAPLING.get(), 5, 1, 8, 1),
                new VillagerTrades.ItemsForEmeralds(net.satisfy.vinery.core.registry.ObjectRegistry.RED_GRAPE.get(), 2, 1, 8, 1),
                new VillagerTrades.ItemsForEmeralds(net.satisfy.vinery.core.registry.ObjectRegistry.RED_GRAPEJUICE.get(), 4, 1, 8, 1),
                new VillagerTrades.ItemsForEmeralds(net.satisfy.vinery.core.registry.ObjectRegistry.WHITE_GRAPEJUICE.get(), 4, 1, 8, 1),
                new VillagerTrades.ItemsForEmeralds(net.satisfy.vinery.core.registry.ObjectRegistry.RED_SAVANNA_GRAPEJUICE.get(), 4, 1, 8, 1),
                new VillagerTrades.ItemsForEmeralds(net.satisfy.vinery.core.registry.ObjectRegistry.WHITE_TAIGA_GRAPEJUICE.get(), 4, 1, 8, 1),
                new VillagerTrades.ItemsForEmeralds(net.satisfy.vinery.core.registry.ObjectRegistry.RED_JUNGLE_GRAPEJUICE.get(), 4, 1, 8, 1),

                new VillagerTrades.ItemsForEmeralds(vectorwing.farmersdelight.common.registry.ModItems.ORGANIC_COMPOST.get(), 1, 6, 8, 1),
                new VillagerTrades.ItemsForEmeralds(vectorwing.farmersdelight.common.registry.ModItems.ORGANIC_COMPOST.get(), 1, 6, 8, 1),

                new VillagerTrades.ItemsForEmeralds(net.satisfy.vinery.core.registry.ObjectRegistry.DARK_CHERRY_PLANKS.get(), 3, 4, 8, 1),
                new VillagerTrades.ItemsForEmeralds(net.satisfy.vinery.core.registry.ObjectRegistry.CHERRY_WINE.get(), 1, 1, 8, 1)
        };
    }
}