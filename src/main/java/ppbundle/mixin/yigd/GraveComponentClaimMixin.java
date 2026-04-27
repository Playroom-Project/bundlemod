package ppbundle.mixin.yigd;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ppbundle.features.deathpoint.ftbchunks.DeathpointGate;
import ppbundle.features.deathpoint.net.DeathpointNetworking;

import java.lang.reflect.Field;
import java.util.UUID;

@Pseudo
@Mixin(targets = "com.b1n_ry.yigd.components.GraveComponent")
public class GraveComponentClaimMixin {

    @Inject(method = "claim*", at = @At("RETURN"), require = 0)
    private void ppbundle$onClaimReturn(
            ServerPlayer clicker,
            ServerLevel world,
            BlockState previousState,
            BlockPos gravePos,
            ItemStack tool,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (world == null || gravePos == null) return;

        InteractionResult res = cir.getReturnValue();
        if (res == null || !(res.consumesAction() || res == InteractionResult.SUCCESS)) return;

        UUID owner = ppbundle$resolveOwnerUuid(this);
        if (owner == null) return;

        // Finn deathpoint knyttet til akkurat denne graven
        DeathpointGate.DeathpointInfo info = DeathpointGate.onGravePicked(owner, gravePos);
        if (info == null) return;

        // Send clear til eier (må være online)
        ServerPlayer ownerPlayer = world.getServer().getPlayerList().getPlayer(owner);
        if (ownerPlayer == null) return;

        DeathpointNetworking.sendClear(ownerPlayer, info.dimension(), info.pos());
    }

    @Unique
    private static UUID ppbundle$resolveOwnerUuid(Object graveComponentInstance) {
        try {
            for (Field f : graveComponentInstance.getClass().getDeclaredFields()) {
                if (!GameProfile.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                GameProfile profile = (GameProfile) f.get(graveComponentInstance);
                return profile != null ? profile.getId() : null;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}