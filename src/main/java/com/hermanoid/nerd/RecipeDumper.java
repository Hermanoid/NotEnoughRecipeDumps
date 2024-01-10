package com.hermanoid.nerd;

import codechicken.nei.ItemList;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.PositionedStack;
import codechicken.nei.config.DataDumper;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.ICraftingHandler;
import codechicken.nei.util.NBTJson;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.RegistryNamespaced;
import org.apache.commons.lang3.NotImplementedException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

// This dumper will likely be pretty heavy when run on a large modpack
// It finds all items in the world, then queries all recipe handlers for recipes to make it (crafting, not usage)
// Finally, it dumps all that into a (probably large) output file
public class RecipeDumper extends DataDumper {

    private static final RegistryNamespaced itemRegistry = Item.itemRegistry;

    public RecipeDumper(String name) {
        super(name);
    }

    public String version = "1.0";

    @Override
    public String[] header() {
        return new String[] { "Name", "ID", "NBT", "Handler Name", "Handler Recipe Index", "Ingredients", "Other Items",
            "Output Item" };
    }

    private JsonObject stackToDetailedJson(ItemStack stack) {
        JsonObject itemObj = new JsonObject();
        Item item = stack.getItem();
        itemObj.addProperty("id", itemRegistry.getIDForObject(item));
        itemObj.addProperty("regName", itemRegistry.getNameForObject(item));
        itemObj.addProperty("name", item != null ? stack.getUnlocalizedName() : "null");
        itemObj.addProperty("displayName", stack.getDisplayName());

        NBTTagCompound tag = stack.writeToNBT(new NBTTagCompound());
        itemObj.add("nbt", NBTJson.toJsonObject(tag));

        // I think there will be extra metadata/info here.
        return itemObj;
    }

    private JsonArray stacksToJsonArray(List<PositionedStack> stacks) {
        JsonArray arr = new JsonArray();
        for (PositionedStack stack : stacks) {
            Item item = stack.item.getItem();
            JsonObject itemObj = stackToDetailedJson(stack.item);
            arr.add(itemObj);
        }
        return arr;
    }

    public JsonObject extractJsonRecipeData(ItemStack targetStack){
        // Gather item details (don't grab everything... you can dump items if you want more details)
        // These columns will be repeated many times in the output, so don't write more than needed.

        JsonObject queryDump = new JsonObject();
        queryDump.add("query_item", stackToDetailedJson(targetStack));

        JsonArray handlerDumpArr = new JsonArray();
        // Perform the Query
        List<ICraftingHandler> handlers = GuiCraftingRecipe.getCraftingHandlers("item", targetStack);
        for (ICraftingHandler handler : handlers) {

            JsonObject handlerDump = new JsonObject();

            // Gather crafting handler details (again, just a minimum, cross-reference a handler dump if you want)
            handlerDump.addProperty("id", handler.getHandlerId());
            handlerDump.addProperty("name", handler.getRecipeName());
            handlerDump.addProperty("tab_name", handler.getRecipeTabName());

            JsonArray recipeDumpArr = new JsonArray();
            // There be some *nested loop* action here
            for (int recipeIndex = 0; recipeIndex < handler.numRecipes(); recipeIndex++) {
                JsonObject recipeDump = new JsonObject();
                // Collapse Ingredient Lists into JSON format to keep CSV file sizes from going *completely* crazy
                // List<> ingredients = handler.getIngredientStacks(recipeIndex).stream().map(
                // pos_stack -> pos_stack.item.getItem()
                // ).collect(Collectors.toList());
                recipeDump.add("ingredients", stacksToJsonArray(handler.getIngredientStacks(recipeIndex)));
                recipeDump.add("other_stacks", stacksToJsonArray(handler.getOtherStacks(recipeIndex)));
                if (handler.getResultStack(recipeIndex) != null) {
                    recipeDump.add("out_item", stackToDetailedJson(handler.getResultStack(recipeIndex).item));
                }
                recipeDumpArr.add(recipeDump);
            }
            handlerDump.add("recipes", recipeDumpArr);
            handlerDumpArr.add(handlerDump);
        }
        queryDump.add("handlers", handlerDumpArr);
        return queryDump;
    }

    public Stream<JsonObject> getQueryDumps() {
        return ItemList.items.parallelStream()
                    .limit(4000)
                    .map(this::extractJsonRecipeData);
    }



    @Override
    public String renderName() {
        return translateN(name);
    }

    @Override
    public String getFileExtension() {
        return ".json";
//        return switch (getMode()) {
//            case 0 -> ".csv";
//            case 1 -> ".json";
//            default -> null;
//        };
    }

    @Override
    public ChatComponentTranslation dumpMessage(File file) {
        return new ChatComponentTranslation(namespaced(name + ".dumped"), "dumps/" + file.getName());
    }

    @Override
    public String modeButtonText() {
        return translateN(name + ".mode." + getMode());
    }

    @Override
    public Iterable<String[]> dump(int mode) {
        // A little crunchy, I'll admit
        throw new NotImplementedException("Recipe Dumper overrides the base DataDumper's dumping functionality in dumpTo(file)! dump() should never be called.");
    }

    @Override
    public void dumpTo(File file) throws IOException {
        if (getMode() != 1) { throw new RuntimeException("RecipeDumper received an unexpected mode! There should only be one mode: JSON");}
        dumpJson(file);
    }

    // If you don't wanna hold all this crap in memory at once, you're going to have to work for it.
    public void dumpJson(File file) throws IOException {
        final String[] header = header();
        final FileWriter writer;
        final JsonWriter jsonWriter;
        final Gson gson = new Gson();

        try {
            writer = new FileWriter(file);
            jsonWriter = new JsonWriter(writer);

            jsonWriter.beginObject();
            jsonWriter.setIndent("    ");
            jsonWriter.name("version").value(version);

            jsonWriter.name("queries").beginArray();
            Object lock = new Object();

//            AtomicReference<IOException> error = new AtomicReference<>(null);

            getQueryDumps().forEach(obj ->
            {
                synchronized (lock){
                    gson.toJson(obj, jsonWriter);
                }
            });

//            // Super cool error handling.
//            if (error.get() != null){
//                throw error.get();
//            }

            jsonWriter.endArray();

            jsonWriter.endObject();
            jsonWriter.close();
            writer.close();
        } catch (IOException e) {
            NEIClientConfig.logger.error("Filed to save dump recipe list to file {}", file, e);
        }
    }

    @Override
    public int modeCount() {
        return 1; // Only JSON
    }
}
