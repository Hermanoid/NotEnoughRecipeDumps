package com.hermanoid.nerd.info_extractors;

import codechicken.nei.recipe.ICraftingHandler;
import com.google.gson.JsonElement;

public interface IRecipeInfoExtractor {
    public JsonElement extractInfo(ICraftingHandler handler, int recipeIndex);

    public String[] getCompatibleHandlers();

    public String getSlug();
}
