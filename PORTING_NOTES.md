# Porting notes

## Authoritative input

- File: `lucky-block-fabric-1.19.3-13.1.jar`
- Size: 2,023,359 bytes
- SHA-256: `260521B91B145480EF039AA705C370A747AF8CF7B3ACD371D94FC8FA7E4361A1`
- Mod: Lucky Block
- Mod ID: `lucky`
- Version: `1.19.3-13.1`
- Author: Alex Socha
- Loader: Fabric
- Original runtime/tool metadata: Minecraft 1.19.3, Java 17, Fabric Loader 0.17.3, Fabric API 0.73.0+1.19.3, Fabric Loom 1.1.14, Gradle 7.2

The JAR contained 895 class files. Of those, 239 were Lucky Block classes and the remainder were relocated Kotlin/runtime classes. The recovered project uses the original source package structure instead of retaining the shaded runtime.

## Architecture

The mod is split into three logical layers:

1. `mod.lucky.common` — attribute parser/evaluator, weighted drops, template variables, actions, structures, and platform-neutral game contracts.
2. `mod.lucky.java` — configuration/add-on discovery, legacy format conversion, Java Edition item/entity data, crafting rules, notifications, and bundled structures.
3. `mod.lucky.fabric` — Minecraft/Fabric registry access, items, blocks, block entities, projectiles, recipes, world generation, client rendering, and resource packs.

There are no mixins in the port and the original release did not contain a custom mixin configuration. There are also no raw OpenGL calls.

## Porting assumptions

- Fabric remains the target loader because the release JAR is a Fabric mod. A loader conversion to NeoForge would be a separate product, not a faithful port.
- Mod version `26.2-13.1` expresses the new game target while retaining the original Lucky Block feature-version lineage.
- The original text configuration and add-on formats are public compatibility surfaces and therefore remain readable.
- Modern namespaced item data is written as `lucky:luck` and `lucky:drops`; legacy capitalized keys are accepted on read.
- The 13.1 default outcome tables remain authoritative, while their embedded vanilla NBT payloads were mechanically migrated to 26.2 entity fields and item components. Editable defaults are kept in `src/main/lucky-config` and bundled during `processResources`.
- Fabric supplies the appropriate resource-pack 88.0 and data-pack 107.1 compatibility metadata for mod resource packs. A single root `pack.mcmeta` is intentionally not added because the client and server pack types have different 26.2 format numbers.
- The default OpenGL backend and experimental Vulkan backend share the new renderer submission path used by the port; the mod does not select or call either graphics API directly.

## Hardest changes

- Replacing 1.19-era registry construction and item NBT with required registry IDs plus typed data components.
- Preserving configuration-driven projectile behavior while moving both entity persistence and rendering to the 26.2 systems.
- Rebuilding runtime add-on recipes around the stateless recipe codec and new assembly contract.
- Moving time manipulation to 26.2 world clocks and updating data-driven enchantment lookup after registry bootstrap.
- Keeping external add-on assets loadable without treating each third-party archive as an installed Fabric mod.

## Residual risks

- No third-party add-on archive was supplied for this port. The add-on registration/resource-pack path compiles and the original discovery/config formats are preserved, but an individual old add-on may still require its own 26.2 item-definition or asset-schema update.
- The client, default-config parser, data generator, recipes, generated data components, resources, registries, and packaged build were exercised. A long-form survival playthrough covering every random outcome was not automated; rare commands or binary structure contents may expose game-specific behavior changes not reached by the smoke tests.
- Legacy `Luck`/`Drops` item data is handled automatically. A third-party add-on that embeds unrelated pre-component vanilla item NBT (for example `display` or `Enchantments`) must migrate that payload to a 26.2 `components` map.
- Data-driven enchantments introduced after the original release use a maintainable `BREAKABLE` fallback unless their ID is mapped to a more specific legacy template category.
