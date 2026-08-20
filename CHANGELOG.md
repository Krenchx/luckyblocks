# Changelog

## 26.2-13.1 — Minecraft Java Edition 26.2 port

Ported the Fabric 1.19.3-13.1 release to Minecraft Java Edition 26.2 while retaining the original `lucky` namespace and configuration/add-on ecosystem.

### Source recovery and project layout

- Treated `lucky-block-fabric-1.19.3-13.1.jar` as the authoritative binary and resource input.
- Identified the exact public source tag for readable Kotlin source, then cross-checked its metadata, packages, entrypoints, resources, and behavior against the release JAR.
- Restored the common drop parser/evaluator, Java Edition resource loader, Fabric adapter, gameplay objects, translations, models, textures, recipes, structures, and default configuration.
- Removed the empty starter classes, unused example mixins, old spawn-packet implementation, obsolete shaded Kotlin runtime, and outdated loader internals where modern public APIs exist.

### Toolchain and metadata

- Updated Minecraft from 1.19.3 to 26.2.
- Updated Fabric Loader from the original 0.17.3 build environment to 0.19.3.
- Updated Fabric API from 0.73.0+1.19.3 to 0.158.0+26.2.
- Added Fabric Language Kotlin 1.13.13+kotlin.2.4.10 and Kotlin 2.4.10.
- Updated Fabric Loom from 1.1.x to the 1.17 line.
- Updated the wrapper from Gradle 7.2 to Gradle 9.6.1.
- Raised Java compilation and bytecode targets from Java 17 to Java 25.
- Rebuilt `build.gradle`, `gradle.properties`, `settings.gradle`, and `fabric.mod.json` for the modern toolchain.
- Removed the obsolete mappings dependency. Minecraft 26.2 is distributed without the old obfuscation step, so Loom uses the game's official names directly while Fabric handles its runtime namespace requirements.
- Corrected the missing icon referenced by the original metadata.

### Registration and data components

- Replaced the 1.19 registry setup and removed Fabric builder APIs with Minecraft 26.2 registry keys and entity builders.
- Added explicit registry IDs to every block and item property set, as required by 26.2.
- Registered the Lucky Block's block codec in the modern block-type registry.
- Rebuilt block entity and entity type registration, including add-on block validity and velocity/tracking configuration.
- Introduced persistent, network-synchronized `lucky:luck` and `lucky:drops` data components.
- Retained read compatibility for the legacy `Luck` and `Drops` keys while writing the modern namespaced keys.
- Flattened legacy `minecraft:custom_data` on read so Lucky values from data-fixed 1.19.3 item stacks remain visible to the original drop engine.

### Bundled configuration migration

- Kept the 13.1 drop tables, weights, structures, and gameplay intent while converting their old item NBT to 26.2 components.
- Converted generated enchantments to the current ID-to-level map, potion effects to named effect records, and fireworks to `minecraft:fireworks` data.
- Updated configured item stacks, villager trades, custom names, potion contents, player profiles, dyed leather, Lucky item data, and entity equipment to their current codecs.
- Updated renamed entity fields and IDs, including TNT fuse data, elder guardians, cat variants, horse equipment/ownership, and throwable potion entities.
- Replaced obsolete metadata-based quartz, prismarine, and stone-brick variants with their real modern block IDs, and replaced the removed `flowing_lava` ID.
- Moved the editable default configuration to `src/main/lucky-config`; Gradle now builds the embedded configuration archive reproducibly.

### Blocks, items, and persistence

- Ported Lucky Block placement, redstone activation, right-click drops, player breaking, add-on settings, and silk-touch/custom-data flows.
- Migrated block entity, projectile, potion, and delayed-drop persistence from `CompoundTag` callbacks to 26.2 `ValueInput`/`ValueOutput` codecs.
- Preserved legacy projectile NBT parsing so the bundled bow and sword drop configurations continue to create customized projectiles.
- Updated item tooltips to the 26.2 tooltip context and holder lookup.
- Migrated sword construction to `ToolMaterial`/item components and retained its original 7200 durability.
- Updated bow release behavior, sound, ammo consumption, damage, and lucky-drop dispatch for the current item API.
- Updated thrown potion construction, item synchronization, rotations, statistics, and interaction results.
- Added modern item-definition files, including the range/condition-driven pulling model for the Lucky Bow.

### Entities, networking, and rendering

- Replaced the removed custom raw spawn packet with Minecraft/Fabric's native entity synchronization.
- Moved arrow and throwable-projectile references to their 26.2 packages.
- Updated entity movement, rotation, spawn reasons, owner assignment, physics flags, and save/load methods.
- Replaced the old immediate `MultiBufferSource` renderer with 26.2 render-state submission via `ThrownItemRenderer` and `ItemSupplier`.
- Reworked the invisible delayed-drop renderer as a render-state-only renderer.
- Registered renderers through the current widened vanilla `EntityRenderers` API.
- The mod contains no raw OpenGL calls or renderer-specific Vulkan assumptions; all custom entity rendering now goes through Minecraft's backend-neutral submission pipeline.

### Recipes, data generation, and world generation

- Moved recipes from the removed plural `recipes` directory to the 26.2 `recipe` data path.
- Updated shaped recipe ingredients and result stacks to the current JSON schema.
- Reimplemented stateless custom recipe serializers with `MapCodec` and `StreamCodec`.
- Ported luck-modifier crafting to the new `CraftingInput`/assembly contract.
- Reimplemented dynamic add-on shaped and shapeless matching, including mirrored shaped recipes.
- Updated natural generation registration and dimension key handling.
- Added a 26.2 dynamic-registry data provider for the configured and placed Lucky Block features.
- Added data-generation validation that parses all shipped recipes through Minecraft's own 26.2 recipe codec.
- Added codec checks for generated enchantment, potion, firework, profile, dyed-color, and Lucky data components.

### Game API and add-on compatibility

- Updated commands to the 26.2 permission-set model.
- Updated status effects, particles, potion contents, sounds, explosions, structure placement processors, block-state rotation, entity loading, and registry lookups.
- Migrated time operations to the new world-clock system rather than changing game time.
- Refreshes data-driven enchantments after server registry loading and maps them to the legacy Lucky Block template categories.
- Retained external add-on discovery, dynamic block/item registration, crafting definitions, configurations, and structure loading.
- Updated external add-on resource pack exposure to the current Fabric resource-loader activation model.

### Verification

- `compileKotlin` passes on the Java 25 bytecode target.
- `build` produces the remapped distributable and sources JARs.
- `runDatagen` completes successfully, generates both worldgen resources, and validates every shipped recipe codec.
- A Minecraft 26.2 client smoke test reached the title screen with Fabric Loader 0.19.3, loaded all Lucky Block client resources, created all atlases, initialized audio, and reported no Lucky Block model, texture, renderer, or initialization errors.
