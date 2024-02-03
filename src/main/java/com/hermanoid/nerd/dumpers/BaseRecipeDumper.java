package com.hermanoid.nerd.dumpers;

import com.google.gson.JsonElement;
import com.hermanoid.nerd.stack_serialization.RecipeDumpContext;

import codechicken.nei.recipe.ICraftingHandler;

public abstract class BaseRecipeDumper {

    public final static String FALLBACK_DUMPER_NAME = "<FALLBACK>";

    public abstract JsonElement dump(ICraftingHandler handler, int recipeIndex);

    public abstract String[] getCompatibleHandlers();

    public abstract String getSlug();

    protected RecipeDumpContext context;

    // In retrospect, I'm not a huge fan of Dumpers-manage-their-own-context paradigm
    // Dumpers need to know when the context changes/resets to update their GSON instances, etc.
    // but that could be covered more simply by a singleton context (or context manager instance) with an update event
    // but this does work, so unless I have a reason to change it, it'll stay.
    public void setContext(RecipeDumpContext context) {
        this.context = context;
    }
}
