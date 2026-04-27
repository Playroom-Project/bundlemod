package ppbundle.compat.ftbchunks;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.FTBChunksClientAPI;
import dev.ftb.mods.ftbchunks.api.client.waypoint.Waypoint;
import dev.ftb.mods.ftbchunks.api.client.waypoint.WaypointManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.Optional;

public final class FTBChunksCompatClient {

	private FTBChunksCompatClient() {}

	public static void initClient() {}

	public static void tryClearDeathpoint(ResourceLocation dimensionId, BlockPos pos) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;

		// Sikkerhet: bare i riktig dimensjon
		ResourceLocation currentDim = mc.level.dimension().location();
		if (!currentDim.equals(dimensionId)) return;

		FTBChunksClientAPI api = FTBChunksAPI.clientApi();

		// Din FTB API returnerer Optional<WaypointManager>
		Optional<WaypointManager> optManager = api.getWaypointManager(mc.level.dimension());
		if (optManager.isEmpty()) return;

		WaypointManager manager = optManager.get();

		final int maxDist = 6;
		final int maxDistSq = maxDist * maxDist;

		Optional<Waypoint> closest = manager.getAllWaypoints().stream()
				.filter(w -> w.getPos() != null)
				.filter(w -> distSq(w.getPos(), pos) <= maxDistSq)
				.min(Comparator.comparingInt(w -> distSq(w.getPos(), pos)));

		if (closest.isEmpty()) return;

		manager.removeWaypoint(closest.get());
		api.requestMinimapIconRefresh();
	}

	private static int distSq(BlockPos a, BlockPos b) {
		int dx = a.getX() - b.getX();
		int dy = a.getY() - b.getY();
		int dz = a.getZ() - b.getZ();
		return dx * dx + dy * dy + dz * dz;
	}
}