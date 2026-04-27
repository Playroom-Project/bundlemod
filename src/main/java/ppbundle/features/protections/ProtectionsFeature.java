package ppbundle.features.protections;

import dev.ftb.mods.ftbchunks.api.Protection;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import ppbundle.core.feature.Feature;
import ppbundle.core.feature.FeatureContext;
import ppbundle.features.protections.config.InteractionBlacklistConfig;
import ppbundle.features.protections.ftb.PPBundleProtections;

public class ProtectionsFeature implements Feature {
	private static volatile InteractionBlacklistConfig BLACKLIST;

	@Override
	public String id() {
		return "protections";
	}

	@Override
	public void initCommon(FeatureContext ctx) {
		BLACKLIST = InteractionBlacklistConfig.loadOrCreate();

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((livingEntity, damageSource, amount) -> allowLivingDamage(livingEntity, damageSource));

		UseItemCallback.EVENT.register((player, world, hand) -> {
			InteractionResultHolder<ItemStack> r = maybeDenyItemUse(player, world, hand);
			return r == null ? InteractionResultHolder.pass(player.getItemInHand(hand)) : r;
		});

		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			BlockPos pos = hitResult.getBlockPos();

			// Allow special placement in other people's claims (sleeping bag + presents)
			InteractionResult specialPlace = maybeAllowSpecialPlace(player, world, hand, hitResult);
			if (specialPlace != null) {
				return specialPlace;
			}

			InteractionResult carry = maybeBlockCarry(player, world, hand, pos);
			if (carry != null) {
				if (!world.isClientSide && carry == InteractionResult.FAIL) {
					ppbundle$resyncAfterDeniedBlockUse(player, world, hitResult);
				}
				return carry;
			}

			InteractionResult special = maybeDenyBlockUse(player, world, hand, hitResult);
			if (special != null) {
				if (!world.isClientSide && special == InteractionResult.FAIL) {
					ppbundle$resyncAfterDeniedBlockUse(player, world, hitResult);
				}
				return special;
			}

			return InteractionResult.PASS;
		});

		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			InteractionResult carry = maybeEntityCarry(player, world, hand, entity);
			if (carry != null) return carry;

			InteractionResult special = maybeDenyEntityUse(player, world, hand, entity);
			return special == null ? InteractionResult.PASS : special;
		});

		// HARD BLOCK-BREAK PROTECTION (stops real mining / left click breaking)
		PlayerBlockBreakEvents.BEFORE.register((Level world, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) -> {
			if (world.isClientSide) return true;
			if (!FabricLoader.getInstance().isModLoaded("ftbchunks")) return true;
			if (!(player instanceof ServerPlayer sp)) return true;

			// Sleeping bag is allowed to break anywhere (even in others' claims)
			if (isComfortsSleepingBagBlock(state)) {
				return true;
			}

			// Presents must NOT be breakable without permissions (no whitelist here)

			ClaimedChunkManagerImpl manager = ClaimedChunkManagerImpl.getInstance();
			if (manager == null) return true;

			boolean prevent = manager.shouldPreventInteraction(
					sp,
					InteractionHand.MAIN_HAND,
					pos,
					Protection.EDIT_BLOCK,
					null
			);

			if (prevent) {
				sp.connection.send(new ClientboundBlockUpdatePacket(world, pos));
				sp.inventoryMenu.sendAllDataToRemote();
				return false;
			}

			return true;
		});
	}

	private static InteractionResult maybeAllowSpecialPlace(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
		if (world.isClientSide) return null;
		if (!FabricLoader.getInstance().isModLoaded("ftbchunks")) return null;
		if (!(player instanceof ServerPlayer sp)) return null;

		ItemStack stack = sp.getItemInHand(hand);
		if (stack.isEmpty()) return null;
		if (!(stack.getItem() instanceof BlockItem)) return null;

		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
		boolean whitelisted = isComfortsSleepingBagItem(itemId) || isPresentItem(itemId);
		if (!whitelisted) return null;

		ClaimedChunkManagerImpl manager = ClaimedChunkManagerImpl.getInstance();
		if (manager == null) return null;

		BlockPos targetPos = hit.getBlockPos();

		boolean preventEdit =
				manager.shouldPreventInteraction(sp, hand, targetPos, Protection.EDIT_BLOCK, null)
						|| manager.shouldPreventInteraction(sp, hand, targetPos, Protection.EDIT_AND_INTERACT_BLOCK, null);

		if (!preventEdit) return null;

		InteractionResult r = stack.useOn(new UseOnContext(sp, hand, hit));
		if (r.consumesAction() || r == InteractionResult.SUCCESS) {
			sp.inventoryMenu.sendAllDataToRemote();
			return r;
		}

		return InteractionResult.PASS;
	}

	// Accept both comforts:sleeping_bag and comforts:sleeping_bag_*
	private static boolean isComfortsSleepingBagItem(ResourceLocation itemId) {
		if (!"comforts".equals(itemId.getNamespace())) return false;
		String p = itemId.getPath();
		return p.equals("sleeping_bag") || p.startsWith("sleeping_bag_");
	}

	private static boolean isPresentItem(ResourceLocation itemId) {
		if (!"supplementaries".equals(itemId.getNamespace())) return false;
		String p = itemId.getPath();
		return p.equals("present") || p.startsWith("present_") || p.equals("trapped_present") || p.startsWith("trapped_present_");
	}

	// Accept both comforts:sleeping_bag and comforts:sleeping_bag_*
	private static boolean isComfortsSleepingBagBlock(BlockState state) {
		Block b = state.getBlock();
		ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
		if (!"comforts".equals(id.getNamespace())) return false;

		String p = id.getPath().toLowerCase();
		return p.equals("sleeping_bag") || p.startsWith("sleeping_bag_") || p.contains("sleepingbag");
	}

	private static void ppbundle$resyncAfterDeniedBlockUse(Player player, Level level, BlockHitResult hit) {
		if (!(player instanceof ServerPlayer sp)) return;

		BlockPos target = hit.getBlockPos();
		BlockPos placed = target.relative(hit.getDirection());

		sp.connection.send(new ClientboundBlockUpdatePacket(level, target));
		sp.connection.send(new ClientboundBlockUpdatePacket(level, placed));

		sp.inventoryMenu.sendAllDataToRemote();
	}

	private static boolean allowLivingDamage(LivingEntity target, DamageSource source) {
		Level level = target.level();
		if (level.isClientSide) return true;

		Entity srcEntity = source.getEntity();
		if (!(srcEntity instanceof ServerPlayer attacker)) return true;

		if (target instanceof Player) return true;
		if (isHostile(target)) return true;

		if (!FabricLoader.getInstance().isModLoaded("ftbchunks")) return true;

		ClaimedChunkManagerImpl manager = ClaimedChunkManagerImpl.getInstance();
		if (manager == null) return true;

		BlockPos pos = target.blockPosition();
		boolean noEditPermission = manager.shouldPreventInteraction(
				attacker,
				InteractionHand.MAIN_HAND,
				pos,
				Protection.EDIT_AND_INTERACT_BLOCK,
				target
		);

		return !noEditPermission;
	}

	private static boolean isHostile(LivingEntity entity) {
		if (entity instanceof Enemy) return true;

		EntityType<?> type = entity.getType();
		MobCategory cat = type.getCategory();
		return cat == MobCategory.MONSTER;
	}

	private static InteractionResultHolder<ItemStack> maybeDenyItemUse(Player player, Level world, InteractionHand hand) {
		if (!FabricLoader.getInstance().isModLoaded("ftbchunks")) return null;

		ItemStack stack = player.getItemInHand(hand);
		if (stack.isEmpty()) return null;

		InteractionBlacklistConfig cfg = BLACKLIST;
		if (cfg == null) return null;

		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (!cfg.isBlockedItem(itemId.getNamespace(), itemId.getPath())) return null;

		ClaimedChunkManagerImpl manager = ClaimedChunkManagerImpl.getInstance();
		if (manager == null) return null;

		BlockPos pos = player.blockPosition();
		boolean noEditPermission = manager.shouldPreventInteraction(player, hand, pos, Protection.EDIT_AND_INTERACT_BLOCK, null);

		return noEditPermission ? InteractionResultHolder.fail(stack) : null;
	}

	private static InteractionResult maybeDenyBlockUse(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
		if (!FabricLoader.getInstance().isModLoaded("ftbchunks")) return null;

		ClaimedChunkManagerImpl manager = ClaimedChunkManagerImpl.getInstance();
		if (manager == null) return null;

		BlockPos pos = hit.getBlockPos();
		BlockState state = world.getBlockState(pos);
		Block block = state.getBlock();

		ItemStack held = player.getItemInHand(hand);
		if (!held.isEmpty() && held.getItem() instanceof BucketItem) {
			boolean noEditPermission = manager.shouldPreventInteraction(player, hand, pos, Protection.EDIT_AND_INTERACT_BLOCK, null);
			return noEditPermission ? InteractionResult.FAIL : null;
		}

		if (state.is(BlockTags.BEDS) || isComfortsSleepBlock(block)) {
			boolean noEditPermission = manager.shouldPreventInteraction(player, hand, pos, Protection.EDIT_AND_INTERACT_BLOCK, null);
			return noEditPermission ? InteractionResult.FAIL : null;
		}

		InteractionBlacklistConfig cfg = BLACKLIST;
		if (cfg == null) return null;

		if (state.is(BlockTags.CAULDRONS) && cfg.isBlockedBlock("minecraft", "cauldron")) {
			boolean noEditPermission = manager.shouldPreventInteraction(player, hand, pos, Protection.EDIT_AND_INTERACT_BLOCK, null);
			return noEditPermission ? InteractionResult.FAIL : null;
		}

		ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
		if (cfg.isBlockedBlock(blockId.getNamespace(), blockId.getPath())) {
			boolean noEditPermission = manager.shouldPreventInteraction(player, hand, pos, Protection.EDIT_AND_INTERACT_BLOCK, null);
			return noEditPermission ? InteractionResult.FAIL : null;
		}

		if (!held.isEmpty()) {
			ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(held.getItem());
			if (cfg.isBlockedItem(itemId.getNamespace(), itemId.getPath())) {
				boolean noEditPermission = manager.shouldPreventInteraction(player, hand, pos, Protection.EDIT_AND_INTERACT_BLOCK, null);
				return noEditPermission ? InteractionResult.FAIL : null;
			}
		}

		return null;
	}

	private static boolean isComfortsSleepBlock(Block block) {
		ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
		String ns = id.getNamespace();
		if (!ns.equals("comforts") && !ns.equals("comfort")) return false;

		String path = id.getPath();
		return path.contains("sleeping_bag") || path.contains("hammock");
	}

	private static InteractionResult maybeDenyEntityUse(Player player, Level world, InteractionHand hand, Entity entity) {
		if (!FabricLoader.getInstance().isModLoaded("ftbchunks")) return null;

		InteractionBlacklistConfig cfg = BLACKLIST;
		if (cfg == null) return null;

		ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
		if (!cfg.isBlockedEntity(typeId.getNamespace(), typeId.getPath())) return null;

		ClaimedChunkManagerImpl manager = ClaimedChunkManagerImpl.getInstance();
		if (manager == null) return null;

		BlockPos pos = entity.blockPosition();
		boolean noEditPermission = manager.shouldPreventInteraction(player, hand, pos, Protection.EDIT_AND_INTERACT_BLOCK, entity);
		return noEditPermission ? InteractionResult.FAIL : null;
	}

	private static InteractionResult maybeBlockCarry(Player player, Level world, InteractionHand hand, BlockPos pos) {
		if (world.isClientSide) return null;
		if (!FabricLoader.getInstance().isModLoaded("carryon")) return null;
		if (!FabricLoader.getInstance().isModLoaded("ftbchunks")) return null;
		if (!(player instanceof ServerPlayer serverPlayer)) return null;

		if (!serverPlayer.isShiftKeyDown()) return null;

		ItemStack stack = serverPlayer.getItemInHand(hand);
		if (!stack.isEmpty()) return null;

		ClaimedChunkManagerImpl manager = ClaimedChunkManagerImpl.getInstance();
		if (manager == null) return null;

		boolean prevent = manager.shouldPreventInteraction(serverPlayer, hand, pos, PPBundleProtections.CARRYON_BLOCK, null);
		return prevent ? InteractionResult.FAIL : null;
	}

	private static InteractionResult maybeEntityCarry(Player player, Level world, InteractionHand hand, Entity entity) {
		if (world.isClientSide) return null;
		if (!FabricLoader.getInstance().isModLoaded("carryon")) return null;
		if (!FabricLoader.getInstance().isModLoaded("ftbchunks")) return null;
		if (!(player instanceof ServerPlayer serverPlayer)) return null;

		if (!serverPlayer.isShiftKeyDown()) return null;

		ItemStack stack = serverPlayer.getItemInHand(hand);
		if (!stack.isEmpty()) return null;

		ClaimedChunkManagerImpl manager = ClaimedChunkManagerImpl.getInstance();
		if (manager == null) return null;

		boolean prevent = manager.shouldPreventInteraction(serverPlayer, hand, entity.blockPosition(), PPBundleProtections.CARRYON_ENTITY, entity);
		return prevent ? InteractionResult.FAIL : null;
	}
}