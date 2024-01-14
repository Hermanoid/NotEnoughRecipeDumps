package com.hermanoid.nerd;

import com.hermanoid.nerd.info_extractors.GTDefaultRecipeInfoExtractor;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;

// This class is automatically discovered by a system in NotEnoughItems
@SuppressWarnings("unused")
public class NEI_NERD_Config implements IConfigureNEI {

    @Override
    public void loadConfig() {
        RecipeDumper recipeDumper = new RecipeDumper("tools.dump.recipes");
        recipeDumper.registerRecipeInfoExtractor(new GTDefaultRecipeInfoExtractor());
        API.addOption(recipeDumper);
    }

    @Override
    public String getName() {
        return "NotEnoughRecipeDumps NEI Plugin";
    }

    @Override
    public String getVersion() {
        return "(1.0)";
    }
}
