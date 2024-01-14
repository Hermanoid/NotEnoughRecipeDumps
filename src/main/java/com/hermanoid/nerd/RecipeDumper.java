package com.hermanoid.nerd;

import codechicken.nei.ItemList;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.config.DataDumper;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.ICraftingHandler;
import codechicken.nei.util.NBTJson;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hermanoid.nerd.info_extractors.IRecipeInfoExtractor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.RegistryNamespaced;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
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
    long progressInterval = 2500L;

    public int totalQueries = -1;
    public int dumpedQueries = -1;
    private boolean dumpActive = false;
    private Timer timer = new Timer();

    private final Multimap<String, IRecipeInfoExtractor> recipeInfoExtractors = HashMultimap.create();

    public void registerRecipeInfoExtractor(IRecipeInfoExtractor extractor){
        for(String id : extractor.getCompatibleHandlers())
            recipeInfoExtractors.put(id, extractor);
    }

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
            JsonObject itemObj = stackToDetailedJson(stack.item);
            arr.add(itemObj);
        }
        return arr;
    }

    private static class QueryResult{
        public ItemStack targetStack;
        public List<ICraftingHandler> handlers;
    }

    private QueryResult performQuery(ItemStack targetStack){
        QueryResult result = new QueryResult();
        result.targetStack = targetStack;
        result.handlers = GuiCraftingRecipe.getCraftingHandlers("item", targetStack);
        return result;
    }

    private JsonObject extractJsonRecipeData(QueryResult queryResult){
        // Gather item details (don't grab everything... you can dump items if you want more details)
        // These columns will be repeated many times in the output, so don't write more than needed.

        JsonObject queryDump = new JsonObject();
        queryDump.add("query_item", stackToDetailedJson(queryResult.targetStack));

        JsonArray handlerDumpArr = new JsonArray();
        // Perform the Query
        List<ICraftingHandler> handlers = queryResult.handlers;
        for (ICraftingHandler handler : handlers) {
            JsonObject handlerDump = new JsonObject();

            // Gather crafting handler details (again, just a minimum, cross-reference a handler dump if you want)
            String handlerId = handler.getHandlerId();
            handlerDump.addProperty("id", handlerId);
            handlerDump.addProperty("name", handler.getRecipeName());
            handlerDump.addProperty("tab_name", handler.getRecipeTabName());

            JsonArray recipeDumpArr = new JsonArray();
            // There be some *nested loop* action here
            for (int recipeIndex = 0; recipeIndex < handler.numRecipes(); recipeIndex++) {
                JsonObject recipeDump = new JsonObject();
                // Collapse Ingredient Lists into JSON format to keep CSV file sizes from going *completely* crazy
                recipeDump.add("ingredients", stacksToJsonArray(handler.getIngredientStacks(recipeIndex)));
                recipeDump.add("other_stacks", stacksToJsonArray(handler.getOtherStacks(recipeIndex)));
                if (handler.getResultStack(recipeIndex) != null) {
                    recipeDump.add("out_item", stackToDetailedJson(handler.getResultStack(recipeIndex).item));
                }
                if(recipeInfoExtractors.containsKey(handlerId)){
                    for(IRecipeInfoExtractor extractor : recipeInfoExtractors.get(handlerId)){
                        recipeDump.add(extractor.getSlug(), extractor.extractInfo(handler, recipeIndex));
                    }
                }
                recipeDumpArr.add(recipeDump);
            }
            handlerDump.add("recipes", recipeDumpArr);
            handlerDumpArr.add(handlerDump);
        }
        queryDump.add("handlers", handlerDumpArr);
        return queryDump;
    }

    public Stream<JsonObject> getQueryDumps(List<ItemStack> items) {
        // Parallelization doesn't help a *lot* but it is like a 2x speedup so I'll take it
        return items.parallelStream()
                    .map(this::performQuery)
                    .map(this::extractJsonRecipeData);
    }



    @Override
    public String renderName() {
        return translateN(name);
    }

    @Override
    public String getFileExtension() {
        return ".json";
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

    private void doDumpJson(File file){
        final String[] header = header();
        final FileWriter writer;
        final JsonWriter jsonWriter;
        final Gson gson = new Gson();
        List<ItemStack> items = ItemList.items;
        totalQueries = items.size();
        dumpedQueries = 0;

        try {
            writer = new FileWriter(file);
            jsonWriter = new JsonWriter(writer);

            jsonWriter.beginObject();
            jsonWriter.setIndent("    ");
            jsonWriter.name("version").value(version);

            jsonWriter.name("queries").beginArray();
            Object lock = new Object();
            getQueryDumps(items).forEach(obj ->
            {
                synchronized (lock){
                    gson.toJson(obj, jsonWriter);
                    dumpedQueries++;
                }
            });

            jsonWriter.endArray();

            jsonWriter.endObject();
            jsonWriter.close();
            writer.close();
        } catch (IOException e) {
            NEIClientConfig.logger.error("Filed to save dump recipe list to file {}", file, e);
        }
        totalQueries = -1;
        dumpedQueries = -1;
    }

    // If you don't wanna hold all this crap in memory at once, you're going to have to work for it.
    public void dumpJson(File file) throws IOException {
        if(dumpActive){
            NEIClientUtils.printChatMessage(new ChatComponentTranslation(
                "nei.options.tools.dump.recipes.duplicate"
            ));
            return;
        }
        dumpActive = true;
        TimerTask progressTask = getProgressTask();
        Thread workerThread = new Thread(()-> {
            try{
                doDumpJson(file);
            }finally{
                dumpActive = false;
                progressTask.cancel();
            }
            NEIClientUtils.printChatMessage(new ChatComponentTranslation(
                "nei.options.tools.dump.recipes.complete"
            ));
        });
        workerThread.start();
    }

    @NotNull
    private TimerTask getProgressTask() {
        TimerTask progressTask = new TimerTask() {
            @Override
            public void run() {
                NEIClientUtils.printChatMessage(new ChatComponentTranslation(
                    "nei.options.tools.dump.recipes.progress",
                    dumpedQueries,
                    totalQueries,
                    (float)dumpedQueries/totalQueries*100
                ));
            }
        };
        timer.schedule(progressTask, 0, progressInterval);
        return progressTask;
    }

    @Override
    public int modeCount() {
        return 1; // Only JSON
    }
}
