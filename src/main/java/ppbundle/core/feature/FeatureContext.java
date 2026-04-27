package ppbundle.core.feature;

import org.slf4j.Logger;

/**
 * Shared services given to each feature.
 * Keep this class small and stable; add things here only when multiple features benefit.
 */
public final class FeatureContext {
	private final String modId;
	private final Logger logger;

	public FeatureContext(String modId, Logger logger) {
		this.modId = modId;
		this.logger = logger;
	}

	public String modId() {
		return modId;
	}

	public Logger logger() {
		return logger;
	}
}