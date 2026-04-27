package ppbundle.mixin.ftbchunks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ppbundle.features.deathpoint.ftbchunks.DeathpointGate;

@Mixin(ServerPlayer.class)
public class ServerPlayerDieMixin {

	@Inject(method = "die", at = @At("HEAD"))
	private void ppbundle$onDieHead(net.minecraft.world.damagesource.DamageSource source, CallbackInfo ci) {
		ServerPlayer sp = (ServerPlayer) (Object) this;
		ResourceLocation dim = sp.level().dimension().location();
		DeathpointGate.beginDeath(sp.getUUID(), dim, sp.blockPosition());
	}
}