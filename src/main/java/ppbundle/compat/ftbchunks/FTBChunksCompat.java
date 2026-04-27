package ppbundle.compat.ftbchunks;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-only implementation lives in src/client/java.
 * This stub exists so common code can reference the class without pulling in client classes on the server.
 */
public final class FTBChunksCompat {
	private FTBChunksCompat() {
	}

	public static void tryClearDeathpoint(ResourceLocation dimensionId, BlockPos pos) {
		// no-op on dedicated server
	}
}