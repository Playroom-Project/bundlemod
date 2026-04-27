package ppbundle.mixin;

import dev.ftb.mods.ftbchunks.api.Protection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.ftb.mods.ftbchunks.fabric.FTBChunksExpectedImpl")
public class FTBChunksExpectedImplMixin {
	@Inject(method = "getBlockInteractProtection", at = @At("HEAD"), cancellable = true)
	private static void ppbundle_useInteractOnlyProtection(CallbackInfoReturnable<Protection> cir) {
		cir.setReturnValue(Protection.INTERACT_BLOCK);
	}
}