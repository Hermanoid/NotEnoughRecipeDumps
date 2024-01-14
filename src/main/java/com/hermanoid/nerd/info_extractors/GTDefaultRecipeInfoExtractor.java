package com.hermanoid.nerd.info_extractors;

import codechicken.nei.recipe.ICraftingHandler;
import codechicken.nei.util.NBTJson;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import gregtech.api.util.GT_Recipe;
import gregtech.common.fluid.GT_Fluid;
import gregtech.nei.GT_NEI_DefaultHandler;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
            "block",
            // Some recipes are GT_Recipe_WithAlt, which have more evil ItemStacks we can't serialize.
            "mOreDictAlt"

        ));
        List<Type> badTypes = Arrays.asList(GT_NEI_DefaultHandler.class, ItemStack.class, FluidStack.class);
        @Override
        public boolean shouldSkipField(FieldAttributes f) {

            return badFields.contains(f.getName());
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return badTypes.contains(clazz);
        }
    }

    private class FluidStackSerializer implements JsonSerializer<FluidStack>{
        private static final Type fluidType = new TypeToken<Fluid>(){}.getType();
        @Override
        public JsonElement serialize(FluidStack src, Type typeOfSrc, JsonSerializationContext context) {
            // Fluids have some goofy unserializable things, similar to ItemStacks
            JsonObject root = new JsonObject();
            root.addProperty("amount", src.amount);
            if(src.tag != null){
                root.add("tag", NBTJson.toJsonObject(src.tag));
            }
            // Some fluids (like water) are defined using anonymous types
            // I think that specifying the type for GT_Fluids would throw away information,
            // but for non-GT_Fluids, we'll need to un-anonymize this beeswax.
            JsonObject fluid = null;
            if(src.getFluid().getClass().equals(GT_Fluid.class)){
                fluid = (JsonObject) gson.toJsonTree(src.getFluid());
            }else{
                fluid = (JsonObject) gson.toJsonTree(src.getFluid(), fluidType);
            }
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
        try{
            return gson.toJsonTree(recipe);
        }catch(Exception e){
            System.out.println("O poop");
            return null;
        }
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
