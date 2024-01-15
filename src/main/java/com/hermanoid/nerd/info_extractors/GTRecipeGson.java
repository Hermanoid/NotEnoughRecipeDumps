package com.hermanoid.nerd.info_extractors;

import com.google.gson.*;
import com.hermanoid.nerd.RecipeDumpContext;
import gregtech.api.enums.Materials;
import gregtech.nei.GT_NEI_DefaultHandler;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GTRecipeGson {

    public static class GTRecipeExclusionStrategy implements ExclusionStrategy {

        private final static Set<String> badFields = new HashSet<>(
            Arrays.asList(
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
        List<Type> badTypes = Arrays.asList(GT_NEI_DefaultHandler.class, ItemStack.class, Materials.class);

        @Override
        public boolean shouldSkipField(FieldAttributes f) {

            return badFields.contains(f.getName());
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return badTypes.contains(clazz);
        }
    }

    private static class FluidStackSerializer implements JsonSerializer<FluidStack> {

        private final RecipeDumpContext context;

        private FluidStackSerializer(RecipeDumpContext context) {
            this.context = context;
        }

        @Override
        public JsonElement serialize(FluidStack src, Type typeOfSrc, JsonSerializationContext jcontext) {
            return context.getMinimalFluidDump(src);
        }
    }

    public static Gson buildGson(RecipeDumpContext context) {
        return new GsonBuilder()
            // We might be only doing serializations, but GSON will still create
            // a type adapter and get stuck in nasty recursion/type access land
            // if it thinks it might need to do deserialization.
            .addSerializationExclusionStrategy(new GTRecipeExclusionStrategy())
            .addDeserializationExclusionStrategy(new GTRecipeExclusionStrategy())
            .registerTypeAdapter(FluidStack.class, new FluidStackSerializer(context))
            .create();
    }
}
