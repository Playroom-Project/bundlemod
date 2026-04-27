package ppbundle.mixin.ftbchunks;

import dev.architectury.networking.simple.BaseS2CMessage;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ppbundle.features.deathpoint.ftbchunks.DeathpointGate;

@Mixin(BaseS2CMessage.class)
public abstract class BaseS2CMessageSendToMixin {

	@Unique
	private static final String PPBUNDLE_FTB_PLAYER_DEATH_PACKET = "dev.ftb.mods.ftbchunks.net.PlayerDeathPacket";

	@Inject(method = "sendTo(Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("HEAD"), cancellable = true)
	private void ppbundle$gateFtbDeathPacket(ServerPlayer player, CallbackInfo ci) {
		if (player == null) return;

		if (!this.getClass().getName().equals(PPBUNDLE_FTB_PLAYER_DEATH_PACKET)) {
			return;
		}

		boolean allow = DeathpointGate.consumeShouldAllow(player.getUUID());
		if (!allow) {
			ci.cancel();
		}
	}
}