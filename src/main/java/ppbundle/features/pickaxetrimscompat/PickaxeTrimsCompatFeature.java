package ppbundle.features.pickaxetrimscompat;

import net.fabricmc.loader.api.FabricLoader;
import ppbundle.core.feature.Feature;
import ppbundle.core.feature.FeatureContext;

/**
 * This feature enables the Pickaxe Trims compat layer only when the target mod is present.

 * The actual fix lives in a mixin and static helper so the bundle keeps the same
 * modular structure as the other PPBundle features.
 */
public final class PickaxeTrimsCompatFeature implements Feature {
    private static final String MOD_ID = "pickaxetrims";

    @Override
    public String id() {
        return "pickaxetrims_compat";
    }

    @Override
    public void initCommon(FeatureContext ctx) {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return;
        }

        ctx.logger().info("[PPBundle] Pickaxe Trims compat initialized");
    }
}