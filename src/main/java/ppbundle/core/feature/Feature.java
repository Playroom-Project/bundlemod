package ppbundle.core.feature;

/**
 * A self-contained module inside the bundle mod.
 *
 * Rules:
 * - initCommon() must be safe to run on dedicated servers.
 * - initClient() is only called from the Fabric client entrypoint.
 */
public interface Feature {
	/**
	 * Stable feature id used for logging/config keys.
	 */
	String id();

	/**
	 * Called during mod init on both physical server and client.
	 */
	void initCommon(FeatureContext ctx);

	/**
	 * Called only on the physical client.
	 */
	default void initClient(FeatureContext ctx) {
	}
}