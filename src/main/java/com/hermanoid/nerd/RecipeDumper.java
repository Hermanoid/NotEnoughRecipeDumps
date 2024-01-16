package com.hermanoid.nerd;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Stream;

import com.hermanoid.nerd.dumpers.DumperRegistry;
import com.hermanoid.nerd.stack_serialization.RecipeDumpContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;

import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hermanoid.nerd.dumpers.BaseRecipeDumper;

import codechicken.core.CommonUtils;
import codechicken.nei.ItemList;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.config.DataDumper;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.ICraftingHandler;

// This dumper will likely be pretty heavy when run on a large modpack
// It finds all items in the world, then queries all recipe handlers for recipes to make it (crafting, not usage)
// Finally, it dumps all that into a (probably large) output file
public class RecipeDumper extends DataDumper {

    private final static long PROGRESS_INTERVAL = 2500L;
    public int totalQueries = -1;
    public int dumpedQueries = -1;
    private boolean dumpActive = false;
    private final Timer timer = new Timer();
    private RecipeDumpContext context = null;
    public RecipeDumper(String name) {
        super(name);
    }

    @Override
    public String[] header() {
        return new String[] { "Name", "ID", "NBT", "Handler Name", "Handler Recipe Index", "Ingredients", "Other Items",
            "Output Item" };
    }

    private static class QueryResult {

        public ItemStack targetStack;
        public List<ICraftingHandler> handlers;
    }

    private QueryResult performQuery(ItemStack targetStack) {
        QueryResult result = new QueryResult();
        result.targetStack = targetStack;
        result.handlers = GuiCraftingRecipe.getCraftingHandlers("item", targetStack);
        return result;
    }


    private JsonObject extractJsonRecipeData(QueryResult queryResult) {
        // Gather item details (don't grab everything... you can dump items if you want more details)
        // These columns will be repeated many times in the output, so don't write more than needed.

        JsonObject queryDump = new JsonObject();
        queryDump.add("query_item", context.getMinimalItemDump(queryResult.targetStack));

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

            Iterable<BaseRecipeDumper> dumpers;
            if (DumperRegistry.containsKey(handlerId)) {
                dumpers = DumperRegistry.get(handlerId);
            }else{
                dumpers = DumperRegistry.get(BaseRecipeDumper.FALLBACK_DUMPER_NAME);
            }

            JsonArray recipeDumpArr = new JsonArray();
            for (int recipeIndex = 0; recipeIndex < handler.numRecipes(); recipeIndex++) {
                JsonObject recipeDump = new JsonObject();
                // There be some seriously nested loop action here
                for (BaseRecipeDumper dumper : dumpers) {
                    recipeDump.add(dumper.getSlug(), dumper.dump(handler, recipeIndex));
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
        // Update yeahhhh so parallelization works with some mods but in the larger GTNH modpack, some handlers don't react well
        return items.stream()
            .map(this::performQuery)
            .map(this::extractJsonRecipeData);
    }

    @Override
    public Iterable<String[]> dump(int mode) {
        // This is a little crunchy, I'll admit
        throw new NotImplementedException(
            "Recipe Dumper overrides the base DataDumper's dumping functionality in dumpTo(file)! dump() should never be called.");
    }

    @Override
    public void dumpTo(File file) {
        // Allow both 1 and 0... I dunno why but if you're running debug the mode is 1 and if you run with the full
        // GTNH modpack it's 0.
        // Instead of solving it, we ignore it (big brain)
        if (getMode() != 1 && getMode() != 0) {
            throw new RuntimeException("RecipeDumper received an unexpected mode! There should only be one mode: JSON");
        }
        dumpJson(file);
    }

    // If you don't wanna hold all this crap in memory at once, you're going to have to work for it (w/ JsonWriter)
    private void doDumpJson(File file) {
        context = setupNewContext();
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
            jsonWriter.name("version")
                .value(Tags.VERSION);
            jsonWriter.name("queries")
                .beginArray();
            Object lock = new Object();
            getQueryDumps(items).forEach(obj -> {
                synchronized (lock) {
                    gson.toJson(obj, jsonWriter);
                    dumpedQueries++;
                }
            });

            jsonWriter.endArray();

            jsonWriter.endObject();
            jsonWriter.close();
            writer.close();

            dumpContext(context);
        } catch (IOException e) {
            NEIClientConfig.logger.error("Failed to save dump recipe list to file {}", file, e);
        } finally {
            context = null;
            totalQueries = -1;
            dumpedQueries = -1;
        }
    }

    @NotNull
    private RecipeDumpContext setupNewContext() {
        RecipeDumpContext context = new RecipeDumpContext();
        DumperRegistry.setContext(context);
        return context;
    }

    private void dumpContext(RecipeDumpContext context) {
        try {
            File file = new File(
                CommonUtils.getMinecraftDir(),
                "dumps/" + getFileName(name.replaceFirst(".+\\.", "") + "_stacks"));
            if (!file.getParentFile()
                .exists())
                file.getParentFile()
                    .mkdirs();
            if (!file.exists()) file.createNewFile();

            FileWriter writer = new FileWriter(file);
            JsonWriter jsonWriter = new JsonWriter(writer);
            // Use a jsonWriter dump because the FileWriter seems to chop off the end of the (very large) dump().toString()
            new Gson().toJson(context.dump(), jsonWriter);
            jsonWriter.close();
            writer.close();
        } catch (Exception e) {
            NEIClientConfig.logger.error("Error dumping extras for " + renderName() + " mode: " + getMode(), e);
        }
    }

    public void dumpJson(File file) {
        if (dumpActive) {
            NEIClientUtils.printChatMessage(new ChatComponentTranslation("nei.options.tools.dump.recipes.duplicate"));
            return;
        }
        dumpActive = true;
        TimerTask progressTask = getProgressTask();
        Thread workerThread = new Thread(() -> {
            try {
                doDumpJson(file);
            } finally {
                dumpActive = false;
                progressTask.cancel();
            }
            NEIClientUtils.printChatMessage(new ChatComponentTranslation("nei.options.tools.dump.recipes.complete"));
        });
        workerThread.start();
    }

    @NotNull
    private TimerTask getProgressTask() {
        TimerTask progressTask = new TimerTask() {

            @Override
            public void run() {
                NEIClientUtils.printChatMessage(
                    new ChatComponentTranslation(
                        "nei.options.tools.dump.recipes.progress",
                        dumpedQueries,
                        totalQueries,
                        (float) dumpedQueries / totalQueries * 100));
            }
        };
        timer.schedule(progressTask, 0, PROGRESS_INTERVAL);
        return progressTask;
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
    public int modeCount() {
        return 1; // Only JSON
    }
}
