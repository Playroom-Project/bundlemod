package ppbundle.mixin.pickaxetrims;

import dev.foxgirl.pickaxetrims.shared.effect.CryingObsidianMultiBreakEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ppbundle.features.pickaxetrimscompat.CryingObsidianMultiBreakCompat;

/**
 * This mixin replaces the original Crying Obsidian multi-break logic.

 * The extra blocks are routed through the normal server-side player mining path
 * so loot, tool requirements, enchantments, durability and XP behave like
 * standard mining.
 */
@Mixin(value = CryingObsidianMultiBreakEffect.class, remap = false)
public abstract class CryingObsidianMultiBreakEffectMixin {

    @Inject(method = "onBlockBreak", at = @At("HEAD"), cancellable = true, remap = false)
    private void ppbundle$useVanillaMiningPath(Level level, BlockPos pos, BlockState state, ServerPlayer player, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (CryingObsidianMultiBreakCompat.isActive()) {
            ci.cancel();
            return;
        }

        CryingObsidianMultiBreakCompat.breakMatchingArea(serverLevel, pos, state, player);
        ci.cancel();
    }
}