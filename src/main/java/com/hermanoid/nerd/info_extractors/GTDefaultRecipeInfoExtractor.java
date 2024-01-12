package com.hermanoid.nerd.info_extractors;

import codechicken.nei.recipe.ICraftingHandler;
import codechicken.nei.util.NBTJson;
import com.google.gson.*;
import gregtech.api.util.GT_Recipe;
import gregtech.nei.GT_NEI_DefaultHandler;
import net.minecraftforge.fluids.FluidStack;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class GTDefaultRecipeInfoExtractor implements IRecipeInfoExtractor {
    private static class GTRecipeExclusionStrategy implements ExclusionStrategy {
        private final static Set<String> badFields = new HashSet<String>(Arrays.asList(
            // Unnecessary/bulky info
            "recipeCategory",
            "stackTraces",
            "owners",
            // Rely on other dumping logic for inputs and outputs;
            // auto-dumping Minecraft ItemStacks causes recursion into some nasty places
            // I could make an adapter, but the generic NEI inputs/outputs logic covers these items no problemo
            "mInputs",
            "mOutputs",
            // FluidStack things
            // Icons are very large, not wise to have stored every time we dump an item
            "stillIconResourceLocation",
            "flowingIconResourceLocation",
            "stillIcon",
            "flowingIcon",
            // There's a recursive fluid definition here
            "registeredFluid",
            // The automatic serializer doesn't like the rarity enum, I dunno why
            "rarity",
            // I don't think a fluid's corresponding block is useful, and it causes breaky recursion
            "block"

        ));
        @Override
        public boolean shouldSkipField(FieldAttributes f) {

            return badFields.contains(f.getName());
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return clazz.equals(GT_NEI_DefaultHandler.class); // Block recursion
        }
    }

    private class FluidStackSerializer implements JsonSerializer<FluidStack>{
        @Override
        public JsonElement serialize(FluidStack src, Type typeOfSrc, JsonSerializationContext context) {
            // Fluids have some goofy unserializable things, similar to ItemStacks
            JsonObject root = new JsonObject();
            root.addProperty("amount", src.amount);
            if(src.tag != null){
                root.add("tag", NBTJson.toJsonObject(src.tag));
            }
            JsonObject fluid = (JsonObject) gson.toJsonTree(src.getFluid());
            // Manually serialize rarity bc wierdness
            fluid.addProperty("rarity", src.getFluid().getRarity().toString());
            // Slap on some info that's only available via method calls
            fluid.addProperty("id", src.getFluidID());
            root.add("fluid", fluid);
            return root;
        }
    }

    private Gson gson;
    public GTDefaultRecipeInfoExtractor(){
        gson = new GsonBuilder()
            // We might be only doing serializations, but GSON will still create
            // a type adapter and get stuck in nasty recursion/type access land
            // if it thinks it might need to do deserialization.
            .addSerializationExclusionStrategy(new GTRecipeExclusionStrategy())
            .addDeserializationExclusionStrategy(new GTRecipeExclusionStrategy())
            .registerTypeAdapter(FluidStack.class, new FluidStackSerializer())
            .create();
    }

    @Override
    public JsonElement extractInfo(ICraftingHandler handler, int recipeIndex) {
        GT_NEI_DefaultHandler gthandler = (GT_NEI_DefaultHandler) handler;
        GT_Recipe recipe = gthandler.getCache().get(recipeIndex).mRecipe;
        return gson.toJsonTree(recipe);
    }

    @Override
    public String[] getCompatibleHandlers() {
        return new String[]{ "gregtech.nei.GT_NEI_DefaultHandler" };
    }

    @Override
    public String getSlug() {
        return "greg_data";
    }


}
