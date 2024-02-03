package com.hermanoid.nerd.dumpers;

import java.util.Collection;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.hermanoid.nerd.stack_serialization.RecipeDumpContext;

public class DumperRegistry {

    protected static Multimap<String, BaseRecipeDumper> dumperMap = HashMultimap.create();
    protected static RecipeDumpContext context;

    public static void registerDumper(BaseRecipeDumper dumper) {
        for (String id : dumper.getCompatibleHandlers()) dumperMap.put(id, dumper);
    }

    public static void setContext(RecipeDumpContext context) {
        for (BaseRecipeDumper dumper : dumperMap.values()) {
            dumper.setContext(context);
        }
    }

    public static boolean containsKey(String key) {
        return dumperMap.containsKey(key);
    }

    public static Collection<BaseRecipeDumper> get(String key) {
        return dumperMap.get(key);
    }

}
