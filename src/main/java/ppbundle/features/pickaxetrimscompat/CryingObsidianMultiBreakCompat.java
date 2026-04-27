package ppbundle.features.pickaxetrimscompat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * This helper replaces the broken multi-break behavior from Better Pickaxe Trims.

 * The original mod removes extra blocks through a generic world break call, which
 * skips the normal player mining path for loot/tool handling. This compat layer
 * breaks the surrounding blocks through the player's server game mode instead,
 * so drops, enchantments, tool requirements, experience and durability behave
 * like normal mining.
 */
public final class CryingObsidianMultiBreakCompat {
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

    private static final int DEFAULT_RADIUS = 1;

    private CryingObsidianMultiBreakCompat() {
    }

    /**
     * Returns true while the compat layer is already breaking extra blocks.

     * Side-block mining triggers normal break callbacks again, so the guard prevents
     * recursive 3x3 expansion and also suppresses the original broken effect.
     */
    public static boolean isActive() {
        return ACTIVE.get();
    }

    /**
     * Breaks the surrounding matching blocks through the normal server-side player
     * mining path. The center block is intentionally excluded because vanilla is
     * already handling the original block break.
     */
    public static void breakMatchingArea(ServerLevel level, BlockPos origin, BlockState originState, ServerPlayer player) {
        if (isActive()) {
            return;
        }

        int radius = resolveConfiguredRadius();
        if (radius <= 0) {
            return;
        }

        Block originBlock = originState.getBlock();

        ACTIVE.set(true);
        try {
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (x == 0 && y == 0 && z == 0) {
                            continue;
                        }

                        BlockPos targetPos = origin.offset(x, y, z);
                        BlockState targetState = level.getBlockState(targetPos);

                        if (targetState.isAir()) {
                            continue;
                        }

                        if (targetState.getBlock() != originBlock) {
                            continue;
                        }

                        if (!player.mayInteract(level, targetPos)) {
                            continue;
                        }

                        player.gameMode.destroyBlock(targetPos);
                    }
                }
            }
        } finally {
            ACTIVE.set(false);
        }
    }

    /**
     * Reads the Pickaxe Trims config at runtime without taking a hard compile-time
     * dependency on that mod. This keeps PPBundle self-contained and safe to load
     * even when Pickaxe Trims is absent.
     */
    private static int resolveConfiguredRadius() {
        try {
            Class<?> implClass = Class.forName("dev.foxgirl.pickaxetrims.shared.PickaxeTrimsImpl");
            Method getInstance = implClass.getDeclaredMethod("getInstance");
            Object instance = getInstance.invoke(null);
            if (instance == null) {
                return DEFAULT_RADIUS;
            }

            Field configField = implClass.getDeclaredField("config");
            configField.setAccessible(true);
            Object config = configField.get(instance);
            if (config == null) {
                return DEFAULT_RADIUS;
            }

            Field radiusField = config.getClass().getDeclaredField("cryingObsidianMultiBreakRadius");
            radiusField.setAccessible(true);

            int radius = radiusField.getInt(config);
            return Math.max(0, radius);
        } catch (Throwable ignored) {
            return DEFAULT_RADIUS;
        }
    }
}