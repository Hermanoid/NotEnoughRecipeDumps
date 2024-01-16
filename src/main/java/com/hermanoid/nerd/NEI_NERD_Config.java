package com.hermanoid.nerd;

import com.hermanoid.nerd.dumpers.DumperRegistry;
import com.hermanoid.nerd.dumpers.GTDefaultRecipeDumper;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import com.hermanoid.nerd.dumpers.GenericDumper;

// This class is automatically discovered by a system in NotEnoughItems
@SuppressWarnings("unused")
public class NEI_NERD_Config implements IConfigureNEI {

    @Override
    public void loadConfig() {
        DumperRegistry.registerDumper(new GenericDumper());
        DumperRegistry.registerDumper(new GTDefaultRecipeDumper());
        API.addOption(new RecipeDumper("tools.dump.recipes"));
    }

    @Override
    public String getName() {
        return "NotEnoughRecipeDumps NEI Plugin";
    }

    @Override
    public String getVersion() {
        return Tags.VERSION;
    }
}
