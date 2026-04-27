package ppbundle;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import ppbundle.compat.ftbchunks.FTBChunksDeathpointVisibilityFix;
import ppbundle.core.bootstrap.Bootstrap;
import ppbundle.core.feature.FeatureContext;
import ppbundle.features.deathpoint.net.DeathpointClientNetworking;
import ppbundle.features.smallshipsvariants.SmallShipsVariantClient;

/**
 * This is the earliest PPBundle client entrypoint.

 * Small Ships client compat is initialized here before the generic feature bootstrap
 * so its runtime resource registration is in place as early as possible during client startup.

 * The generic feature bootstrap still runs afterwards for the rest of the client features.
 * Small Ships client bootstrap is idempotent, so the later feature pass is safe.
 */
public class PlayroomProjectBundlemodClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		FeatureContext ctx = new FeatureContext(
				PlayroomProjectBundlemod.MOD_ID,
				PlayroomProjectBundlemod.LOGGER
		);

		SmallShipsVariantClient.bootstrap(ctx);

		Bootstrap.initClient();
		DeathpointClientNetworking.init();

		ClientTickEvents.END_CLIENT_TICK.register((Minecraft client) -> {
			FTBChunksDeathpointVisibilityFix.onClientTick(client);
		});
	}
}