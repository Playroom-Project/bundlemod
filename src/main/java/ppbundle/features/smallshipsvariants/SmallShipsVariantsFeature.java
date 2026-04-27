package ppbundle.features.smallshipsvariants;

import net.fabricmc.loader.api.FabricLoader;
import ppbundle.core.feature.Feature;
import ppbundle.core.feature.FeatureContext;

/**
 * This feature enables Small Ships compat only when the target mod is present.

 * Common bootstrap must run before client bootstrap so the discovered wood registry,
 * compat items and runtime-generated resources already exist when client assets are built.

 * The common source set does not directly link to client-only classes, so the client
 * bootstrap is invoked through reflection.
 */
public final class SmallShipsVariantsFeature implements Feature {
    private static final String SMALL_SHIPS_MOD_ID = "smallships";
    private static final String CLIENT_BOOTSTRAP_CLASS =
            "ppbundle.features.smallshipsvariants.SmallShipsVariantClient";

    private static boolean commonInitialized = false;
    private static boolean clientInitialized = false;

    @Override
    public String id() {
        return "smallships_variants";
    }

    @Override
    public void initCommon(FeatureContext ctx) {
        if (!isSmallShipsLoaded()) {
            return;
        }

        if (commonInitialized) {
            return;
        }
        commonInitialized = true;

        SmallShipsVariantRegistry.bootstrap(ctx);

        SmallShipsCommonDynamicResources dynamicResources = SmallShipsCommonDynamicResources.getInstance();
        dynamicResources.register(ctx);
        dynamicResources.rebuildCache(ctx.logger());

        ctx.logger().info("[PPBundle] Small Ships common compat initialized");
    }

    @Override
    public void initClient(FeatureContext ctx) {
        if (!isSmallShipsLoaded()) {
            return;
        }

        if (clientInitialized) {
            return;
        }
        clientInitialized = true;

        if (!commonInitialized) {
            SmallShipsVariantRegistry.bootstrap(ctx);

            SmallShipsCommonDynamicResources dynamicResources = SmallShipsCommonDynamicResources.getInstance();
            dynamicResources.register(ctx);
            dynamicResources.rebuildCache(ctx.logger());

            commonInitialized = true;
            ctx.logger().warn("[PPBundle] Small Ships common compat was initialized from client bootstrap");
        }

        try {
            Class<?> clientClass = Class.forName(CLIENT_BOOTSTRAP_CLASS);
            clientClass.getMethod("bootstrap", FeatureContext.class).invoke(null, ctx);
        } catch (Throwable t) {
            ctx.logger().error("[PPBundle] Failed to initialize Small Ships client compat", t);
        }
    }

    private static boolean isSmallShipsLoaded() {
        return FabricLoader.getInstance().isModLoaded(SMALL_SHIPS_MOD_ID);
    }
}