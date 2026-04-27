package ppbundle.features.vinery;

import net.fabricmc.loader.api.FabricLoader;
import ppbundle.core.feature.Feature;
import ppbundle.core.feature.FeatureContext;

/**
 * This feature enables the Vinery compat layer only when the target mod is present.
 */
public final class VineryCompatFeature implements Feature {
    private static final String MOD_ID = "vinery";

    @Override
    public String id() {
        return "vinery_compat";
    }

    @Override
    public void initCommon(FeatureContext ctx) {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return;
        }

        ctx.logger().info("[PPBundle] Vinery compat initialized");
    }
}