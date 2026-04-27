package ppbundle.features.protections.ftb;

import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.FTBChunksProperties;
import dev.ftb.mods.ftbchunks.api.Protection;
import dev.ftb.mods.ftbchunks.api.ProtectionPolicy;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Custom protections used for CarryOn pick-up in claimed chunks.

 * IMPORTANT:
 * In a claimed chunk where the player lacks permissions, we must return DENY,
 * not CHECK. CHECK can allow Carry On to proceed (looks like breaking blocks).
 */
public final class PPBundleProtections {

	public static final Protection CARRYON_BLOCK = (player, pos, hand, chunk, entity) -> canEdit(player, chunk);
	public static final Protection CARRYON_ENTITY = (player, pos, hand, chunk, entity) -> canEdit(player, chunk);

	private static ProtectionPolicy canEdit(ServerPlayer player, @Nullable ClaimedChunk chunk) {
		if (chunk == null) {
			return ProtectionPolicy.CHECK;
		}

		if (chunk.getTeamData().canPlayerUse(player, FTBChunksProperties.BLOCK_EDIT_MODE)) {
			return ProtectionPolicy.ALLOW;
		}

		if (chunk.getTeamData().canPlayerUse(player, FTBChunksProperties.BLOCK_EDIT_AND_INTERACT_MODE)) {
			return ProtectionPolicy.ALLOW;
		}

		return ProtectionPolicy.DENY;
	}

	private PPBundleProtections() {
	}
}