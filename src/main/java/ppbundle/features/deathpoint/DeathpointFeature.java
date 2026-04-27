package ppbundle.features.deathpoint;

import ppbundle.core.feature.Feature;
import ppbundle.core.feature.FeatureContext;

/**
 * Server-side deathpoint feature (client receiver init is done in the client entrypoint).
 */
public final class DeathpointFeature implements Feature {

	@Override
	public String id() {
		return "deathpoint";
	}

	@Override
	public void initCommon(FeatureContext ctx) {
		// Networking IDs live in ppbundle.features.deathpoint.net.DeathpointNetworking
		// Server sends packets when a grave is picked up.
	}
}