package com.hermanoid.nerd.dumpers;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.hermanoid.nerd.stack_serialization.RecipeDumpContext;

import codechicken.nei.recipe.ICraftingHandler;
import com.hermanoid.nerd.stack_serialization.SluggerGson;
import gregtech.api.enums.Materials;
import gregtech.api.util.GT_Recipe;
import gregtech.nei.GT_NEI_DefaultHandler;

import java.lang.reflect.Type;
import java.util.List;

public class GTDefaultRecipeDumper extends BaseRecipeDumper {

    private static final List<String> badFields = ImmutableList.of(
        // Unnecessary/bulky info
        "recipeCategory",
        "stackTraces",
        "owners"
        // Some recipes are GT_Recipe_WithAlt, which have more evil ItemStacks we can't serialize.
        // TODO: Remove this comment if new serialization now works with mOreDictAlt
//        "mOreDictAlt"

    );
    private static final List<Type> badTypes = ImmutableList.of(
        GT_NEI_DefaultHandler.class, Materials.class
    );
    private Gson gson;

    @Override
    public void setContext(RecipeDumpContext context){
        super.setContext(context);
        gson = SluggerGson.gsonBuilder(context, badFields, badTypes).create();
    }

    @Override
    public JsonElement dump(ICraftingHandler handler, int recipeIndex) {
        GT_NEI_DefaultHandler gthandler = (GT_NEI_DefaultHandler) handler;
        GT_Recipe recipe = gthandler.getCache().get(recipeIndex).mRecipe;
        try {
            return gson.toJsonTree(recipe);
        } catch (Exception e) {
            System.out.println("GTDefaultRecipeDumper GSON Serialization failed for handler " + handler.getRecipeName());
            return null;
        }
    }

    @Override
    public String[] getCompatibleHandlers() {
        return new String[] { "gregtech.nei.GT_NEI_DefaultHandler" };
    }

    @Override
    public String getSlug() {
        return "greg_data";
    }

}
