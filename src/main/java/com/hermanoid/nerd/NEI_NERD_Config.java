package com.hermanoid.nerd;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;

public class NEI_NERD_Config implements IConfigureNEI {
    @Override
    public void loadConfig() {
        API.addOption(new RecipeDumper("tools.dump.recipes"));
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
