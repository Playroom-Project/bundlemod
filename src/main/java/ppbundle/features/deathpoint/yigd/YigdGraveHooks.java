package ppbundle.features.deathpoint.yigd;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import ppbundle.features.deathpoint.ftbchunks.DeathpointGate;
import ppbundle.features.deathpoint.net.DeathpointNetworking;

/**
 * Optional hook (if you use it) to clear only the matching deathpoint when a YiGD grave is used.
 *
 * NOTE:
 * This does NOT remove all deathpoints; it only clears the one bound to this grave position.
 *
 * If you are using a GraveBlock mixin instead, you can keep this class unused.
 */
public final class YigdGraveHooks {

    private static final ResourceLocation YIGD_GRAVE_BLOCK_ID = new ResourceLocation("yigd", "grave");

    private YigdGraveHooks() {}

    public static void init() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (level.isClientSide) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

            BlockPos gravePos = hitResult.getBlockPos();
            BlockState state = level.getBlockState(gravePos);

            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (!YIGD_GRAVE_BLOCK_ID.equals(id)) return InteractionResult.PASS;

            // Only clear if we have a binding for THIS grave position.
            DeathpointGate.DeathpointInfo info = DeathpointGate.onGravePicked(sp.getUUID(), gravePos);
            if (info != null) {
                DeathpointNetworking.sendClear(sp, info.dimension(), info.pos());
            }

            return InteractionResult.PASS;
        });
    }
}