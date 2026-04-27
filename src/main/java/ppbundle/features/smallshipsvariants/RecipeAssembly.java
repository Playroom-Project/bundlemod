package ppbundle.features.smallshipsvariants;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * This helper builds recipe json objects that can be parsed by vanilla serializers during reload.

 * All recipe inputs and outputs are resolved directly from the live item registry so compat
 * items always use their actual registered ids, including namespace-safe fallback ids.
 */
public final class RecipeAssembly {
    private RecipeAssembly() {
    }

    public static JsonObject boatRecipe(Item planks, Item result) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:crafting_shaped");

        JsonArray pattern = new JsonArray();
        pattern.add("p p");
        pattern.add("ppp");
        root.add("pattern", pattern);

        JsonObject key = new JsonObject();
        key.add("p", itemEntry(idOf(planks)));
        root.add("key", key);

        root.add("result", resultEntry(idOf(result)));
        return root;
    }

    public static JsonObject chestBoatRecipe(Item boat, Item result) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:crafting_shapeless");

        JsonArray ingredients = new JsonArray();
        ingredients.add(itemEntry(idOf(boat)));
        ingredients.add(itemEntry(idOf(Items.CHEST)));
        root.add("ingredients", ingredients);

        root.add("result", resultEntry(idOf(result)));
        return root;
    }

    public static JsonObject shipRecipe(VariantShipKind kind, Item boat, Item chestBoat, Item result) {
        return switch (kind) {
            case COG -> cogRecipe(boat, result);
            case BRIGG -> briggRecipe(chestBoat, result);
            case GALLEY -> galleyRecipe(boat, result);
            case DRAKKAR -> drakkarRecipe(boat, result);
        };
    }

    private static JsonObject cogRecipe(Item boat, Item result) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:crafting_shaped");
        root.addProperty("group", VariantShipKind.COG.recipeGroup());

        JsonArray pattern = new JsonArray();
        pattern.add("lsl");
        pattern.add("bbb");
        root.add("pattern", pattern);

        JsonObject key = new JsonObject();
        key.add("s", itemEntry("smallships:sail"));
        key.add("l", itemEntry(idOf(Items.LEAD)));
        key.add("b", itemEntry(idOf(boat)));
        root.add("key", key);

        root.add("result", resultEntry(idOf(result)));
        return root;
    }

    private static JsonObject briggRecipe(Item chestBoat, Item result) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:crafting_shaped");
        root.addProperty("group", VariantShipKind.BRIGG.recipeGroup());

        JsonArray pattern = new JsonArray();
        pattern.add("sls");
        pattern.add("bbb");
        root.add("pattern", pattern);

        JsonObject key = new JsonObject();
        key.add("l", itemEntry(idOf(Items.LEAD)));
        key.add("s", itemEntry("smallships:sail"));
        key.add("b", itemEntry(idOf(chestBoat)));
        root.add("key", key);

        root.add("result", resultEntry(idOf(result)));
        return root;
    }

    private static JsonObject galleyRecipe(Item boat, Item result) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:crafting_shaped");
        root.addProperty("group", VariantShipKind.GALLEY.recipeGroup());

        JsonArray pattern = new JsonArray();
        pattern.add("lll");
        pattern.add("csc");
        pattern.add("bbb");
        root.add("pattern", pattern);

        JsonObject key = new JsonObject();
        key.add("s", itemEntry("smallships:sail"));
        key.add("c", itemEntry(idOf(Items.CHEST)));
        key.add("l", itemEntry(idOf(Items.LEAD)));
        key.add("b", itemEntry(idOf(boat)));
        root.add("key", key);

        root.add("result", resultEntry(idOf(result)));
        return root;
    }

    private static JsonObject drakkarRecipe(Item boat, Item result) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:crafting_shaped");
        root.addProperty("group", VariantShipKind.DRAKKAR.recipeGroup());

        JsonArray pattern = new JsonArray();
        pattern.add("sys");
        pattern.add("xlx");
        pattern.add("bbb");
        root.add("pattern", pattern);

        JsonObject key = new JsonObject();
        key.add("y", itemEntry("smallships:sail"));
        key.add("x", itemEntry(idOf(Items.CHEST)));
        key.add("l", itemEntry(idOf(Items.LEAD)));
        key.add("s", itemEntry(idOf(Items.STRING)));
        key.add("b", itemEntry(idOf(boat)));
        root.add("key", key);

        root.add("result", resultEntry(idOf(result)));
        return root;
    }

    private static JsonObject itemEntry(String itemId) {
        JsonObject obj = new JsonObject();
        obj.addProperty("item", itemId);
        return obj;
    }

    private static JsonObject resultEntry(String itemId) {
        JsonObject obj = new JsonObject();
        obj.addProperty("item", itemId);
        return obj;
    }

    /**
     * This resolves the exact registered item id and fails fast for invalid entries.
     */
    private static String idOf(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Recipe item cannot be null");
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (BuiltInRegistries.ITEM.get(id) == Items.AIR) {
            throw new IllegalStateException("Recipe item is not registered: " + item);
        }

        return id.toString();
    }
}