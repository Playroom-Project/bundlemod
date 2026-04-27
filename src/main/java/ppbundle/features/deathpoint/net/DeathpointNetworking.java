package ppbundle.features.deathpoint.net;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class DeathpointNetworking {
	private DeathpointNetworking() {}

	// Canonical packet id
	public static final ResourceLocation CLEAR_DEATHPOINT =
			new ResourceLocation("playroom-project-bundlemod", "deathpoint_clear");

	// Backwards/alias ids (so client file can keep using these names)
	public static final ResourceLocation S2C_DEATHPOINT_CLEAR = CLEAR_DEATHPOINT;
	public static final ResourceLocation S2C_CLEAR_DEATHPOINT_LEGACY = CLEAR_DEATHPOINT;

	/**
	 * Send clear with explicit dimension+pos (used by YiGD claim hooks).
	 */
	public static void sendClear(ServerPlayer player, ResourceLocation dimensionId, BlockPos pos) {
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		buf.writeResourceLocation(dimensionId);
		buf.writeBlockPos(pos);
		ServerPlayNetworking.send(player, CLEAR_DEATHPOINT, buf);
	}

	/**
	 * Wrapper kept for older call sites (dimension is taken from player's current level).
	 */
	public static void sendClearDeathpoint(ServerPlayer player, BlockPos pos) {
		ResourceLocation dimId = player.level().dimension().location();
		sendClear(player, dimId, pos);
	}
}