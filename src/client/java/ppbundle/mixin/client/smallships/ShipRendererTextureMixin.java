package ppbundle.mixin.client.smallships;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.talhanation.smallships.client.model.ShipModel;
import com.talhanation.smallships.client.renderer.entity.ShipRenderer;
import com.talhanation.smallships.world.entity.ship.Ship;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ppbundle.features.smallshipsvariants.DynamicShipVariantHolder;
import ppbundle.features.smallshipsvariants.SmallShipsVariantClient;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This redirects the cached ship texture lookup used by Small Ships renderers.

 * The renderer stores a cached Pair and reads the texture directly from that pair
 * during render, so redirecting the read is the most reliable way to swap in the
 * generated compat texture at runtime.
 */
@Mixin(ShipRenderer.class)
public abstract class ShipRendererTextureMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("PPBundle/ShipRenderer");

    /**
     * These one-time flags keep launcher verification visible without spamming the log.
     */
    private static final AtomicBoolean LOGGED_RENDER_PATH = new AtomicBoolean(false);
    private static final AtomicBoolean LOGGED_MISSING_HOLDER = new AtomicBoolean(false);
    private static final AtomicBoolean LOGGED_MISSING_WOOD_ID = new AtomicBoolean(false);
    private static final AtomicBoolean LOGGED_COMPAT_TEXTURE = new AtomicBoolean(false);

    @Redirect(
            method = "render(Lcom/talhanation/smallships/world/entity/ship/Ship;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/datafixers/util/Pair;getFirst()Ljava/lang/Object;"
            )
    )
    private Object ppbundle$useCompatShipTextureDuringRender(
            Pair<ResourceLocation, ShipModel<?>> pair,
            Ship ship,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource multiBufferSource,
            int packedLight
    ) {
        if (LOGGED_RENDER_PATH.compareAndSet(false, true)) {
            LOGGER.info("[ShipRenderer] Redirect is active in launcher/runtime");
        }

        if (!(ship instanceof DynamicShipVariantHolder holder)) {
            if (LOGGED_MISSING_HOLDER.compareAndSet(false, true)) {
                LOGGER.info("[ShipRenderer] Ship does not implement DynamicShipVariantHolder: {}", ship.getClass().getName());
            }
            return pair.getFirst();
        }

        String woodTypeId = holder.ppbundle$getWoodTypeId();

        if (woodTypeId == null || woodTypeId.isBlank()) {
            if (LOGGED_MISSING_WOOD_ID.compareAndSet(false, true)) {
                LOGGER.info("[ShipRenderer] Ship renderer reached a ship with missing woodTypeId");
            }
            return pair.getFirst();
        }

        ResourceLocation generated = SmallShipsVariantClient.entityTextureFor(ship, woodTypeId);

        if (generated != null) {
            if (LOGGED_COMPAT_TEXTURE.compareAndSet(false, true)) {
                LOGGER.info("[ShipRenderer] Using compat texture {}", generated);
            }
            return generated;
        }

        return pair.getFirst();
    }
}