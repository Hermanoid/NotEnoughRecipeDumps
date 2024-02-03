package com.hermanoid.nerd;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {

    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());

        NotEnoughRecipeDumps.LOG.info(Config.greeting);
        NotEnoughRecipeDumps.LOG.info("I am " + Tags.MODNAME + " at version " + Tags.VERSION);
    }
}
