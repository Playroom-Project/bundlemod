package ppbundle.mixin.client.smallships;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ppbundle.features.smallshipsvariants.DynamicShipVariantHolder;
import ppbundle.features.smallshipsvariants.SmallShipsVariantClient;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This redirects the cached vanilla boat texture lookup used by BoatRenderer.

 * Vanilla BoatRenderer caches texture/model pairs and pulls the texture directly
 * from the cached Pair during render. Because of that, overriding
 * getTextureLocation(...) is not reliable for dynamic compat variants.

 * This redirect replaces the texture at the exact point where BoatRenderer reads
 * the cached texture from the Pair used inside render(...).
 */
@Mixin(BoatRenderer.class)
public abstract class BoatRendererTextureMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("PPBundle/BoatRenderer");

    /**
     * These one-time flags keep launcher verification visible without spamming the log.
     */
    private static final AtomicBoolean LOGGED_RENDER_PATH = new AtomicBoolean(false);
    private static final AtomicBoolean LOGGED_MISSING_HOLDER = new AtomicBoolean(false);
    private static final AtomicBoolean LOGGED_MISSING_WOOD_ID = new AtomicBoolean(false);
    private static final AtomicBoolean LOGGED_COMPAT_TEXTURE = new AtomicBoolean(false);

    @Redirect(
            method = "render(Lnet/minecraft/world/entity/vehicle/Boat;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/datafixers/util/Pair;getFirst()Ljava/lang/Object;"
            )
    )
    private Object ppbundle$useCompatBoatTextureDuringRender(
            Pair<ResourceLocation, ListModel<Boat>> pair,
            Boat boat,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource multiBufferSource,
            int packedLight
    ) {
        if (LOGGED_RENDER_PATH.compareAndSet(false, true)) {
            LOGGER.info("[BoatRenderer] Redirect is active in launcher/runtime");
        }

        if (!(boat instanceof DynamicShipVariantHolder holder)) {
            if (LOGGED_MISSING_HOLDER.compareAndSet(false, true)) {
                LOGGER.info("[BoatRenderer] Boat does not implement DynamicShipVariantHolder: {}", boat.getClass().getName());
            }
            return pair.getFirst();
        }

        String woodTypeId = holder.ppbundle$getWoodTypeId();

        if (woodTypeId == null || woodTypeId.isBlank()) {
            if (LOGGED_MISSING_WOOD_ID.compareAndSet(false, true)) {
                LOGGER.info("[BoatRenderer] Boat renderer reached a boat with missing woodTypeId");
            }
            return pair.getFirst();
        }

        ResourceLocation generated = SmallShipsVariantClient.boatTextureFor(boat, woodTypeId);

        if (generated != null) {
            if (LOGGED_COMPAT_TEXTURE.compareAndSet(false, true)) {
                LOGGER.info("[BoatRenderer] Using compat texture {}", generated);
            }
            return generated;
        }

        return pair.getFirst();
    }
}