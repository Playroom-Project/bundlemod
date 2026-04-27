package ppbundle.mixin.yigd;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ppbundle.features.deathpoint.ftbchunks.DeathpointGate;

import java.lang.reflect.Field;
import java.util.UUID;

@Pseudo
@Mixin(targets = "com.b1n_ry.yigd.components.GraveComponent")
public class GraveComponentPlaceAndLoadMixin {

    @Inject(method = "placeAndLoad", at = @At("RETURN"))
    private void ppbundle$onPlaceAndLoadReturn(
            Direction dir,
            @Coerce Object deathContext,
            BlockPos pos,
            ServerLevel world,
            @Coerce Object respawnComponent,
            CallbackInfo ci
    ) {
        UUID owner = ppbundle$resolveOwnerUuid(this);
        if (owner != null && pos != null) {
            DeathpointGate.onGravePlaced(owner, pos);
        }
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