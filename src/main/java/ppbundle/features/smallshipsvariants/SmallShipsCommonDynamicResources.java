package ppbundle.features.smallshipsvariants;

import com.google.gson.JsonObject;
import net.devtech.arrp.api.RRPCallback;
import net.devtech.arrp.api.RuntimeResourcePack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;
import ppbundle.core.feature.FeatureContext;

import java.nio.charset.StandardCharsets;

/**
 * This common service builds a runtime data pack in memory and exposes dynamic
 * Small Ships recipes through ARRP so recipe loading follows the same startup
 * path in IDEA and normal launcher environments.
 */
public final class SmallShipsCommonDynamicResources {
    private static final SmallShipsCommonDynamicResources INSTANCE = new SmallShipsCommonDynamicResources();

    private static final String PACK_NAMESPACE = "playroom-project-bundlemod";
    private static final String PACK_PATH = "generated_smallships_data";
    private static final String SMALL_SHIPS_NAMESPACE = "smallships";
    private static final ResourceLocation AIR_ID = BuiltInRegistries.ITEM.getKey(Items.AIR);

    private boolean registered = false;
    private GeneratedRecipePack cachedPack;

    private SmallShipsCommonDynamicResources() {
    }

    public static SmallShipsCommonDynamicResources getInstance() {
        return INSTANCE;
    }

    public void register(FeatureContext ctx) {
        if (registered) {
            return;
        }
        registered = true;

        try {
            cachedPack = buildRuntimePack(ctx.logger());
            ctx.logger().info(
                    "[PPBundle] Prebuilt Small Ships runtime data pack with {} recipes",
                    cachedPack.generatedRecipeCount()
            );
        } catch (Throwable t) {
            ctx.logger().error("[PPBundle] Failed to prebuild Small Ships runtime data pack", t);
        }

        RRPCallback.BETWEEN_MODS_AND_USER.register(resources -> {
            try {
                GeneratedRecipePack freshPack = buildRuntimePack(ctx.logger());
                resources.add(freshPack.pack());

                ctx.logger().info(
                        "[PPBundle] Exposed Small Ships runtime data pack with {} recipes",
                        freshPack.generatedRecipeCount()
                );
            } catch (Throwable t) {
                ctx.logger().error("[PPBundle] Failed to expose Small Ships runtime data pack", t);
            }
        });
    }

    public void rebuildCache(Logger logger) {
        try {
            cachedPack = buildRuntimePack(logger);
            logger.info(
                    "[PPBundle] Rebuilt cached Small Ships runtime data pack with {} recipes",
                    cachedPack.generatedRecipeCount()
            );
        } catch (Throwable t) {
            logger.error("[PPBundle] Failed to rebuild cached Small Ships runtime data pack", t);
        }
    }

    private GeneratedRecipePack buildRuntimePack(Logger logger) {
        RuntimeResourcePack pack = RuntimeResourcePack.create(new ResourceLocation(PACK_NAMESPACE, PACK_PATH));
        int generatedRecipeCount = 0;

        logger.info(
                "[PPBundle] Building Small Ships runtime recipes for {} wood families",
                SmallShipsVariantRegistry.woods().size()
        );

        for (VariantWoodType wood : SmallShipsVariantRegistry.woods()) {
            Block planksBlock = BuiltInRegistries.BLOCK.get(wood.planksId());
            Item planksItem = planksBlock.asItem();

            logger.debug(
                    "[PPBundle] Runtime recipe scan | wood={} | planksBlock={} | planksItem={}",
                    wood.namespacedWoodKey(),
                    BuiltInRegistries.BLOCK.getKey(planksBlock),
                    BuiltInRegistries.ITEM.getKey(planksItem)
            );

            if (!isUsableItem(planksItem)) {
                logger.debug(
                        "[PPBundle] Runtime recipe skip | wood={} | reason=planks item not usable",
                        wood.namespacedWoodKey()
                );
                continue;
            }

            if (SmallShipsVariantRegistry.needsCompatBoatItem(wood)) {
                Item compatBoat = SmallShipsVariantRegistry.boatIngredientItem(wood.namespacedWoodKey());

                logger.debug(
                        "[PPBundle] Runtime boat recipe candidate | wood={} | result={}",
                        wood.namespacedWoodKey(),
                        compatBoat == null ? "null" : BuiltInRegistries.ITEM.getKey(compatBoat)
                );

                if (isUsableItem(compatBoat)) {
                    addRecipe(
                            pack,
                            recipeId(wood, "boat"),
                            RecipeAssembly.boatRecipe(planksItem, compatBoat)
                    );
                    generatedRecipeCount++;
                } else {
                    logger.debug(
                            "[PPBundle] Runtime boat recipe skip | wood={} | reason=compat boat not usable",
                            wood.namespacedWoodKey()
                    );
                }
            }

            if (SmallShipsVariantRegistry.needsCompatChestBoatItem(wood)) {
                Item boatIngredient = SmallShipsVariantRegistry.resolvedBoatIngredient(wood);
                Item compatChestBoat = SmallShipsVariantRegistry.chestBoatIngredientItem(wood.namespacedWoodKey());

                logger.debug(
                        "[PPBundle] Runtime chest boat recipe candidate | wood={} | boatIngredient={} | result={}",
                        wood.namespacedWoodKey(),
                        boatIngredient == null ? "null" : BuiltInRegistries.ITEM.getKey(boatIngredient),
                        compatChestBoat == null ? "null" : BuiltInRegistries.ITEM.getKey(compatChestBoat)
                );

                if (isUsableItem(boatIngredient) && isUsableItem(compatChestBoat)) {
                    addRecipe(
                            pack,
                            recipeId(wood, "chest_boat"),
                            RecipeAssembly.chestBoatRecipe(boatIngredient, compatChestBoat)
                    );
                    generatedRecipeCount++;
                } else {
                    logger.debug(
                            "[PPBundle] Runtime chest boat recipe skip | wood={} | reason=ingredient not usable",
                            wood.namespacedWoodKey()
                    );
                }
            }

            for (VariantShipKind kind : VariantShipKind.values()) {
                if (!SmallShipsVariantRegistry.needsCompatShipItem(wood, kind)) {
                    logger.debug(
                            "[PPBundle] Runtime ship recipe skip | wood={} | kind={} | reason=no compat ship registered",
                            wood.namespacedWoodKey(),
                            kind.serializedName()
                    );
                    continue;
                }

                Item boatIngredient = SmallShipsVariantRegistry.resolvedBoatIngredient(wood);
                Item chestBoatIngredient = SmallShipsVariantRegistry.resolvedChestBoatIngredient(wood);
                Item shipResult = SmallShipsVariantRegistry.shipItem(kind, wood.namespacedWoodKey());

                logger.debug(
                        "[PPBundle] Runtime ship recipe candidate | wood={} | kind={} | boatIngredient={} | chestBoatIngredient={} | result={}",
                        wood.namespacedWoodKey(),
                        kind.serializedName(),
                        boatIngredient == null ? "null" : BuiltInRegistries.ITEM.getKey(boatIngredient),
                        chestBoatIngredient == null ? "null" : BuiltInRegistries.ITEM.getKey(chestBoatIngredient),
                        shipResult == null ? "null" : BuiltInRegistries.ITEM.getKey(shipResult)
                );

                if (!isUsableItem(boatIngredient) || !isUsableItem(chestBoatIngredient) || !isUsableItem(shipResult)) {
                    logger.debug(
                            "[PPBundle] Runtime ship recipe skip | wood={} | kind={} | reason=ingredient not usable",
                            wood.namespacedWoodKey(),
                            kind.serializedName()
                    );
                    continue;
                }

                addRecipe(
                        pack,
                        recipeId(wood, kind.serializedName()),
                        RecipeAssembly.shipRecipe(kind, boatIngredient, chestBoatIngredient, shipResult)
                );
                generatedRecipeCount++;
            }
        }

        logger.info(
                "[PPBundle] Built Small Ships runtime data pack with {} recipes",
                generatedRecipeCount
        );

        return new GeneratedRecipePack(pack, generatedRecipeCount);
    }

    private static void addRecipe(RuntimeResourcePack pack, ResourceLocation id, JsonObject json) {
        ResourceLocation dataPath = new ResourceLocation(
                id.getNamespace(),
                "recipes/" + id.getPath() + ".json"
        );

        pack.addData(dataPath, json.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static ResourceLocation recipeId(VariantWoodType wood, String suffix) {
        return new ResourceLocation(
                SMALL_SHIPS_NAMESPACE,
                wood.flattenedName() + "_" + suffix + "_recipe"
        );
    }

    private static boolean isUsableItem(Item item) {
        if (item == null || item == Items.AIR) {
            return false;
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return BuiltInRegistries.ITEM.containsKey(id) && !id.equals(AIR_ID);
    }

    private record GeneratedRecipePack(RuntimeResourcePack pack, int generatedRecipeCount) {
    }
}