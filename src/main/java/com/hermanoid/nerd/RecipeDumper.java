package com.hermanoid.nerd;

import codechicken.nei.ItemPanels;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.PositionedStack;
import codechicken.nei.config.DataDumper;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.ICraftingHandler;
import codechicken.nei.util.NBTJson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.RegistryNamespaced;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

// This dumper will likely be pretty heavy when run on a large modpack
// It finds all items in the world, then queries all recipe handlers for recipes to make it (crafting, not usage)
// Finally, it dumps all that into a (probably large) output file
public class RecipeDumper extends DataDumper {

    private static final RegistryNamespaced itemRegistry = Item.itemRegistry;

    public RecipeDumper(String name) {
        super(name);
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
        tag.removeTag("Count");
        itemObj.addProperty("nbt", tag.toString());
        // I think there will be extra metadata/info here.
        return itemObj;
    }

    private String stacksToJsonArrayString(List<PositionedStack> stacks) {
        JsonArray arr = new JsonArray();
        for (PositionedStack stack : stacks) {
            Item item = stack.item.getItem();
            JsonObject itemObj = stackToDetailedJson(stack.item);
            arr.add(itemObj);
        }
        return arr.toString();
    }

    @Override
    public Iterable<String[]> dump(int mode) {
        // ItemStack craftingTable = new ItemStack(Item.getItemById(58));
        // ItemStack furnace = new ItemStack(Item.getItemById(61));

        // Dare I cache all this dump output into a list?
        // We'll try it and beg forgiveness from the gods of RAM if things go south
        // I just don't know how much text I'm going to be dealing with yet.
        List<String[]> list = new LinkedList<>();
        for (ItemStack stack : ItemPanels.itemPanel.getItems()) {
            // Gather item details (don't grab everything... you can dump items if you want more details)
            // These columns will be repeated many times in the output, so don't write more than needed.
            Item item = (Item) stack.getItem();
            int id = itemRegistry.getIDForObject(item);
            String name = itemRegistry.getNameForObject(item);

            String queryItem = stackToDetailedJson(stack).toString();

            // Perform the Query
            List<ICraftingHandler> handlers = GuiCraftingRecipe.getCraftingHandlers("item", stack);
            for (ICraftingHandler handler : handlers) {
                // Gather crafting handler details (again, just a minimum, cross-reference a handler dump if you want)
                final String handlerName = handler.getHandlerId();
                final String recipeName = handler.getRecipeName();
                final String recipeTabName = handler.getRecipeTabName();

                // There be some *nested loop* action here
                for (int recipeIndex = 0; recipeIndex < handler.numRecipes(); recipeIndex++) {
                    // Collapse Ingredient Lists into JSON format to keep CSV file sizes from going *completely* crazy
                    // List<> ingredients = handler.getIngredientStacks(recipeIndex).stream().map(
                    // pos_stack -> pos_stack.item.getItem()
                    // ).collect(Collectors.toList());
                    String ingredients = stacksToJsonArrayString(handler.getIngredientStacks(recipeIndex));
                    String otherIngredients = stacksToJsonArrayString(handler.getOtherStacks(recipeIndex));
                    String outItem = stackToDetailedJson(handler.getResultStack(recipeIndex).item).toString();
                    list.add(
                        new String[] { name, Integer.toString(id), queryItem, handlerName,
                            Integer.toString(recipeIndex), ingredients, otherIngredients, outItem });
                }
            }
        }

        // for (IRecipeHandler handler : GuiUsageRecipe.usagehandlers) {
        // final String handlerName = handler.getHandlerId();
        // final String handlerId = Objects.firstNonNull(
        // handler instanceof TemplateRecipeHandler ? ((TemplateRecipeHandler) handler).getOverlayIdentifier()
        // : null,
        // "null");
        // HandlerInfo info = GuiRecipeTab.getHandlerInfo(handlerName, handlerId);
        //
        // list.add(
        // new String[] { handler.getRecipeName(), handlerName, handlerId,
        // info != null ? info.getModName() : "Unknown",
        // info != null && info.getItemStack() != null ? info.getItemStack().toString() : "Unknown" });
        // }
        return list;
    }

    @Override
    public String renderName() {
        return translateN(name);
    }

    @Override
    public String getFileExtension() {
        switch (getMode()) {
            case 0:
                return ".csv";
            case 1:
                return ".json";
        }
        return null;
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
    public void dumpTo(File file) throws IOException {
        if (getMode() == 0) super.dumpTo(file);
        else dumpJson(file);
    }

    public void dumpJson(File file) throws IOException {
        final String[] header = header();
        final FileWriter writer;
        try {
            writer = new FileWriter(file);
            for (String[] list : dump(getMode())) {
                NBTTagCompound tag = new NBTTagCompound();
                for (int i = 0; i < header.length; i++) {
                    tag.setString(header[i], list[i]);
                }
                IOUtils.write(NBTJson.toJson(tag) + "\n", writer);
            }
            writer.close();
        } catch (IOException e) {
            NEIClientConfig.logger.error("Filed to save dump recipe list to file {}", file, e);
        }
    }

    @Override
    public int modeCount() {
        return 2;
    }
}
