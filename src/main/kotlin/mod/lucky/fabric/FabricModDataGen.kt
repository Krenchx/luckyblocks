package mod.lucky.fabric

import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import mod.lucky.common.DropTemplateContext
import mod.lucky.common.KotlinRandom
import mod.lucky.common.LuckyRegistry
import mod.lucky.common.attribute.DictAttr
import mod.lucky.common.attribute.ListAttr
import mod.lucky.common.attribute.TemplateVar
import mod.lucky.common.attribute.dictAttrOf
import mod.lucky.common.attribute.intAttrOf
import mod.lucky.common.attribute.listAttrOf
import mod.lucky.common.attribute.stringAttrOf
import mod.lucky.java.JAVA_GAME_API
import mod.lucky.fabric.game.*
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider
import net.minecraft.core.Holder
import net.minecraft.core.HolderLookup
import net.minecraft.core.Registry
import net.minecraft.core.RegistrySetBuilder
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.RegistryOps
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import java.util.concurrent.CompletableFuture
import java.io.InputStreamReader


class WorldGenerator(output: FabricPackOutput, registriesFuture: CompletableFuture<HolderLookup.Provider>) :
    FabricDynamicRegistryProvider(output, registriesFuture) {
    override fun configure(registries: HolderLookup.Provider, entries: Entries) {
        validateRecipes(registries)
        validateGeneratedComponents(registries)
        entries.addAll(registries.lookupOrThrow(Registries.CONFIGURED_FEATURE))
        entries.addAll(registries.lookupOrThrow(Registries.PLACED_FEATURE))
    }

    private fun validateGeneratedComponents(registries: HolderLookup.Provider) {
        FabricGameAPI.refreshDynamicRegistries(registries)
        val ops = RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, registries)
        val context = DropTemplateContext(null, null, KotlinRandom(kotlin.random.Random(13)))

        fun template(name: String) = LuckyRegistry.templateVarFns.getValue(name)(TemplateVar(name, ListAttr()), context)
        fun validateComponents(components: DictAttr) {
            val tag = JAVA_GAME_API.attrToNBT(components) as CompoundTag
            DataComponentMap.CODEC.parse(ops, tag).getOrThrow()
        }

        val swordEnchantments = template("luckySwordEnchantments")
        validateComponents(dictAttrOf("minecraft:enchantments" to swordEnchantments))
        validateComponents(dictAttrOf("minecraft:stored_enchantments" to template("randEnchantment")))
        validateComponents(template("randFireworksRocket") as DictAttr)
        validateComponents(dictAttrOf(
            "minecraft:potion_contents" to dictAttrOf(
                "potion" to stringAttrOf("minecraft:fire_resistance"),
                "custom_effects" to template("luckyPotionEffects"),
            ),
        ))
        validateComponents(dictAttrOf("minecraft:dyed_color" to intAttrOf(FabricGameAPI.getRGBPalette().first())))
        validateComponents(dictAttrOf("minecraft:profile" to stringAttrOf("Player")))
        validateComponents(dictAttrOf("lucky:luck" to intAttrOf(80)))

    }

    private fun validateRecipes(registries: HolderLookup.Provider) {
        val ops = RegistryOps.create(JsonOps.INSTANCE, registries)
        listOf("addons", "luck", "lucky_block").forEach { name ->
            val path = "/data/lucky/recipe/$name.json"
            val stream = javaClass.getResourceAsStream(path)
                ?: error("Missing recipe resource $path")
            stream.use {
                val json = JsonParser.parseReader(InputStreamReader(it))
                Recipe.CODEC.parse(ops, json).getOrThrow()
            }
        }
    }

    override fun getName(): String {
        return "lucky"
    }
}

class FabricModDataGen : DataGeneratorEntrypoint {
    override fun onInitializeDataGenerator(fabricDataGenerator: FabricDataGenerator) {
        val pack = fabricDataGenerator.createPack()
        pack.addProvider(::WorldGenerator)
    }

    override fun buildRegistry(registryBuilder: RegistrySetBuilder) {
        val featureId = MCIdentifier.parse(FabricLuckyRegistry.luckyWorldFeatureId)
        @Suppress("UNCHECKED_CAST")
        val feature = BuiltInRegistries.FEATURE.getOptional(featureId).orElseThrow()
            as net.minecraft.world.level.levelgen.feature.Feature<NoneFeatureConfiguration>
        val configuredFeature = ConfiguredFeature(feature, NoneFeatureConfiguration());
        val placedFeature = PlacedFeature(Holder.direct(configuredFeature), emptyList())

        registryBuilder.add(Registries.CONFIGURED_FEATURE) { registry ->
            val configuredKey = ResourceKey.create(Registries.CONFIGURED_FEATURE, featureId)
            registry.register(configuredKey, configuredFeature)
        }
        registryBuilder.add(Registries.PLACED_FEATURE) { registry ->
            val placedKey = ResourceKey.create(Registries.PLACED_FEATURE, featureId)
            registry.register(placedKey, placedFeature)
        }
    }
}
