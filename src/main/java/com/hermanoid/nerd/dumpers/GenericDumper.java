package com.hermanoid.nerd.dumpers;

import codechicken.nei.recipe.ICraftingHandler;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hermanoid.nerd.stack_serialization.RecipeDumpContext;
import com.hermanoid.nerd.stack_serialization.SluggerGson;

public class GenericDumper extends BaseRecipeDumper {
    private Gson gson = null;

    @Override
    public void setContext(RecipeDumpContext context){
        super.setContext(context);
        gson = SluggerGson.gsonBuilder(context).create();
    }
    @Override
    public JsonElement dump(ICraftingHandler handler, int recipeIndex) {
        assert gson != null;
        // Surface-level ingredients+other stacks tend not to tell the whole story for modded handlers
        // But, they're also always available and the best option available
        JsonObject recipeDump = new JsonObject();
        recipeDump.add("ingredients", gson.toJsonTree(handler.getIngredientStacks(recipeIndex)));
        recipeDump.add("other_stacks", gson.toJsonTree(handler.getOtherStacks(recipeIndex)));
        if (handler.getResultStack(recipeIndex) != null) {
            recipeDump.add("out_item", gson.toJsonTree(handler.getResultStack(recipeIndex).item));
        }
        return recipeDump;
    }

    @Override
    public String[] getCompatibleHandlers() {
        return new String[]{ FALLBACK_DUMPER_NAME };
    }

    @Override
    public String getSlug() {
        return "generic";
    }
}