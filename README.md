## NERD - Not Enough Recipe Dumps

---

At long last, you can finally export all that delicious recipe data from just about every mod, automatically, into one huge and hideous dump file!


This (somewhat) simple project adds an extra dump option to the NEI data dumps screen (Lower-left of inventory screen>Tools>Dumps).
Why would you ever want this? Well, sometimes, those of us foolish enough to take on the GTNewHorizons modpack look up from our sprawling platline/titanium/whatever spreadsheets (I know, this is already sounding unrealistic), and we think, "boy wouldn't it be nice if I could write a script to calculate this crap." But to manually enter recipe data into a script like some sort of __plebian?__ *scoff*

### But why do it this ugly way?

Other methods of accessing recipe data are limited. You can:
1. Dig up crafting .json files, but those are almost strictly limited to vanilla shaped/shapeless crafting recipes. Most mods don't even do this, and instead add crafting recipes programmatically at game startup.
2. Dig into the mod's source code. Some mods (notably AE2) have non-sucky ways of storing recipes in nice config files.
3. Write a mods that interfaces with a mod's own recipe implementation. GT5-Unofficial sports a *chonking* RecipeMap that's actually quite nice.
4. Probably something else I didn't see while spiralling through layers of there-has-to-be-a-path-where-I-don't-write-Java denial.

But each of these approaches are limited in how many recipes they can dig up. If you really want *all* recipes for something, there's only one end-all place to get it - NotEnoughItem's GUI. And so that's exactly where this mod looks.

Every mod that integrates with NEI provides one or more recipe handlers. When you ask for how to make/use an item, NEI queries all registered handlers (in the GTNH fork of NEI, this happens [mostly] in parallel). Each handler that offers at least one recipe for that query, becomes a tab in the resulting GUI.

These handlers are rich with all sorts of UI stuff - we're digging into a UI system, after all. But this dumper ain't got time for that. It iterates through every item in the item panel (the list of all items you can see/search) and queries all recipe handlers for how to craft that item. They return a bunch of UI stuff for drawing those recipes to the screen. NERD ignores all that stuff and scrapes item details, other associated items (e.g. fuel), and some extra metadata when it's available (mostly targeted at Gregtech b/c that's what the author cares about, sorry). This all gets dumped into one chonky-lad JSON file.

This JSON file is hideous and has a number of problems. Fuel, for example, cycles on-screen through everything you could use as fuel. This dumper just grabs the first item in that cycle, the sapling. There's a lot of work that could be done to pretty things up. If you're willing to do that and PR it, by all means. I've elected to do this cleaning in Python because Python less suck.
