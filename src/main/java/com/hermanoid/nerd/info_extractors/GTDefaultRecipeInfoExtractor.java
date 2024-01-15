package com.hermanoid.nerd.info_extractors;

import com.google.gson.JsonElement;
import com.hermanoid.nerd.RecipeDumpContext;

import codechicken.nei.recipe.ICraftingHandler;
import gregtech.api.util.GT_Recipe;
import gregtech.nei.GT_NEI_DefaultHandler;

public class GTDefaultRecipeInfoExtractor implements IRecipeInfoExtractor {

    public GTDefaultRecipeInfoExtractor() {

    }

    @Override
    public JsonElement extractInfo(RecipeDumpContext context, ICraftingHandler handler, int recipeIndex) {
        GT_NEI_DefaultHandler gthandler = (GT_NEI_DefaultHandler) handler;
        GT_Recipe recipe = gthandler.getCache()
            .get(recipeIndex).mRecipe;
        try {
            return context.gson.toJsonTree(recipe);
        } catch (Exception e) {
            System.out.println("GSON Serialization failed for handler " + handler.getRecipeName());
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
