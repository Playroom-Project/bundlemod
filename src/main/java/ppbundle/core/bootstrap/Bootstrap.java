package ppbundle.core.bootstrap;

import ppbundle.PlayroomProjectBundlemod;
import ppbundle.core.feature.Feature;
import ppbundle.core.feature.FeatureContext;
import ppbundle.features.crates.CrateOpenFeature;
import ppbundle.features.deathpoint.DeathpointFeature;
import ppbundle.features.pickaxetrimscompat.PickaxeTrimsCompatFeature;
import ppbundle.features.protections.ProtectionsFeature;
import ppbundle.features.smallshipsvariants.SmallShipsVariantsFeature;
import ppbundle.features.vinery.VineryCompatFeature;

import java.util.List;

/**
 * Single place responsible for bootstrapping all features.

 * This keeps Fabric entrypoints clean and makes it easy to scale to many modules.
 */
public final class Bootstrap {
	private Bootstrap() {
	}

	private static final List<Feature> FEATURES = List.of(
			new ProtectionsFeature(),
			new DeathpointFeature(),
			new CrateOpenFeature(),
			new SmallShipsVariantsFeature(),
			new PickaxeTrimsCompatFeature(),
			new VineryCompatFeature()
	);

	private static FeatureContext context() {
		return new FeatureContext(PlayroomProjectBundlemod.MOD_ID, PlayroomProjectBundlemod.LOGGER);
	}

	public static void initCommon() {
		FeatureContext ctx = context();
		for (Feature f : FEATURES) {
			try {
				f.initCommon(ctx);
				ctx.logger().info("[PPBundle] Feature enabled: {}", f.id());
			} catch (Throwable t) {
				ctx.logger().error("[PPBundle] Feature failed to initialize: {}", f.id(), t);
			}
		}
	}

	public static void initClient() {
		FeatureContext ctx = context();
		for (Feature f : FEATURES) {
			try {
				f.initClient(ctx);
			} catch (Throwable t) {
				ctx.logger().error("[PPBundle] Feature failed to initialize on client: {}", f.id(), t);
			}
		}
	}
}