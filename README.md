# Lucky Block for Fabric 26.2

This is the Minecraft Java Edition 26.2 port of Lucky Block 1.19.3-13.1. It preserves the original mod ID (`lucky`), configuration format, random-drop engine, blocks, items, entities, recipes, structures, natural generation, and Lucky Block add-on discovery.

## Requirements

- Minecraft Java Edition 26.2
- Java SE 25 or newer
- Fabric Loader 0.19.3 or newer compatible 0.19 release
- Fabric API 0.158.0+26.2
- Fabric Language Kotlin 1.13.13+kotlin.2.4.10 or newer compatible release

The project uses the included Gradle 9.6.1 wrapper and Fabric Loom 1.17.

## Build

Set `JAVA_HOME` to a Java 25+ JDK, then run:

```powershell
.\gradlew.bat clean build
```

On macOS or Linux:

```bash
./gradlew clean build
```

The distributable mod is written to `build/libs/lucky-block-fabric-26.2-13.1.jar`. Install it alongside Fabric API and Fabric Language Kotlin; do not install the sources JAR.

## Development checks

```powershell
.\gradlew.bat runDatagen
.\gradlew.bat runClient
```

Data generation parses every shipped recipe through Minecraft 26.2's recipe codec and validates representative generated enchantment, potion, firework, profile, dyed-color, and Lucky data components. Generated world-generation resources are kept in `src/main/generated` and are included in normal builds.

## Configuration and add-ons

On first launch, the default configuration and bundled structures are extracted below the game directory under:

```text
config/lucky/26.2-13.1-fabric/
```

The legacy text configuration syntax remains supported. Existing add-ons are still discovered and registered at startup. Their blocks/items use the modern registry and data-component systems, and their client assets are exposed as always-enabled resource packs.

The editable bundled defaults live in `src/main/lucky-config`. The build packages them into the internal `lucky-config.zip` automatically; no manual archive step is required.

See [CHANGELOG.md](CHANGELOG.md) for the full port record and [PORTING_NOTES.md](PORTING_NOTES.md) for source provenance, architecture, assumptions, and residual compatibility risks.
