package ppbundle.features.smallshipsvariants;

import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;
import ppbundle.core.feature.FeatureContext;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * This registry discovers plank families incrementally and registers compat items
 * only when native support is truly absent or explicitly disabled for selected namespaces.

 * Compat items are registered into the Small Ships namespace.
 * Normal compat ids use the plain wood path.
 * Selected namespaces can use a dedicated prefix to avoid collisions with native items.
 */
public final class SmallShipsVariantRegistry {
    public static final String WOOD_TYPE_TAG = "PPBundleWoodType";
    private static final String SMALL_SHIPS_NAMESPACE = "smallships";
    private static final String FORCED_COMPAT_PREFIX = "ppbundle_";

    /**
     * These wood families already have native vanilla boats and native Small Ships support.
     * They must never receive compat variants from this mod.
     */
    private static final Set<String> WOOD_BLACKLIST = Set.of(
            "minecraft:oak",
            "minecraft:spruce",
            "minecraft:birch",
            "minecraft:jungle",
            "minecraft:acacia",
            "minecraft:cherry",
            "minecraft:dark_oak",
            "minecraft:mangrove",
            "minecraft:bamboo"
    );

    /**
     * These namespaces always use PPBundle-owned Small Ships compat ship ids.
     * Native Small Ships ship ids from these namespaces are ignored by the resolver.
     */
    private static final Set<String> FORCE_COMPAT_SHIP_OVERRIDE_NAMESPACES = Set.of(
            "paradise_lost"
    );

    /**
     * This mirrors the native Small Ships-supported vanilla boat set directly from Boat.Type.
     */
    private static final Set<String> NATIVE_SMALL_SHIPS_WOOD_TYPES = buildNativeSmallShipsWoodTypes();

    /**
     * These ids are reserved by native vanilla-oriented Small Ships content and must never be
     * registered as compat fallbacks.
     */
    private static final Set<ResourceLocation> RESERVED_NATIVE_SMALL_SHIPS_ITEM_IDS = buildReservedNativeSmallShipsItemIds();

    /**
     * These aliases handle woods that do not follow the standard "<wood>_planks" rule.
     */
    private static final Map<ResourceLocation, String> WOOD_NAME_ALIASES = Map.of(
            new ResourceLocation("hybrid-aquatic", "driftwood_planks"), "driftwood"
    );

    private static final Map<String, PendingWoodFamily> PENDING_WOODS = new LinkedHashMap<>();
    private static final Map<String, VariantWoodType> WOOD_TYPES = new LinkedHashMap<>();

    /**
     * These maps hold compat fallback items registered in the Small Ships namespace.
     */
    private static final Map<String, Item> BOAT_INGREDIENT_ITEMS = new LinkedHashMap<>();
    private static final Map<String, Item> CHEST_BOAT_INGREDIENT_ITEMS = new LinkedHashMap<>();
    private static final Map<VariantShipKind, Map<String, Item>> SHIP_ITEMS = new EnumMap<>(VariantShipKind.class);

    private static boolean bootstrapped = false;
    private static Logger logger;

    private SmallShipsVariantRegistry() {
    }

    public static void bootstrap(FeatureContext ctx) {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;
        logger = ctx.logger();

        for (VariantShipKind kind : VariantShipKind.values()) {
            SHIP_ITEMS.put(kind, new LinkedHashMap<>());
        }

        registerCreativeTabEntries();
        discoverExistingBlocks(ctx);
        installLateBlockDiscovery(ctx);

        ctx.logger().info("[PPBundle] Small Ships variant woods discovered during bootstrap: {}", WOOD_TYPES.size());
        ctx.logger().info("[PPBundle] Small Ships runtime ship kinds enabled: {}", supportedShipKindsLogValue());

        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) ->
                ctx.logger().info("[PPBundle] Small Ships variant woods loaded: {}", WOOD_TYPES.size())
        );
    }

    public static Collection<VariantWoodType> woods() {
        return Collections.unmodifiableCollection(WOOD_TYPES.values());
    }

    public static VariantWoodType wood(String woodTypeId) {
        return WOOD_TYPES.get(woodTypeId);
    }

    public static Map<String, VariantWoodType> woodMap() {
        return Collections.unmodifiableMap(WOOD_TYPES);
    }

    public static boolean isKnownWoodType(String woodTypeId) {
        return WOOD_TYPES.containsKey(woodTypeId);
    }

    public static Item boatIngredientItem(String woodTypeId) {
        return BOAT_INGREDIENT_ITEMS.get(woodTypeId);
    }

    public static Item chestBoatIngredientItem(String woodTypeId) {
        return CHEST_BOAT_INGREDIENT_ITEMS.get(woodTypeId);
    }

    public static Item shipItem(VariantShipKind kind, String woodTypeId) {
        Map<String, Item> map = SHIP_ITEMS.get(kind);
        return map == null ? null : map.get(woodTypeId);
    }

    public static boolean hasNativeBoatItem(VariantWoodType wood) {
        return BuiltInRegistries.ITEM.containsKey(wood.boatItemId());
    }

    public static boolean hasNativeChestBoatItem(VariantWoodType wood) {
        return BuiltInRegistries.ITEM.containsKey(wood.chestBoatItemId());
    }

    /**
     * This checks for a real native Small Ships ship item while excluding compat
     * items that this registry has already inserted under the same id.
     */
    public static boolean hasNativeShipItem(VariantWoodType wood, VariantShipKind kind) {
        if (shouldForceCompatShips(wood)) {
            return false;
        }

        ResourceLocation id = nativeSmallShipsShipId(wood, kind);

        if (RESERVED_NATIVE_SMALL_SHIPS_ITEM_IDS.contains(id)) {
            return true;
        }

        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            return false;
        }

        return !isCompatShipRegistered(wood, kind);
    }

    public static boolean needsCompatBoatItem(VariantWoodType wood) {
        return !hasNativeBoatItem(wood) && BOAT_INGREDIENT_ITEMS.containsKey(wood.namespacedWoodKey());
    }

    public static boolean needsCompatChestBoatItem(VariantWoodType wood) {
        return !hasNativeChestBoatItem(wood) && CHEST_BOAT_INGREDIENT_ITEMS.containsKey(wood.namespacedWoodKey());
    }

    public static boolean needsCompatShipItem(VariantWoodType wood, VariantShipKind kind) {
        return SHIP_ITEMS.get(kind).containsKey(wood.namespacedWoodKey());
    }

    /**
     * This returns the expected native Small Ships ship id used by direct integrations.
     */
    public static ResourceLocation nativeSmallShipsShipId(VariantWoodType wood, VariantShipKind kind) {
        String woodPath = woodPathFromPlankId(wood.planksId());
        return new ResourceLocation(SMALL_SHIPS_NAMESPACE, woodPath + "_" + kind.serializedName());
    }

    public static Item resolvedBoatIngredient(VariantWoodType wood) {
        if (hasNativeBoatItem(wood)) {
            return BuiltInRegistries.ITEM.get(wood.boatItemId());
        }
        return BOAT_INGREDIENT_ITEMS.get(wood.namespacedWoodKey());
    }

    public static Item resolvedChestBoatIngredient(VariantWoodType wood) {
        if (hasNativeChestBoatItem(wood)) {
            return BuiltInRegistries.ITEM.get(wood.chestBoatItemId());
        }
        return CHEST_BOAT_INGREDIENT_ITEMS.get(wood.namespacedWoodKey());
    }

    public static Item resolvedShipItem(VariantWoodType wood, VariantShipKind kind) {
        if (isCompatShipRegistered(wood, kind)) {
            return shipItem(kind, wood.namespacedWoodKey());
        }

        if (shouldForceCompatShips(wood)) {
            return null;
        }

        ResourceLocation id = nativeSmallShipsShipId(wood, kind);
        if (BuiltInRegistries.ITEM.containsKey(id)) {
            return BuiltInRegistries.ITEM.get(id);
        }

        return null;
    }

    private static boolean shouldForceCompatShips(VariantWoodType wood) {
        return FORCE_COMPAT_SHIP_OVERRIDE_NAMESPACES.contains(wood.planksId().getNamespace());
    }

    private static boolean isCompatShipRegistered(VariantWoodType wood, VariantShipKind kind) {
        Map<String, Item> map = SHIP_ITEMS.get(kind);
        return map != null && map.containsKey(wood.namespacedWoodKey());
    }

    private static boolean shouldSkipWoodFamily(String woodTypeId) {
        return WOOD_BLACKLIST.contains(woodTypeId) || NATIVE_SMALL_SHIPS_WOOD_TYPES.contains(woodTypeId);
    }

    private static void discoverExistingBlocks(FeatureContext ctx) {
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
            observeBlock(blockId, ctx);
        }
    }

    private static void installLateBlockDiscovery(FeatureContext ctx) {
        RegistryEntryAddedCallback.event(BuiltInRegistries.BLOCK).register((rawId, id, block) -> observeBlock(id, ctx));
    }

    private static void observeBlock(ResourceLocation blockId, FeatureContext ctx) {
        String woodPath = resolveWoodPath(blockId);
        if (woodPath == null || woodPath.isBlank()) {
            return;
        }

        String woodTypeId = blockId.getNamespace() + ":" + woodPath;
        if (shouldSkipWoodFamily(woodTypeId)) {
            return;
        }

        PendingWoodFamily pending = PENDING_WOODS.computeIfAbsent(
                woodTypeId,
                ignored -> createPendingFamily(blockId.getNamespace(), woodPath)
        );

        pending.setPlanksId(blockId);
        finalizePendingFamily(pending, ctx);
    }

    private static PendingWoodFamily createPendingFamily(String namespace, String woodPath) {
        String woodTypeId = namespace + ":" + woodPath;
        String flattenedName = compatName(namespace, woodPath);
        String englishDisplayName = humanize(woodPath);

        ResourceLocation boatId = new ResourceLocation(namespace, woodPath + "_boat");
        ResourceLocation chestBoatId = new ResourceLocation(namespace, woodPath + "_chest_boat");

        return new PendingWoodFamily(
                woodTypeId,
                woodPath,
                flattenedName,
                englishDisplayName,
                boatId,
                chestBoatId
        );
    }

    private static void finalizePendingFamily(PendingWoodFamily pending, FeatureContext ctx) {
        if (pending.isFinalized() || !pending.hasPlanks()) {
            return;
        }

        VariantWoodType wood = pending.toVariantWoodType();
        String woodTypeId = pending.woodTypeId();

        if (WOOD_TYPES.containsKey(woodTypeId)) {
            pending.markFinalized();
            return;
        }

        WOOD_TYPES.put(woodTypeId, wood);
        pending.markFinalized();

        registerFallbackItems(wood);

        ctx.logger().info("[PPBundle] Registered Small Ships wood family: {}", woodTypeId);
    }

    private static void registerFallbackItems(VariantWoodType wood) {
        if (!hasNativeBoatItem(wood)) {
            registerBoatFallback(wood);
        }

        if (!hasNativeChestBoatItem(wood)) {
            registerChestBoatFallback(wood);
        }

        for (VariantShipKind kind : VariantShipKind.values()) {
            if (hasNativeShipItem(wood, kind)) {
                logSkipNativeShip(wood, kind);
                continue;
            }

            Item item = createShipFallbackItem(wood, kind);
            if (item != null) {
                SHIP_ITEMS.get(kind).put(wood.namespacedWoodKey(), item);
                logRegisteredCompatShip(wood, kind, item);
            } else {
                logMissingCompatShip(wood, kind);
            }
        }
    }

    private static void registerBoatFallback(VariantWoodType wood) {
        ResourceLocation id = compatBoatId(wood);

        Item existing = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (existing != null) {
            BOAT_INGREDIENT_ITEMS.put(wood.namespacedWoodKey(), existing);
            return;
        }

        Item item = Registry.register(
                BuiltInRegistries.ITEM,
                id,
                new VariantBoatIngredientItem(
                        wood.namespacedWoodKey(),
                        false,
                        new Item.Properties().stacksTo(1)
                )
        );

        BOAT_INGREDIENT_ITEMS.put(wood.namespacedWoodKey(), item);
    }

    private static void registerChestBoatFallback(VariantWoodType wood) {
        ResourceLocation id = compatChestBoatId(wood);

        Item existing = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (existing != null) {
            CHEST_BOAT_INGREDIENT_ITEMS.put(wood.namespacedWoodKey(), existing);
            return;
        }

        Item item = Registry.register(
                BuiltInRegistries.ITEM,
                id,
                new VariantBoatIngredientItem(
                        wood.namespacedWoodKey(),
                        true,
                        new Item.Properties().stacksTo(1)
                )
        );

        CHEST_BOAT_INGREDIENT_ITEMS.put(wood.namespacedWoodKey(), item);
    }

    private static Item createShipFallbackItem(VariantWoodType wood, VariantShipKind kind) {
        ResourceLocation id = compatShipId(wood, kind);

        Item existing = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (existing != null) {
            return existing;
        }

        return Registry.register(
                BuiltInRegistries.ITEM,
                id,
                switch (kind) {
                    case COG -> new VariantCogCompatItem(
                            wood.namespacedWoodKey(),
                            new Item.Properties().stacksTo(1)
                    );
                    case BRIGG -> new VariantBriggCompatItem(
                            wood.namespacedWoodKey(),
                            new Item.Properties().stacksTo(1)
                    );
                    case GALLEY -> new VariantGalleyCompatItem(
                            wood.namespacedWoodKey(),
                            new Item.Properties().stacksTo(1)
                    );
                    case DRAKKAR -> new VariantDrakkarCompatItem(
                            wood.namespacedWoodKey(),
                            new Item.Properties().stacksTo(1)
                    );
                }
        );
    }

    private static void logSkipNativeShip(VariantWoodType wood, VariantShipKind kind) {
        if (logger == null) {
            return;
        }

        logger.info(
                "[PPBundle] Skipping compat Small Ships ship because native ship is active | wood={} | kind={} | nativeId={}",
                wood.namespacedWoodKey(),
                kind.serializedName(),
                nativeSmallShipsShipId(wood, kind)
        );
    }

    private static void logRegisteredCompatShip(VariantWoodType wood, VariantShipKind kind, Item item) {
        if (logger == null) {
            return;
        }

        logger.info(
                "[PPBundle] Registered compat Small Ships ship | wood={} | kind={} | itemId={}",
                wood.namespacedWoodKey(),
                kind.serializedName(),
                BuiltInRegistries.ITEM.getKey(item)
        );
    }

    private static void logMissingCompatShip(VariantWoodType wood, VariantShipKind kind) {
        if (logger == null) {
            return;
        }

        logger.warn(
                "[PPBundle] Missing compat Small Ships ship after registration attempt | wood={} | kind={} | compatId={}",
                wood.namespacedWoodKey(),
                kind.serializedName(),
                compatShipId(wood, kind)
        );
    }

    /**
     * This returns the base compat name used inside the Small Ships namespace.

     * Normal namespaces use only the wood path.
     * Forced namespaces use a dedicated prefix plus the wood path.
     */
    private static String compatBaseName(VariantWoodType wood) {
        if (shouldForceCompatShips(wood)) {
            return FORCED_COMPAT_PREFIX + wood.woodPath();
        }

        return wood.woodPath();
    }

    private static ResourceLocation compatBoatId(VariantWoodType wood) {
        return new ResourceLocation(SMALL_SHIPS_NAMESPACE, compatBaseName(wood) + "_boat");
    }

    private static ResourceLocation compatChestBoatId(VariantWoodType wood) {
        return new ResourceLocation(SMALL_SHIPS_NAMESPACE, compatBaseName(wood) + "_chest_boat");
    }

    private static ResourceLocation compatShipId(VariantWoodType wood, VariantShipKind kind) {
        return new ResourceLocation(SMALL_SHIPS_NAMESPACE, compatBaseName(wood) + "_" + kind.serializedName());
    }

    private static void registerCreativeTabEntries() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            for (VariantWoodType wood : WOOD_TYPES.values()) {
                if (needsCompatBoatItem(wood)) {
                    entries.accept(BOAT_INGREDIENT_ITEMS.get(wood.namespacedWoodKey()));
                }

                if (needsCompatChestBoatItem(wood)) {
                    entries.accept(CHEST_BOAT_INGREDIENT_ITEMS.get(wood.namespacedWoodKey()));
                }

                for (VariantShipKind kind : VariantShipKind.values()) {
                    if (needsCompatShipItem(wood, kind)) {
                        entries.accept(SHIP_ITEMS.get(kind).get(wood.namespacedWoodKey()));
                    }
                }
            }
        });
    }

    private static String resolveWoodPath(ResourceLocation blockId) {
        String aliased = WOOD_NAME_ALIASES.get(blockId);
        if (aliased != null) {
            return aliased;
        }

        String path = blockId.getPath();
        if (path.endsWith("_planks")) {
            return path.substring(0, path.length() - "_planks".length());
        }

        return null;
    }

    private static String woodPathFromPlankId(ResourceLocation plankId) {
        String aliased = WOOD_NAME_ALIASES.get(plankId);
        if (aliased != null) {
            return aliased;
        }

        String path = plankId.getPath();
        if (path.endsWith("_planks")) {
            return path.substring(0, path.length() - "_planks".length());
        }

        return path;
    }

    private static Set<String> buildNativeSmallShipsWoodTypes() {
        LinkedHashSet<String> ids = new LinkedHashSet<>();

        for (Boat.Type type : Boat.Type.values()) {
            ids.add("minecraft:" + type.getName());
        }

        return Set.copyOf(ids);
    }

    private static Set<ResourceLocation> buildReservedNativeSmallShipsItemIds() {
        LinkedHashSet<ResourceLocation> ids = new LinkedHashSet<>();

        for (Boat.Type type : Boat.Type.values()) {
            String woodName = type.getName();

            ids.add(new ResourceLocation(SMALL_SHIPS_NAMESPACE, woodName + "_cog"));
            ids.add(new ResourceLocation(SMALL_SHIPS_NAMESPACE, woodName + "_brigg"));
            ids.add(new ResourceLocation(SMALL_SHIPS_NAMESPACE, woodName + "_galley"));
            ids.add(new ResourceLocation(SMALL_SHIPS_NAMESPACE, woodName + "_drakkar"));
        }

        return Set.copyOf(ids);
    }

    private static String supportedShipKindsLogValue() {
        StringBuilder builder = new StringBuilder();

        for (VariantShipKind kind : VariantShipKind.values()) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(kind.serializedName());
        }

        return builder.toString();
    }

    /**
     * This produces a namespace-safe internal key for maps owned by this registry.
     * It does not define the public item id used in the Small Ships namespace.
     */
    private static String compatName(String namespace, String woodPath) {
        String sanitizedPath = sanitizeCompatPathPart(woodPath);

        if ("minecraft".equals(namespace)) {
            return sanitizedPath;
        }

        return sanitizeCompatPathPart(namespace) + "_" + sanitizedPath;
    }

    private static String sanitizeCompatPathPart(String value) {
        return value
                .replace('/', '_')
                .replace('-', '_')
                .replace('.', '_');
    }

    private static String humanize(String woodPath) {
        String[] parts = woodPath.replace('/', '_').split("_");
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }

            if (!builder.isEmpty()) {
                builder.append(' ');
            }

            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }

        return builder.toString();
    }
}