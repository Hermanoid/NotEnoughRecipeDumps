package com.hermanoid.nerd;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static String greeting = "Greetings from your favorite NERD (hehe, get it, short for 'Not Enough Recipe Dumps')";

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        greeting = configuration.getString("greeting", Configuration.CATEGORY_GENERAL, greeting, "How shall I greet?");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
