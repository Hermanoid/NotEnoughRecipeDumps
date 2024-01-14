package com.hermanoid.nerd.info_extractors;

import com.google.gson.JsonElement;

import codechicken.nei.recipe.ICraftingHandler;

public interface IRecipeInfoExtractor {

    JsonElement extractInfo(ICraftingHandler handler, int recipeIndex);

    String[] getCompatibleHandlers();

    String getSlug();
}
