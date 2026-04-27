package ppbundle.features.crates;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import ppbundle.core.feature.Feature;
import ppbundle.core.feature.FeatureContext;

import java.lang.reflect.Method;
import java.util.List;

/**
 * SHIFT + RIGHT CLICK crate opening:
 * - Go Fish: open even when targeting a block (reroute to Item#use).
 * - Hybrid Aquatic: open from item-in-hand AND from placed crate blocks.
 */
public final class CrateOpenFeature implements Feature {

    private static final String GO_FISH_MOD_ID = "go-fish";
    private static final String HYBRID_AQUATIC_MOD_ID = "hybrid-aquatic";

    private static final String GO_FISH_CRATE_ITEM_CLASS = "draylar.gofish.item.CrateItem";

    @Override
    public String id() {
        return "crate_open";
    }

    @Override
    public void initCommon(FeatureContext ctx) {
        // Right-click in air (item use)
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!player.isShiftKeyDown()) {
                return InteractionResultHolder.pass(player.getItemInHand(hand));
            }

            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty()) {
                return InteractionResultHolder.pass(stack);
            }

            // Hybrid Aquatic: open from item-in-hand (Go Fish style)
            if (isHybridAquaticCrateItem(stack)) {
                if (!world.isClientSide) {
                    openHybridAquaticLoot((ServerPlayer) player, (ServerLevel) world, hybridAquaticCrateLoot(stackIdPath(stack)));
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                }
                return InteractionResultHolder.sidedSuccess(stack, world.isClientSide);
            }

            // Go Fish: DON'T consume the callback here.
            // Let vanilla call CrateItem#use so it opens in air.
            if (isGoFishCrateItem(stack)) {
                return InteractionResultHolder.pass(stack);
            }

            return InteractionResultHolder.pass(stack);
        });

        // Right-click on a block
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }

            // Hybrid Aquatic: open placed crate blocks on sneak-use
            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);
            if (isHybridAquaticCrateBlock(state)) {
                if (!world.isClientSide) {
                    ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    if (blockId != null) {
                        ResourceLocation lootId = hybridAquaticCrateLoot(blockId.getPath());
                        if (lootId != null) {
                            // Remove block first to prevent double interactions
                            world.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                            openHybridAquaticLoot((ServerPlayer) player, (ServerLevel) world, lootId);
                        }
                    }
                }
                return InteractionResult.SUCCESS;
            }

            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty()) {
                return InteractionResult.PASS;
            }

            // Hybrid Aquatic: open from item even when targeting a block
            if (isHybridAquaticCrateItem(stack)) {
                if (!world.isClientSide) {
                    openHybridAquaticLoot((ServerPlayer) player, (ServerLevel) world, hybridAquaticCrateLoot(stackIdPath(stack)));
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                }
                return InteractionResult.SUCCESS;
            }

            // Go Fish: when targeting a block, the mod returns FAIL to prevent placement.
            // We reroute to Item#use to actually open it.
            if (isGoFishCrateItem(stack)) {
                if (!world.isClientSide) {
                    invokeGoFishCrateUse(player, world, hand);
                }
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        });

        ctx.logger().info("[PPBundle] CrateOpenFeature loaded (Go Fish + Hybrid Aquatic compat).");
    }

    private static boolean isGoFishCrateItem(ItemStack stack) {
        if (!FabricLoader.getInstance().isModLoaded(GO_FISH_MOD_ID)) {
            return false;
        }
        try {
            Class<?> crateClass = Class.forName(GO_FISH_CRATE_ITEM_CLASS);
            return crateClass.isInstance(stack.getItem());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void invokeGoFishCrateUse(Player player, Level world, InteractionHand hand) {
        try {
            Item item = player.getItemInHand(hand).getItem();
            Method use = item.getClass().getMethod("use", Level.class, Player.class, InteractionHand.class);
            use.invoke(item, world, player, hand);
        } catch (Throwable ignored) {
        }
    }

    private static boolean isHybridAquaticCrateItem(ItemStack stack) {
        if (!FabricLoader.getInstance().isModLoaded(HYBRID_AQUATIC_MOD_ID)) {
            return false;
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null || !HYBRID_AQUATIC_MOD_ID.equals(id.getNamespace())) {
            return false;
        }

        String path = id.getPath();
        return path.endsWith("_crate") || "crab_pot".equals(path);
    }

    private static boolean isHybridAquaticCrateBlock(BlockState state) {
        if (!FabricLoader.getInstance().isModLoaded(HYBRID_AQUATIC_MOD_ID)) {
            return false;
        }

        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id == null || !HYBRID_AQUATIC_MOD_ID.equals(id.getNamespace())) {
            return false;
        }

        String path = id.getPath();
        return path.endsWith("_crate") || "crab_pot".equals(path);
    }

    private static String stackIdPath(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.getPath();
    }

    private static void openHybridAquaticLoot(ServerPlayer player, ServerLevel level, ResourceLocation lootId) {
        if (lootId == null) {
            return;
        }

        LootTable table = level.getServer().getLootData().getLootTable(lootId);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, player.position())
                .withOptionalParameter(LootContextParams.THIS_ENTITY, player)
                .create(LootContextParamSets.CHEST);

        List<ItemStack> drops = table.getRandomItems(params);
        for (ItemStack drop : drops) {
            if (!player.getInventory().add(drop)) {
                player.drop(drop, false);
            }
        }
    }

    private static ResourceLocation hybridAquaticCrateLoot(String path) {
        return switch (path) {
            case "crab_pot" -> new ResourceLocation(HYBRID_AQUATIC_MOD_ID, "gameplay/crab_pot_treasure");
            case "hybrid_crate" -> new ResourceLocation(HYBRID_AQUATIC_MOD_ID, "gameplay/hybrid_crate_treasure");
            case "oak_crate" -> new ResourceLocation(HYBRID_AQUATIC_MOD_ID, "gameplay/oak_crate_treasure");
            case "spruce_crate" -> new ResourceLocation(HYBRID_AQUATIC_MOD_ID, "gameplay/spruce_crate_treasure");
            case "birch_crate" -> new ResourceLocation(HYBRID_AQUATIC_MOD_ID, "gameplay/birch_crate_treasure");
            case "jungle_crate" -> new ResourceLocation(HYBRID_AQUATIC_MOD_ID, "gameplay/jungle_crate_treasure");
            case "bamboo_crate" -> new ResourceLocation(HYBRID_AQUATIC_MOD_ID, "gameplay/bamboo_crate_treasure");
            case "acacia_crate" -> new ResourceLocation(HYBRID_AQUATIC_MOD_ID, "gameplay/acacia_crate_treasure");
            case "dark_oak_crate" -> new ResourceLocation(HYBRID_AQUATIC_MOD_ID, "gameplay/dark_oak_crate_treasure");
            case "mangrove_crate" -> new ResourceLocation(HYBRID_AQUATIC_MOD_ID, "gameplay/mangrove_crate_treasure");
            case "cherry_crate" -> new ResourceLocation(HYBRID_AQUATIC_MOD_ID, "gameplay/cherry_crate_treasure");
            default -> null;
        };
    }
}