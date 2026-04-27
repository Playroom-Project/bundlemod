package ppbundle.features.smallshipsvariants;

import com.mojang.blaze3d.platform.NativeImage;
import net.devtech.arrp.api.RRPCallback;
import net.devtech.arrp.api.RuntimeResourcePack;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import ppbundle.core.feature.FeatureContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * This client service builds a runtime resource pack in memory and exposes it
 * through ARRP so generated Small Ships models and textures are available
 * during normal resource loading without writing anything to disk.
 */
public final class SmallShipsClientDynamicResources {
    private static final SmallShipsClientDynamicResources INSTANCE = new SmallShipsClientDynamicResources();

    private static final String MOD_ID = "smallships";
    private static final String PACK_NAMESPACE = "playroom-project-bundlemod";
    private static final String PACK_PATH = "generated_smallships_assets";

    private static final String LEGACY_PACK_FOLDER_NAME = "ppbundle_generated_smallships_assets";
    private static final String LEGACY_FILE_PACK_ID = "file/" + LEGACY_PACK_FOLDER_NAME;

    private static final ResourceLocation OAK_PLANKS_TEXTURE =
            new ResourceLocation("minecraft", "textures/block/oak_planks.png");

    private static final ResourceLocation OAK_BOAT_TEXTURE =
            new ResourceLocation("minecraft", "textures/item/oak_boat.png");

    private static final ResourceLocation OAK_CHEST_BOAT_TEXTURE =
            new ResourceLocation("minecraft", "textures/item/oak_chest_boat.png");

    private static final ResourceLocation OAK_BOAT_ENTITY_TEXTURE =
            new ResourceLocation("minecraft", "textures/entity/boat/oak.png");

    private static final ResourceLocation OAK_CHEST_BOAT_ENTITY_TEXTURE =
            new ResourceLocation("minecraft", "textures/entity/chest_boat/oak.png");

    private static final Map<VariantShipKind, ResourceLocation> OAK_SHIP_ITEM_TEXTURES = Map.of(
            VariantShipKind.COG, new ResourceLocation("smallships", "textures/item/ship/oak_cog.png"),
            VariantShipKind.BRIGG, new ResourceLocation("smallships", "textures/item/ship/oak_brigg.png"),
            VariantShipKind.GALLEY, new ResourceLocation("smallships", "textures/item/ship/oak_galley.png"),
            VariantShipKind.DRAKKAR, new ResourceLocation("smallships", "textures/item/ship/oak_drakkar.png")
    );

    private static final ResourceLocation OAK_SHARED_ENTITY_TEXTURE =
            new ResourceLocation("smallships", "textures/entity/ship/oak.png");

    private boolean registered = false;
    private GeneratedPack cachedPack;

    private SmallShipsClientDynamicResources() {
    }

    public static SmallShipsClientDynamicResources getInstance() {
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
                    "[PPBundle] Prebuilt Small Ships runtime resource pack with {} textures and {} item models",
                    cachedPack.generatedTextureCount(),
                    cachedPack.generatedModelCount()
            );
        } catch (Throwable t) {
            ctx.logger().error("[PPBundle] Failed to prebuild Small Ships runtime resource pack", t);
        }

        RRPCallback.BETWEEN_MODS_AND_USER.register(resources -> {
            try {
                GeneratedPack freshPack = buildRuntimePack(ctx.logger());
                resources.add(freshPack.pack());

                ctx.logger().info(
                        "[PPBundle] Exposed Small Ships runtime resource pack with {} textures and {} item models",
                        freshPack.generatedTextureCount(),
                        freshPack.generatedModelCount()
                );
            } catch (Throwable t) {
                ctx.logger().error("[PPBundle] Failed to expose Small Ships runtime resource pack", t);
            }
        });

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            try {
                cleanupLegacyDiskPack(client, ctx.logger());
            } catch (Throwable t) {
                ctx.logger().error("[PPBundle] Failed to clean legacy Small Ships disk resource pack", t);
            }
        });
    }

    public void rebuildCache(Logger logger) {
        try {
            cachedPack = buildRuntimePack(logger);
            logger.info(
                    "[PPBundle] Rebuilt cached Small Ships runtime resource pack with {} textures and {} item models",
                    cachedPack.generatedTextureCount(),
                    cachedPack.generatedModelCount()
            );
        } catch (Throwable t) {
            logger.error("[PPBundle] Failed to rebuild cached Small Ships runtime resource pack", t);
        }
    }

    private void cleanupLegacyDiskPack(Minecraft client, Logger logger) throws IOException {
        boolean changed = false;

        changed |= client.options.resourcePacks.remove(LEGACY_PACK_FOLDER_NAME);
        changed |= client.options.resourcePacks.remove(LEGACY_FILE_PACK_ID);
        changed |= client.options.incompatibleResourcePacks.remove(LEGACY_PACK_FOLDER_NAME);
        changed |= client.options.incompatibleResourcePacks.remove(LEGACY_FILE_PACK_ID);

        if (changed) {
            client.options.save();
            logger.info("[PPBundle] Removed legacy generated Small Ships pack entry from options");
        }

        Path legacyPackDir = FabricLoader.getInstance()
                .getGameDir()
                .resolve("resourcepacks")
                .resolve(LEGACY_PACK_FOLDER_NAME);

        if (Files.exists(legacyPackDir)) {
            deleteDirectory(legacyPackDir);
            logger.info("[PPBundle] Deleted legacy generated Small Ships disk pack");
        }
    }

    private GeneratedPack buildRuntimePack(Logger logger) throws IOException {
        RuntimeResourcePack pack = RuntimeResourcePack.create(new ResourceLocation(PACK_NAMESPACE, PACK_PATH));
        int generatedTextureCount = 0;
        int generatedModelCount = 0;

        try (NativeImage oakPlanks = readTexture(OAK_PLANKS_TEXTURE)) {
            for (VariantWoodType wood : SmallShipsVariantRegistry.woods()) {
                ResourceLocation plankTextureId = resolvePlankTextureLocation(wood, logger);

                if (plankTextureId == null) {
                    logger.warn(
                            "[PPBundle] Skipping Small Ships assets for {} because the planks texture was not found",
                            wood.namespacedWoodKey()
                    );
                    continue;
                }

                try (NativeImage targetPlanks = tryReadTexture(plankTextureId)) {
                    if (targetPlanks == null) {
                        logger.warn(
                                "[PPBundle] Skipping Small Ships assets for {} because the resolved planks texture could not be read: {}",
                                wood.namespacedWoodKey(),
                                plankTextureId
                        );
                        continue;
                    }

                    Counts counts = generateBoatItemAssets(pack, wood, oakPlanks, targetPlanks, logger);
                    generatedTextureCount += counts.generatedTextureCount();
                    generatedModelCount += counts.generatedModelCount();

                    counts = generateBoatEntityAssets(pack, wood, oakPlanks, targetPlanks, logger);
                    generatedTextureCount += counts.generatedTextureCount();

                    counts = generateShipItemAssets(pack, wood, oakPlanks, targetPlanks, logger);
                    generatedTextureCount += counts.generatedTextureCount();
                    generatedModelCount += counts.generatedModelCount();

                    counts = generateShipEntityTextureAssets(pack, wood, oakPlanks, targetPlanks, logger);
                    generatedTextureCount += counts.generatedTextureCount();
                }
            }
        }

        return new GeneratedPack(pack, generatedTextureCount, generatedModelCount);
    }

    private Counts generateBoatItemAssets(
            RuntimeResourcePack pack,
            VariantWoodType wood,
            NativeImage oakPlanks,
            NativeImage targetPlanks,
            Logger logger
    ) throws IOException {
        int generatedTextureCount = 0;
        int generatedModelCount = 0;

        if (SmallShipsVariantRegistry.needsCompatBoatItem(wood)) {
            Item boatItem = SmallShipsVariantRegistry.boatIngredientItem(wood.namespacedWoodKey());
            if (boatItem != null) {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(boatItem);
                Counts counts = generateBoatLikeItem(
                        pack,
                        itemId,
                        OAK_BOAT_TEXTURE,
                        oakPlanks,
                        targetPlanks,
                        logger
                );
                generatedTextureCount += counts.generatedTextureCount();
                generatedModelCount += counts.generatedModelCount();
            }
        }

        if (SmallShipsVariantRegistry.needsCompatChestBoatItem(wood)) {
            Item chestBoatItem = SmallShipsVariantRegistry.chestBoatIngredientItem(wood.namespacedWoodKey());
            if (chestBoatItem != null) {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(chestBoatItem);
                Counts counts = generateBoatLikeItemPreservingOverlay(
                        pack,
                        itemId,
                        OAK_CHEST_BOAT_TEXTURE,
                        OAK_BOAT_TEXTURE,
                        oakPlanks,
                        targetPlanks,
                        logger
                );
                generatedTextureCount += counts.generatedTextureCount();
                generatedModelCount += counts.generatedModelCount();
            }
        }

        return new Counts(generatedTextureCount, generatedModelCount);
    }

    private Counts generateBoatEntityAssets(
            RuntimeResourcePack pack,
            VariantWoodType wood,
            NativeImage oakPlanks,
            NativeImage targetPlanks,
            Logger logger
    ) throws IOException {
        int generatedTextureCount = 0;

        if (SmallShipsVariantRegistry.needsCompatBoatItem(wood)) {
            generatedTextureCount += generateSingleEntityTexture(
                    pack,
                    generatedBoatEntityTextureId(wood),
                    OAK_BOAT_ENTITY_TEXTURE,
                    oakPlanks,
                    targetPlanks,
                    logger
            );
        }

        if (SmallShipsVariantRegistry.needsCompatChestBoatItem(wood)) {
            generatedTextureCount += generateSingleEntityTexturePreservingOverlay(
                    pack,
                    generatedChestBoatEntityTextureId(wood),
                    OAK_CHEST_BOAT_ENTITY_TEXTURE,
                    OAK_BOAT_ENTITY_TEXTURE,
                    oakPlanks,
                    targetPlanks,
                    logger
            );
        }

        return new Counts(generatedTextureCount, 0);
    }

    private Counts generateShipItemAssets(
            RuntimeResourcePack pack,
            VariantWoodType wood,
            NativeImage oakPlanks,
            NativeImage targetPlanks,
            Logger logger
    ) throws IOException {
        int generatedTextureCount = 0;
        int generatedModelCount = 0;

        for (VariantShipKind kind : VariantShipKind.values()) {
            if (!SmallShipsVariantRegistry.needsCompatShipItem(wood, kind)) {
                continue;
            }

            Item shipItem = SmallShipsVariantRegistry.shipItem(kind, wood.namespacedWoodKey());
            if (!isUsableItem(shipItem)) {
                continue;
            }

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(shipItem);
            ResourceLocation oakTextureId = OAK_SHIP_ITEM_TEXTURES.get(kind);
            if (oakTextureId == null) {
                continue;
            }

            Counts counts = generateShipLikeItem(
                    pack,
                    itemId,
                    oakTextureId,
                    oakPlanks,
                    targetPlanks,
                    logger
            );
            generatedTextureCount += counts.generatedTextureCount();
            generatedModelCount += counts.generatedModelCount();
        }

        return new Counts(generatedTextureCount, generatedModelCount);
    }

    private Counts generateShipEntityTextureAssets(
            RuntimeResourcePack pack,
            VariantWoodType wood,
            NativeImage oakPlanks,
            NativeImage targetPlanks,
            Logger logger
    ) throws IOException {
        boolean hasAnyCompatShipVariant = false;

        for (VariantShipKind kind : VariantShipKind.values()) {
            if (SmallShipsVariantRegistry.needsCompatShipItem(wood, kind)) {
                hasAnyCompatShipVariant = true;
                break;
            }
        }

        if (!hasAnyCompatShipVariant) {
            return new Counts(0, 0);
        }

        try (NativeImage oakEntityTexture = readTexture(OAK_SHARED_ENTITY_TEXTURE);
             NativeImage output = PaletteTextureRecolorer.recolorFromOakPlanks(oakEntityTexture, oakPlanks, targetPlanks)) {
            pack.addAsset(generatedSharedEntityTextureId(wood), output.asByteArray());
            logger.info("[PPBundle] Generated entity texture: {}", generatedSharedEntityTextureId(wood));
            return new Counts(1, 0);
        }
    }

    private Counts generateBoatLikeItem(
            RuntimeResourcePack pack,
            ResourceLocation itemId,
            ResourceLocation oakBaseTextureId,
            NativeImage oakPlanks,
            NativeImage targetPlanks,
            Logger logger
    ) throws IOException {
        try (NativeImage oakBaseTexture = readTexture(oakBaseTextureId);
             NativeImage output = PaletteTextureRecolorer.recolorFromOakPlanks(oakBaseTexture, oakPlanks, targetPlanks)) {
            pack.addAsset(boatItemTextureAssetId(itemId), output.asByteArray());
            pack.addAsset(itemModelAssetId(itemId), buildBoatItemModel(itemId).getBytes(StandardCharsets.UTF_8));

            logger.info("[PPBundle] Generated Small Ships item texture: {}", itemId);
            logger.info("[PPBundle] Generated Small Ships item model: {}", itemId);

            return new Counts(1, 1);
        }
    }

    private Counts generateBoatLikeItemPreservingOverlay(
            RuntimeResourcePack pack,
            ResourceLocation itemId,
            ResourceLocation oakCompositeTextureId,
            ResourceLocation oakReferenceTextureId,
            NativeImage oakPlanks,
            NativeImage targetPlanks,
            Logger logger
    ) throws IOException {
        try (NativeImage oakCompositeTexture = readTexture(oakCompositeTextureId);
             NativeImage oakReferenceTexture = readTexture(oakReferenceTextureId);
             NativeImage output = PaletteTextureRecolorer.recolorFromOakPlanksPreservingOverlay(
                     oakCompositeTexture,
                     oakReferenceTexture,
                     oakPlanks,
                     targetPlanks
             )) {
            pack.addAsset(boatItemTextureAssetId(itemId), output.asByteArray());
            pack.addAsset(itemModelAssetId(itemId), buildBoatItemModel(itemId).getBytes(StandardCharsets.UTF_8));

            logger.info("[PPBundle] Generated Small Ships item texture with preserved overlay: {}", itemId);
            logger.info("[PPBundle] Generated Small Ships item model: {}", itemId);

            return new Counts(1, 1);
        }
    }

    private Counts generateShipLikeItem(
            RuntimeResourcePack pack,
            ResourceLocation itemId,
            ResourceLocation oakBaseTextureId,
            NativeImage oakPlanks,
            NativeImage targetPlanks,
            Logger logger
    ) throws IOException {
        try (NativeImage oakBaseTexture = readTexture(oakBaseTextureId);
             NativeImage output = PaletteTextureRecolorer.recolorFromOakPlanks(oakBaseTexture, oakPlanks, targetPlanks)) {
            pack.addAsset(shipItemTextureAssetId(itemId), output.asByteArray());
            pack.addAsset(itemModelAssetId(itemId), buildShipItemModel(itemId).getBytes(StandardCharsets.UTF_8));

            logger.info("[PPBundle] Generated Small Ships item texture: {}", itemId);
            logger.info("[PPBundle] Generated Small Ships item model: {}", itemId);

            return new Counts(1, 1);
        }
    }

    private int generateSingleEntityTexture(
            RuntimeResourcePack pack,
            ResourceLocation outputTextureId,
            ResourceLocation oakBaseTextureId,
            NativeImage oakPlanks,
            NativeImage targetPlanks,
            Logger logger
    ) throws IOException {
        try (NativeImage oakBaseTexture = readTexture(oakBaseTextureId);
             NativeImage output = PaletteTextureRecolorer.recolorFromOakPlanks(oakBaseTexture, oakPlanks, targetPlanks)) {
            pack.addAsset(outputTextureId, output.asByteArray());
            logger.info("[PPBundle] Generated entity texture: {}", outputTextureId);
            return 1;
        }
    }

    private int generateSingleEntityTexturePreservingOverlay(
            RuntimeResourcePack pack,
            ResourceLocation outputTextureId,
            ResourceLocation oakCompositeTextureId,
            ResourceLocation oakReferenceTextureId,
            NativeImage oakPlanks,
            NativeImage targetPlanks,
            Logger logger
    ) throws IOException {
        try (NativeImage oakCompositeTexture = readTexture(oakCompositeTextureId);
             NativeImage oakReferenceTexture = readTexture(oakReferenceTextureId);
             NativeImage output = PaletteTextureRecolorer.recolorFromOakPlanksPreservingOverlay(
                     oakCompositeTexture,
                     oakReferenceTexture,
                     oakPlanks,
                     targetPlanks
             )) {
            pack.addAsset(outputTextureId, output.asByteArray());
            logger.info("[PPBundle] Generated entity texture with preserved overlay: {}", outputTextureId);
            return 1;
        }
    }

    private static String buildBoatItemModel(ResourceLocation itemId) {
        return "{\n" +
                "  \"parent\": \"minecraft:item/generated\",\n" +
                "  \"textures\": {\n" +
                "    \"layer0\": \"" + itemId.getNamespace() + ":item/" + itemId.getPath() + "\"\n" +
                "  }\n" +
                "}\n";
    }

    private static String buildShipItemModel(ResourceLocation itemId) {
        return "{\n" +
                "  \"parent\": \"minecraft:item/generated\",\n" +
                "  \"textures\": {\n" +
                "    \"layer0\": \"" + itemId.getNamespace() + ":item/ship/" + itemId.getPath() + "\"\n" +
                "  }\n" +
                "}\n";
    }

    private static ResourceLocation generatedBoatEntityTextureId(VariantWoodType wood) {
        return new ResourceLocation(MOD_ID, "textures/entity/boat/" + wood.flattenedName() + ".png");
    }

    private static ResourceLocation generatedChestBoatEntityTextureId(VariantWoodType wood) {
        return new ResourceLocation(MOD_ID, "textures/entity/chest_boat/" + wood.flattenedName() + ".png");
    }

    private static ResourceLocation generatedSharedEntityTextureId(VariantWoodType wood) {
        return new ResourceLocation(MOD_ID, "textures/entity/ship/" + wood.flattenedName() + ".png");
    }

    private static ResourceLocation boatItemTextureAssetId(ResourceLocation itemId) {
        return new ResourceLocation(itemId.getNamespace(), "textures/item/" + itemId.getPath() + ".png");
    }

    private static ResourceLocation shipItemTextureAssetId(ResourceLocation itemId) {
        return new ResourceLocation(itemId.getNamespace(), "textures/item/ship/" + itemId.getPath() + ".png");
    }

    private static ResourceLocation itemModelAssetId(ResourceLocation itemId) {
        return new ResourceLocation(itemId.getNamespace(), "models/item/" + itemId.getPath() + ".json");
    }

    private static ResourceLocation resolvePlankTextureLocation(VariantWoodType wood, Logger logger) {
        for (ResourceLocation candidate : plankTextureCandidates(wood)) {
            if (assetExists(candidate)) {
                logger.info("[PPBundle] Using plank texture {} for {}", candidate, wood.namespacedWoodKey());
                return candidate;
            }
        }

        logger.debug("[PPBundle] No plank texture candidates matched for {}", wood.namespacedWoodKey());
        return null;
    }

    private static List<ResourceLocation> plankTextureCandidates(VariantWoodType wood) {
        Set<ResourceLocation> candidates = new LinkedHashSet<>();

        ResourceLocation plankBlockId = wood.planksId();
        String namespace = plankBlockId.getNamespace();
        String fullPath = plankBlockId.getPath();
        String woodPath = wood.woodPath();
        String fullFilename = lastPathSegment(fullPath);
        String woodFilename = lastPathSegment(woodPath);

        addTextureCandidates(candidates, namespace, fullPath);
        addTextureCandidates(candidates, namespace, woodPath);

        if (!fullFilename.equals(fullPath)) {
            addTextureCandidates(candidates, namespace, fullFilename);
        }

        if (!woodFilename.equals(woodPath)) {
            addTextureCandidates(candidates, namespace, woodFilename);
        }

        addTextureCandidates(candidates, namespace, woodPath + "_planks");
        addTextureCandidates(candidates, namespace, woodFilename + "_planks");

        for (String alias : plankNameAliases(wood)) {
            addIndexedPlankCandidates(candidates, namespace, alias);
        }

        return new ArrayList<>(candidates);
    }

    private static void addTextureCandidates(Set<ResourceLocation> candidates, String namespace, String baseName) {
        if (baseName == null || baseName.isBlank()) {
            return;
        }

        addBaseTextureCandidates(candidates, namespace, baseName);
        addIndexedPlankCandidates(candidates, namespace, baseName);
    }

    private static List<String> plankNameAliases(VariantWoodType wood) {
        List<String> aliases = new ArrayList<>();

        String woodPath = wood.woodPath();
        String filename = lastPathSegment(woodPath);

        aliases.add(woodPath);
        aliases.add(filename);

        if ("twilightforest".equals(wood.planksId().getNamespace())) {
            switch (filename) {
                case "dark" -> aliases.add("darkwood");
                case "transformation" -> aliases.add("trans");
                case "mining" -> aliases.add("mine");
                case "sorting" -> aliases.add("sort");
            }
        }

        return aliases;
    }

    private static void addBaseTextureCandidates(Set<ResourceLocation> candidates, String namespace, String baseName) {
        candidates.add(new ResourceLocation(namespace, "textures/block/" + baseName + ".png"));
        candidates.add(new ResourceLocation(namespace, "textures/block/wood/" + baseName + ".png"));
        candidates.add(new ResourceLocation(namespace, "textures/block/planks/" + baseName + ".png"));
        candidates.add(new ResourceLocation(namespace, "textures/block/wood/planks/" + baseName + ".png"));
    }

    private static void addIndexedPlankCandidates(Set<ResourceLocation> candidates, String namespace, String baseName) {
        if (baseName == null || baseName.isBlank()) {
            return;
        }

        String[] indexedBases = new String[] {
                "planks_" + baseName + "_0",
                baseName + "_0"
        };

        for (String indexedBase : indexedBases) {
            candidates.add(new ResourceLocation(namespace, "textures/block/" + indexedBase + ".png"));
            candidates.add(new ResourceLocation(namespace, "textures/block/wood/" + indexedBase + ".png"));
            candidates.add(new ResourceLocation(namespace, "textures/block/planks/" + indexedBase + ".png"));
            candidates.add(new ResourceLocation(namespace, "textures/block/wood/planks/" + indexedBase + ".png"));
        }
    }

    private static String lastPathSegment(String path) {
        int slash = path.lastIndexOf('/');
        if (slash < 0 || slash >= path.length() - 1) {
            return path;
        }
        return path.substring(slash + 1);
    }

    private static boolean assetExists(ResourceLocation assetId) {
        try (InputStream ignored = tryOpenAsset(assetId)) {
            return ignored != null;
        } catch (IOException e) {
            return false;
        }
    }

    private static NativeImage readTexture(ResourceLocation textureId) throws IOException {
        try (InputStream input = openAsset(textureId)) {
            return NativeImage.read(input);
        }
    }

    private static NativeImage tryReadTexture(ResourceLocation textureId) throws IOException {
        InputStream input = tryOpenAsset(textureId);
        if (input == null) {
            return null;
        }

        try (InputStream autoClose = input) {
            return NativeImage.read(autoClose);
        }
    }

    private static InputStream openAsset(ResourceLocation assetId) throws IOException {
        InputStream stream = tryOpenAsset(assetId);
        if (stream == null) {
            throw new IOException("Missing texture resource: " + assetId);
        }
        return stream;
    }

    private static InputStream tryOpenAsset(ResourceLocation assetId) throws IOException {
        String relativePath = "assets/" + assetId.getNamespace() + "/" + assetId.getPath();

        Optional<ModContainer> modContainer = FabricLoader.getInstance().getModContainer(assetId.getNamespace());
        if (modContainer.isPresent()) {
            Optional<Path> modPath = modContainer.get().findPath(relativePath);
            if (modPath.isPresent() && Files.exists(modPath.get())) {
                return Files.newInputStream(modPath.get());
            }
        }

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            InputStream stream = contextClassLoader.getResourceAsStream(relativePath);
            if (stream != null) {
                return stream;
            }
        }

        return SmallShipsClientDynamicResources.class.getClassLoader().getResourceAsStream(relativePath);
    }

    private static boolean isUsableItem(Item item) {
        if (item == null) {
            return false;
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id != null && BuiltInRegistries.ITEM.containsKey(id);
    }

    private static void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        List<Path> paths = new ArrayList<>();
        try (var stream = Files.walk(directory)) {
            stream.forEach(paths::add);
        }

        paths.sort(Comparator.reverseOrder());
        for (Path path : paths) {
            Files.deleteIfExists(path);
        }
    }

    private record Counts(int generatedTextureCount, int generatedModelCount) {
    }

    private record GeneratedPack(
            RuntimeResourcePack pack,
            int generatedTextureCount,
            int generatedModelCount
    ) {
    }
}