package com.hermanoid.nerd.stack_serialization;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.gson.GsonBuilder;
import gregtech.api.enums.Materials;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.RegistryNamespaced;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

import codechicken.nei.util.NBTJson;
import gregtech.common.fluid.GT_Fluid;

public class RecipeDumpContext {

    private static final RegistryNamespaced itemRegistry = Item.itemRegistry;
    private static final Type fluidType = new TypeToken<Fluid>() {}.getType();

    public Map<String, JsonObject> dumpedItems = new HashMap<>();
    public Map<String, JsonObject> dumpedFluids = new HashMap<>();


    private final static Gson gson;
    static {
        List<String> badFields = ImmutableList.of(
            // Icons are very large, not wise to have stored every time we dump an item
            // We're just going to say "dump them separately yourself, using the already-provided dumper"
            "stillIconResourceLocation",
            "flowingIconResourceLocation",
            "stillIcon",
            "flowingIcon",
            // There's a recursive fluid definition here
            "registeredFluid",
            // The automatic serializer doesn't like the rarity enum, I dunno why
            "rarity",
            // I don't think a fluid's corresponding block is useful, and it causes breaky recursion
            "block"
        );
        List<Type> badTypes = Collections.singletonList(Materials.class);
        SetExclusionStrategy exclusionStrategy =
            new SetExclusionStrategy(
                new HashSet<>(badFields),
                new HashSet<>(badTypes));
        gson = new GsonBuilder()
            // We might be only doing serializations, but GSON will still create
            // a type adapter and get stuck in nasty recursion/type access land
            // if it thinks it might need to do deserialization.
            .addSerializationExclusionStrategy(exclusionStrategy)
            .addDeserializationExclusionStrategy(exclusionStrategy)
            .create();
    }

    private JsonObject stackToDetailedJson(ItemStack stack) {
        // For simplicity, just manually pick-and-choose fields to dump
        // rather than doing a gson.toJsonTree-based automatic dump
        // ... because there would be a lot of things to exclude and some extra method outputs to add later anyway
        JsonObject itemObj = new JsonObject();
        Item item = stack.getItem();
        itemObj.addProperty("id", itemRegistry.getIDForObject(item));
        itemObj.addProperty("regName", itemRegistry.getNameForObject(item));
        itemObj.addProperty("name", item != null ? stack.getUnlocalizedName() : "null");
        itemObj.addProperty("displayName", stack.getDisplayName());

        NBTTagCompound tag = stack.writeToNBT(new NBTTagCompound());
        itemObj.add("nbt", NBTJson.toJsonObject(tag));

        // I think there will be extra metadata/info here.
        return itemObj;
    }

    public JsonObject fluidToDetailedJson(Fluid src) {
        // Some fluids (like water) are defined using anonymous types
        // I think that specifying the type as Fluid (not GT) in all cases could throw away information,
        // but for non-GT_Fluids, we'll have to specify it to un-anonymize this beeswax.
        JsonObject fluid;
        if (src.getClass()
            .equals(GT_Fluid.class)) {
            fluid = (JsonObject) gson.toJsonTree(src);
        } else {
            fluid = (JsonObject) gson.toJsonTree(src, fluidType);
        }
        // Manually serialize rarity bc wierdness
        fluid.addProperty("rarity", src.getRarity().toString());
        // Slap on some info that's only available via method calls
        fluid.addProperty("id", src.getID());
        return fluid;
    }

    private final static HashSet<String> standardNbtKeys
        = new HashSet<>(Arrays.asList("id", "Count", "Damage"));

    // Gets a minimal identifier for an item
    // Most data (names, etc) is stored separately, once
    // Only some stuff (count, a slug, rarely some extra NBT stuffs) needs to be stored every time
    //
    // Format note: if the stack has no extra NBT and a count of 1, only the slug is returned
    // this is a very common case so it's worth adding an exception for it to reduce dump sizes
    //
    // There is also the matter of how many/most (but not all) fluids in GTNewHorizons have a corresponding item-based
    // "FluidDisplay" provided by greg, sometimes as an ingredient and sometimes as an "otherStack"
    // Some recipes have one or the other and I haven't a clue what decides whether a recipe gets one, the other, or both
    // I'll leave resolving combining fluids+item displays to the consumer of the dump
    // However, to (pretty dramatically) cut down on export size, I'll refer to the fluid slug (stored as Damage)
    // instead of doing a normal NBT dump
    public JsonElement getMinimalItemDump(ItemStack stack) {
        // Damage is often used to differentiate items with the same name
        // (e.g. all potions have the name "potion")
        NBTTagCompound tag = new NBTTagCompound();
        stack.writeToNBT(tag);
        // Fluid Display special case
        if (itemRegistry.getIDForObject(stack.getItem()) == 4356) {
            // ye olde bait-and-switch (lol noob you get a fluid instead)
            // My apologies to whoever has to parse this json in the future (me)
            // Note that we do rely on there being another actual fluid dump of this in the dumpedFluids cache
            // We don't have all the data available to check+dump here
            return buildMinimalFluidDump(
                Short.toString(tag.getShort("Damage")),
                (int) tag.getLong("mFluidDisplayAmount"),
                null);
        }
        String slug = tag.getInteger("id") + "d" + tag.getShort("Damage");
        if (!dumpedItems.containsKey(slug)) {
            dumpedItems.put(slug, stackToDetailedJson(stack));
        }

        byte count = tag.getByte("Count");
        // func_150296_c = get HashSet of keys from NBT
        // count is sometimes left as 0, we assume this implies 1
        if ((count == 0 || count == 1) && standardNbtKeys.containsAll(tag.func_150296_c())) {
            // Shortened case (implied count of 1, no extra NBT)
            return new JsonPrimitive(slug);
        } else {
            JsonObject itemObj = new JsonObject();
            itemObj.addProperty("itemSlug", slug);
            itemObj.addProperty("count", count);
            for (String key : standardNbtKeys) tag.removeTag(key);
            if(!tag.hasNoTags()){
                itemObj.add("NBT", NBTJson.toJsonObject(tag));
            }
            return itemObj;
        }
    }

    // Similar to items, most of the time fluids aren't very unique, so we can mostly just store a slug
    // A difference is that amounts vary a lot, no "implied count of 1" here.
    // Also, so far as I can tell, there are no different-subitem-implied-by-Damage-or-otherwise situations,
    // so the ID is enough to... IDentify... the fluid (:insert_mind_blown_emoji_here:)
    public JsonObject getMinimalFluidDump(FluidStack fluid) {
        String slug = Integer.toString(fluid.getFluidID());
        if (!dumpedFluids.containsKey(slug)) {
            dumpedFluids.put(slug, fluidToDetailedJson(fluid.getFluid()));
        }
        // No special cases
        // So far as I can tell, there's no special NBT values. If there's a tag, it's worth dumping all of it.
        return buildMinimalFluidDump(slug, fluid.amount, fluid.tag);
    }

    // Making my code as DRY as the atmosphere in Minnesota right now (extremely)
    private JsonObject buildMinimalFluidDump(String fluidSlug, int amount, NBTTagCompound tag) {
        JsonObject fluidObj = new JsonObject();
        fluidObj.addProperty("fluidSlug", fluidSlug);
        fluidObj.addProperty("amount", amount);
        if (tag != null && !tag.hasNoTags()) {
            fluidObj.add("NBT", NBTJson.toJsonObject(tag));
        }
        return fluidObj;
    }

    public JsonObject dump() {
        JsonObject root = new JsonObject();
        root.add("items", gson.toJsonTree(dumpedItems));
        root.add("fluids", gson.toJsonTree(dumpedFluids));
        return root;
    }
}
