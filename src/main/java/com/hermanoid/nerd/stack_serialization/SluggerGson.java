package com.hermanoid.nerd.stack_serialization;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.github.bsideup.jabel.Desugar;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

// Provides a Gson instance that turns items+fluids into minimal slugs to dramatically reduce dump sizes.
public class SluggerGson {

    @Desugar
    private record FluidStackSerializer(RecipeDumpContext context) implements JsonSerializer<FluidStack> {

        @Override
            public JsonElement serialize(FluidStack src, Type typeOfSrc, JsonSerializationContext jcontext) {
                return context.getMinimalFluidDump(src);
            }
        };

    @Desugar
    private record ItemStackSerializer(RecipeDumpContext context) implements JsonSerializer<ItemStack> {
        @Override
        public JsonElement serialize(ItemStack src, Type typeOfSrc, JsonSerializationContext jcontext) {
            return context.getMinimalItemDump(src);
        }
    }

    /**
     * @return A GsonBuilder with Slug-afying Serializers installed, which
     * you can add your own extensions to.
     */
    public static GsonBuilder gsonBuilder(RecipeDumpContext context){
        return new GsonBuilder()
            .registerTypeAdapter(FluidStack.class, new FluidStackSerializer(context))
            .registerTypeAdapter(ItemStack.class, new ItemStackSerializer(context));
    }

    /**
     * Helper method which adds a ListExclusionStrategy to the base gsonBuilder(context)
     * It's a common enough use-case to deserve its own helper.
     */
    public static GsonBuilder gsonBuilder(RecipeDumpContext context, Collection<String> badFields, Collection<Type> badTypes){
        SetExclusionStrategy exclusionStrategy = new SetExclusionStrategy(
            new HashSet<String>(badFields),
            new HashSet<Type>(badTypes)
        );
        return gsonBuilder(context)
            .addSerializationExclusionStrategy(exclusionStrategy)
            .addDeserializationExclusionStrategy(exclusionStrategy);
    }
}
