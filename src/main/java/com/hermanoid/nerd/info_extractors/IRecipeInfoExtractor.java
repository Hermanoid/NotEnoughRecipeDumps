package com.hermanoid.nerd.info_extractors;

import com.google.gson.JsonElement;
import com.hermanoid.nerd.RecipeDumpContext;

import codechicken.nei.recipe.ICraftingHandler;

public interface IRecipeInfoExtractor {

    JsonElement extractInfo(RecipeDumpContext context, ICraftingHandler handler, int recipeIndex);

    String[] getCompatibleHandlers();

    String getSlug();
}
