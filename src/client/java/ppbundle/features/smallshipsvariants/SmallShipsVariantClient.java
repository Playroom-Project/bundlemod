package ppbundle.features.smallshipsvariants;

import com.talhanation.smallships.world.entity.ship.BriggEntity;
import com.talhanation.smallships.world.entity.ship.CogEntity;
import com.talhanation.smallships.world.entity.ship.DrakkarEntity;
import com.talhanation.smallships.world.entity.ship.GalleyEntity;
import com.talhanation.smallships.world.entity.ship.Ship;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import ppbundle.core.feature.FeatureContext;

/**
 * This client bootstrap wires the runtime resource generator and exposes
 * stable entity texture lookups used by client render mixins.

 * The lookup methods intentionally resolve generated texture paths directly from
 * the wood id stored on the entity so rendering does not depend on late client
 * registry state during launcher or modpack startup.
 */
public final class SmallShipsVariantClient {
    private static final String SMALL_SHIPS_NAMESPACE = "smallships";

    private static boolean bootstrapped = false;

    private SmallShipsVariantClient() {
    }

    public static void bootstrap(FeatureContext ctx) {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;

        SmallShipsClientDynamicResources dynamicResources = SmallShipsClientDynamicResources.getInstance();
        dynamicResources.register(ctx);
        dynamicResources.rebuildCache(ctx.logger());

        ctx.logger().info("[PPBundle] Small Ships client compat initialized");
    }

    /**
     * This resolves the generated Small Ships ship texture directly from the
     * stored wood id and ship class.

     * Returning null tells the renderer to keep the original texture.
     */
    public static ResourceLocation entityTextureFor(Ship ship, String woodTypeId) {
        if (ship == null) {
            return null;
        }

        if (!isSupportedShipKind(ship)) {
            return null;
        }

        String flattenedWoodName = flattenedWoodName(woodTypeId);
        if (flattenedWoodName == null) {
            return null;
        }

        return generatedShipEntityTextureId(flattenedWoodName);
    }

    /**
     * This resolves the generated vanilla boat or chest boat texture directly
     * from the stored wood id.

     * Returning null tells the renderer to keep the original texture.
     */
    public static ResourceLocation boatTextureFor(Boat boat, String woodTypeId) {
        if (boat == null) {
            return null;
        }

        String flattenedWoodName = flattenedWoodName(woodTypeId);
        if (flattenedWoodName == null) {
            return null;
        }

        if (boat instanceof ChestBoat) {
            return generatedChestBoatEntityTextureId(flattenedWoodName);
        }

        return generatedBoatEntityTextureId(flattenedWoodName);
    }

    /**
     * This keeps all generated entity texture paths centralized so render lookup
     * and runtime pack generation stay aligned.
     */
    public static ResourceLocation generatedBoatEntityTextureId(VariantWoodType wood) {
        return generatedBoatEntityTextureId(wood.flattenedName());
    }

    /**
     * This keeps all generated chest boat entity texture paths centralized so render lookup
     * and runtime pack generation stay aligned.
     */
    public static ResourceLocation generatedChestBoatEntityTextureId(VariantWoodType wood) {
        return generatedChestBoatEntityTextureId(wood.flattenedName());
    }

    /**
     * This keeps all generated ship entity texture paths centralized so render lookup
     * and runtime pack generation stay aligned.
     */
    public static ResourceLocation generatedShipEntityTextureId(VariantWoodType wood) {
        return generatedShipEntityTextureId(wood.flattenedName());
    }

    /**
     * This creates the generated boat texture path from a flattened wood name.
     */
    public static ResourceLocation generatedBoatEntityTextureId(String flattenedWoodName) {
        return new ResourceLocation(
                SMALL_SHIPS_NAMESPACE,
                "textures/entity/boat/" + flattenedWoodName + ".png"
        );
    }

    /**
     * This creates the generated chest boat texture path from a flattened wood name.
     */
    public static ResourceLocation generatedChestBoatEntityTextureId(String flattenedWoodName) {
        return new ResourceLocation(
                SMALL_SHIPS_NAMESPACE,
                "textures/entity/chest_boat/" + flattenedWoodName + ".png"
        );
    }

    /**
     * This creates the generated ship texture path from a flattened wood name.
     */
    public static ResourceLocation generatedShipEntityTextureId(String flattenedWoodName) {
        return new ResourceLocation(
                SMALL_SHIPS_NAMESPACE,
                "textures/entity/ship/" + flattenedWoodName + ".png"
        );
    }

    /**
     * This verifies that the entity is one of the supported Small Ships ship classes.
     */
    private static boolean isSupportedShipKind(Ship ship) {
        return ship instanceof CogEntity
                || ship instanceof BriggEntity
                || ship instanceof GalleyEntity
                || ship instanceof DrakkarEntity;
    }

    /**
     * This converts a stored canonical wood id into the same flattened name used
     * by runtime-generated asset paths.

     * The registry is used when available, but the method also reconstructs the
     * same flattened output directly from the id so launcher rendering does not
     * depend on registry timing.
     */
    private static String flattenedWoodName(String woodTypeId) {
        if (woodTypeId == null || woodTypeId.isBlank()) {
            return null;
        }

        VariantWoodType knownWood = SmallShipsVariantRegistry.wood(woodTypeId);
        if (knownWood != null) {
            String flattened = knownWood.flattenedName();
            if (flattened != null && !flattened.isBlank()) {
                return flattened;
            }
        }

        ResourceLocation parsed = ResourceLocation.tryParse(woodTypeId);
        if (parsed != null) {
            return parsed.getNamespace() + "_" + parsed.getPath().replace('/', '_');
        }

        return woodTypeId
                .replace(':', '_')
                .replace('/', '_')
                .replace('\\', '_');
    }
}