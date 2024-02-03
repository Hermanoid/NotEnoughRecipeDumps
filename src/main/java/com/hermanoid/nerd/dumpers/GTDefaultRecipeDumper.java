package com.hermanoid.nerd.dumpers;

import java.lang.reflect.Type;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.hermanoid.nerd.stack_serialization.RecipeDumpContext;
import com.hermanoid.nerd.stack_serialization.SluggerGson;

import codechicken.nei.recipe.ICraftingHandler;
import gregtech.api.enums.Materials;
import gregtech.api.util.GT_Recipe;
import gregtech.nei.GT_NEI_DefaultHandler;

public class GTDefaultRecipeDumper extends BaseRecipeDumper {

    private static final List<String> badFields = ImmutableList.of(
        // Unnecessary/bulky info
        "recipeCategory",
        "stackTraces",
        "owners"

    );
    private static final List<Type> badTypes = ImmutableList.of(GT_NEI_DefaultHandler.class, Materials.class);
    private Gson gson;

    @Override
    public void setContext(RecipeDumpContext context) {
        super.setContext(context);
        gson = SluggerGson.gsonBuilder(context, badFields, badTypes)
            .create();
    }

    @Override
    public JsonElement dump(ICraftingHandler handler, int recipeIndex) {
        GT_NEI_DefaultHandler gthandler = (GT_NEI_DefaultHandler) handler;
        GT_Recipe recipe = ((GT_NEI_DefaultHandler.CachedDefaultRecipe) gthandler.arecipes.get(recipeIndex)).mRecipe;
        try {
            return gson.toJsonTree(recipe);
        } catch (Exception e) {
            System.out
                .println("GTDefaultRecipeDumper GSON Serialization failed for handler " + handler.getRecipeName());
            return null;
        }
    }

    @Override
    public String[] getCompatibleHandlers() {
        return new String[] { "gregtech.nei.GT_NEI_DefaultHandler" };
    }

    @Override
    public String getSlug() {
        return "gtDefault";
    }

}
