package ppbundle.mixin;

import dev.ftb.mods.ftbchunks.api.Protection;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClaimedChunkManagerImpl.class)
public class ClaimedChunkManagerImplMixin {

	@Inject(method = "shouldPreventInteraction", at = @At("HEAD"), cancellable = true)
	private void ppbundle_allowInteractions(Entity actor, InteractionHand hand, BlockPos pos, Protection protection, Entity targetEntity, CallbackInfoReturnable<Boolean> cir) {

		// Allow non-edit interactions everywhere (needed for trapped presents opening etc.)
		if (protection == Protection.INTERACT_BLOCK
				|| protection == Protection.INTERACT_ENTITY
				|| protection == Protection.RIGHT_CLICK_ITEM) {
			cir.setReturnValue(false);
			return;
		}

		// Allow explosions to destroy blocks in claims (FTB routes this via EDIT_* with hand == null)
		if ((protection == Protection.EDIT_BLOCK || protection == Protection.EDIT_AND_INTERACT_BLOCK)
				&& hand == null
				&& !(actor instanceof ServerPlayer)) {
			cir.setReturnValue(false);
			return;
		}

		// Allow placing specific blocks in other people's claims.
		// IMPORTANT: only allow on EDIT_AND_INTERACT_BLOCK so we don't accidentally allow mining/break based on held item.
		if (protection == Protection.EDIT_AND_INTERACT_BLOCK
				&& actor instanceof ServerPlayer sp
				&& hand != null) {

			ItemStack stack = sp.getItemInHand(hand);
			if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
				ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
				String ns = itemId.getNamespace();
				String path = itemId.getPath();

				boolean isSleepingBagItem = ns.equals("comforts") && (path.equals("sleeping_bag") || path.startsWith("sleeping_bag_"));

				boolean isPresentItem = ns.equals("supplementaries") && (
						path.equals("present") || path.startsWith("present_") || path.equals("trapped_present") || path.startsWith("trapped_present_")
				);

				if (isSleepingBagItem || isPresentItem) {
					cir.setReturnValue(false);
					return;
				}
			}
		}

		// Allow breaking/picking up Comforts sleeping bag anywhere, regardless of held item.
		// IMPORTANT: FTB may check break using EDIT_BLOCK or EDIT_AND_INTERACT_BLOCK depending on context.
		if ((protection == Protection.EDIT_BLOCK || protection == Protection.EDIT_AND_INTERACT_BLOCK)
				&& actor instanceof ServerPlayer sp2) {

			BlockState st = sp2.level().getBlockState(pos);
			Block b = st.getBlock();
			ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(b);

			if (blockId != null
					&& "comforts".equals(blockId.getNamespace())
					&& (blockId.getPath().equals("sleeping_bag") || blockId.getPath().startsWith("sleeping_bag_"))) {
				cir.setReturnValue(false);
			}
		}
	}
}