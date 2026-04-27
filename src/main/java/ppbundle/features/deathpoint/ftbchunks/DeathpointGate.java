package ppbundle.features.deathpoint.ftbchunks;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks mapping between a placed grave block position and the deathpoint (dim + pos),
 * so we can clear the correct FTB death waypoint when the grave is claimed.
 *
 * We DO NOT gate/cancel FTB deathpoint packets anymore (to avoid hiding older deaths).
 */
public final class DeathpointGate {

	public record DeathpointInfo(ResourceLocation dimension, BlockPos pos) {}

	// Last death info per player (for binding a later grave position).
	private static final Map<UUID, DeathpointInfo> LAST_DEATH = new ConcurrentHashMap<>();

	// Per player: gravePos -> deathpoint info
	private static final Map<UUID, Map<BlockPos, DeathpointInfo>> GRAVE_TO_DEATH = new ConcurrentHashMap<>();

	private DeathpointGate() {}

	/**
	 * Called at death time. Stores last death (dim + pos) for binding to the next grave placement.
	 */
	public static void beginDeath(UUID playerId, ResourceLocation dimension, BlockPos deathPos) {
		if (playerId == null || dimension == null || deathPos == null) return;
		LAST_DEATH.put(playerId, new DeathpointInfo(dimension, deathPos.immutable()));
	}

	/**
	 * Called when YiGD successfully placed/loaded a grave for this death.
	 * Binds grave position to the last death position so we can clear only that marker later.
	 */
	public static void onGravePlaced(UUID playerId, BlockPos gravePos) {
		if (playerId == null || gravePos == null) return;

		DeathpointInfo info = LAST_DEATH.get(playerId);
		if (info == null) return;

		GRAVE_TO_DEATH
				.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
				.put(gravePos.immutable(), info);
	}

	/**
	 * Called when a specific grave is claimed/picked up.
	 * Returns the bound deathpoint info and removes the binding.
	 */
	public static DeathpointInfo onGravePicked(UUID playerId, BlockPos gravePos) {
		if (playerId == null || gravePos == null) return null;

		Map<BlockPos, DeathpointInfo> map = GRAVE_TO_DEATH.get(playerId);
		if (map == null) return null;

		DeathpointInfo removed = map.remove(gravePos);
		if (map.isEmpty()) {
			GRAVE_TO_DEATH.remove(playerId);
		}
		return removed;
	}

	/**
	 * Kept for compatibility with existing mixin hooks.
	 * Always allow FTB to create death waypoints (prevents older deaths being hidden).
	 */
	public static boolean consumeShouldAllow(UUID playerId) {
		return true;
	}
}