package com.hermanoid.nerd.stack_serialization;

import java.lang.reflect.Type;
import java.util.Set;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

public class SetExclusionStrategy implements ExclusionStrategy {

    private final Set<String> badFields;
    private final Set<Type> badTypes;

    public SetExclusionStrategy(Set<String> badFields, Set<Type> badTypes) {
        this.badFields = badFields;
        this.badTypes = badTypes;
    }

    @Override
    public boolean shouldSkipField(FieldAttributes f) {

        return badFields.contains(f.getName());
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
        return badTypes.contains(clazz);
    }
}
