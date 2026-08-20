package mod.lucky.fabric

import com.mojang.logging.LogUtils
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import mod.lucky.common.GAME_API
import mod.lucky.common.LOGGER
import mod.lucky.common.PLATFORM_API
import mod.lucky.fabric.game.*
import mod.lucky.java.*
import mod.lucky.java.game.LuckyItemValues
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.biome.v1.BiomeModifications
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType
import net.fabricmc.fabric.impl.resource.ResourceLoaderImpl
import net.fabricmc.loader.api.ModContainer
import net.fabricmc.loader.api.Version
import net.fabricmc.loader.api.metadata.*
import net.fabricmc.loader.impl.metadata.ModOriginImpl
import net.fabricmc.loader.impl.util.FileSystemUtil
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.util.ExtraCodecs
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.client.renderer.entity.EntityRenderers
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.levelgen.GenerationStep
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration
import java.nio.file.Path
import java.util.Optional

private fun id(value: String): Identifier = Identifier.parse(value)
private fun blockKey(value: Identifier) = ResourceKey.create(Registries.BLOCK, value)
private fun entityKey(value: Identifier) = ResourceKey.create(Registries.ENTITY_TYPE, value)

object FabricLuckyRegistry {
    val LOGGER = LogUtils.getLogger()
    const val modId = "lucky"
    const val luckyWorldFeatureId = "lucky:lucky_world_gen"

    lateinit var luckComponent: DataComponentType<Int>
    lateinit var dropsComponent: DataComponentType<List<String>>
    lateinit var luckyBlock: LuckyBlock
    lateinit var luckyBlockItem: LuckyBlockItem
    lateinit var luckyBow: LuckyBow
    lateinit var luckySword: LuckySword
    lateinit var luckyPotion: LuckyPotion
    lateinit var luckyBlockEntity: BlockEntityType<LuckyBlockEntity>
    lateinit var luckyProjectile: EntityType<LuckyProjectile>
    lateinit var thrownLuckyPotion: EntityType<ThrownLuckyPotion>
    lateinit var delayedDrop: EntityType<DelayedDrop>
    lateinit var luckModifierCraftingRecipe: RecipeSerializer<LuckModifierCraftingRecipe>
    lateinit var addonCraftingRecipe: RecipeSerializer<AddonCraftingRecipe>

    val addonBlocks = linkedMapOf<String, LuckyBlock>()
    val addonItems = linkedMapOf<String, Item>()
}

class FabricMod : ModInitializer {
    init {
        PLATFORM_API = JavaPlatformAPI
        LOGGER = FabricGameAPI
        GAME_API = FabricGameAPI
        JAVA_GAME_API = FabricJavaGameAPI
    }

    override fun onInitialize() {
        registerDataComponents()
        FabricGameAPI.init()
        JavaLuckyRegistry.init()
        registerBlocksAndItems()
        registerEntities()
        registerRecipes()
        registerWorldGen()
        registerAddonCraftingRecipes()
        setupCreativeTabs()

        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            FabricGameAPI.refreshDynamicRegistries(server.registryAccess())
        }
    }

    private fun registerDataComponents() {
        FabricLuckyRegistry.luckComponent = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            id("lucky:luck"),
            DataComponentType.builder<Int>()
                .persistent(ExtraCodecs.intRange(-100, 100))
                .networkSynchronized(ByteBufCodecs.VAR_INT)
                .build()
        )
        FabricLuckyRegistry.dropsComponent = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            id("lucky:drops"),
            DataComponentType.builder<List<String>>()
                .persistent(Codec.STRING.listOf())
                .networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()))
                .build()
        )
    }

    private fun makeBlock(blockId: String): LuckyBlock {
        val identifier = id(blockId)
        return LuckyBlock(LuckyBlock.properties().setId(blockKey(identifier)))
    }

    private fun registerBlocksAndItems() {
        Registry.register(BuiltInRegistries.BLOCK_TYPE, id(JavaLuckyRegistry.blockId), LuckyBlock.CODEC)

        FabricLuckyRegistry.luckyBlock = Registry.register(
            BuiltInRegistries.BLOCK,
            id(JavaLuckyRegistry.blockId),
            makeBlock(JavaLuckyRegistry.blockId)
        )
        FabricLuckyRegistry.luckyBlockItem = Registry.register(
            BuiltInRegistries.ITEM,
            id(JavaLuckyRegistry.blockId),
            LuckyBlockItem(FabricLuckyRegistry.luckyBlock, id(JavaLuckyRegistry.blockId))
        )

        JavaLuckyRegistry.addons.forEach { addon ->
            addon.ids.block?.let { blockId ->
                val block = Registry.register(BuiltInRegistries.BLOCK, id(blockId), makeBlock(blockId))
                val item = Registry.register(BuiltInRegistries.ITEM, id(blockId), LuckyBlockItem(block, id(blockId)))
                FabricLuckyRegistry.addonBlocks[blockId] = block
                FabricLuckyRegistry.addonItems[blockId] = item
            }
            addon.ids.sword?.let { itemId ->
                FabricLuckyRegistry.addonItems[itemId] = Registry.register(BuiltInRegistries.ITEM, id(itemId), LuckySword(id(itemId)))
            }
            addon.ids.bow?.let { itemId ->
                FabricLuckyRegistry.addonItems[itemId] = Registry.register(BuiltInRegistries.ITEM, id(itemId), LuckyBow(id(itemId)))
            }
            addon.ids.potion?.let { itemId ->
                FabricLuckyRegistry.addonItems[itemId] = Registry.register(BuiltInRegistries.ITEM, id(itemId), LuckyPotion(id(itemId)))
            }
        }

        FabricLuckyRegistry.luckySword = Registry.register(
            BuiltInRegistries.ITEM, id(JavaLuckyRegistry.swordId), LuckySword(id(JavaLuckyRegistry.swordId))
        )
        FabricLuckyRegistry.luckyBow = Registry.register(
            BuiltInRegistries.ITEM, id(JavaLuckyRegistry.bowId), LuckyBow(id(JavaLuckyRegistry.bowId))
        )
        FabricLuckyRegistry.luckyPotion = Registry.register(
            BuiltInRegistries.ITEM, id(JavaLuckyRegistry.potionId), LuckyPotion(id(JavaLuckyRegistry.potionId))
        )

        val validBlocks: Set<Block> = (listOf(FabricLuckyRegistry.luckyBlock) + FabricLuckyRegistry.addonBlocks.values).toSet()
        FabricLuckyRegistry.luckyBlockEntity = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            id(JavaLuckyRegistry.blockId),
            BlockEntityType(::LuckyBlockEntity, validBlocks)
        )
    }

    private fun registerEntities() {
        val projectileId = id(JavaLuckyRegistry.projectileId)
        FabricLuckyRegistry.luckyProjectile = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            projectileId,
            EntityType.Builder.of({ type, level -> LuckyProjectile(type, level) }, MobCategory.MISC)
                .clientTrackingRange(100)
                .updateInterval(20)
                .alwaysUpdateVelocity(true)
                .build(entityKey(projectileId))
        )

        val potionId = id(JavaLuckyRegistry.potionId)
        FabricLuckyRegistry.thrownLuckyPotion = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            potionId,
            EntityType.Builder.of({ type, level -> ThrownLuckyPotion(type, level) }, MobCategory.MISC)
                .clientTrackingRange(100)
                .updateInterval(20)
                .alwaysUpdateVelocity(true)
                .build(entityKey(potionId))
        )

        val delayedId = id(JavaLuckyRegistry.delayedDropId)
        FabricLuckyRegistry.delayedDrop = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            delayedId,
            EntityType.Builder.of({ type, level -> DelayedDrop(type, level) }, MobCategory.MISC)
                .clientTrackingRange(100)
                .updateInterval(20)
                .build(entityKey(delayedId))
        )
    }

    private fun registerRecipes() {
        val luckRecipe = LuckModifierCraftingRecipe()
        FabricLuckyRegistry.luckModifierCraftingRecipe = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            id("lucky:crafting_luck"),
            RecipeSerializer(MapCodec.unit(luckRecipe), StreamCodec.unit<RegistryFriendlyByteBuf, LuckModifierCraftingRecipe>(luckRecipe))
        )

        val addonRecipe = AddonCraftingRecipe()
        FabricLuckyRegistry.addonCraftingRecipe = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            id("lucky:crafting_addons"),
            RecipeSerializer(MapCodec.unit(addonRecipe), StreamCodec.unit<RegistryFriendlyByteBuf, AddonCraftingRecipe>(addonRecipe))
        )
    }

    private fun registerWorldGen() {
        val featureId = id(FabricLuckyRegistry.luckyWorldFeatureId)
        Registry.register(BuiltInRegistries.FEATURE, featureId, LuckyWorldFeature(NoneFeatureConfiguration.CODEC))
        BiomeModifications.addFeature(
            BiomeSelectors.all(),
            GenerationStep.Decoration.SURFACE_STRUCTURES,
            ResourceKey.create(Registries.PLACED_FEATURE, featureId)
        )
    }

    private fun setupCreativeTabs() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register { output ->
            output.accept(FabricLuckyRegistry.luckyBlockItem)
            createLuckySubItems(FabricLuckyRegistry.luckyBlockItem, output.context.holders()).forEach(output::accept)
            JavaLuckyRegistry.addons.mapNotNull { it.ids.block }.mapNotNull(FabricLuckyRegistry.addonItems::get).forEach(output::accept)
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register { output ->
            output.accept(FabricLuckyRegistry.luckySword)
            output.accept(FabricLuckyRegistry.luckyBow)
            output.accept(FabricLuckyRegistry.luckyPotion)
            createLuckySubItems(
                FabricLuckyRegistry.luckyPotion,
                output.context.holders(),
                LuckyItemValues.veryLuckyPotion,
                LuckyItemValues.veryUnluckyPotion
            ).forEach(output::accept)
            JavaLuckyRegistry.addons.flatMap { listOfNotNull(it.ids.sword, it.ids.bow, it.ids.potion) }
                .mapNotNull(FabricLuckyRegistry.addonItems::get)
                .forEach(output::accept)
        }
    }
}

@OnlyInClient
class FabricModClient : ClientModInitializer {
    override fun onInitializeClient() {
        registerAddonResourcePacks()

        ClientChunkEvents.CHUNK_LOAD.register { _, _ ->
            JavaLuckyRegistry.notificationState = checkForUpdates(JavaLuckyRegistry.notificationState)
        }

        EntityRenderers.register(FabricLuckyRegistry.luckyProjectile, ::LuckyProjectileRenderer)
        EntityRenderers.register(FabricLuckyRegistry.thrownLuckyPotion, ::ThrownLuckyPotionRenderer)
        EntityRenderers.register(FabricLuckyRegistry.delayedDrop, ::DelayedDropRenderer)
    }

    private fun registerAddonResourcePacks() {
        JavaLuckyRegistry.addons.forEach { addon ->
            val metadata = object : ModMetadata {
                override fun getType() = "builtin"
                override fun getId() = "${addon.addonId.replace(':', '_')}_resources"
                override fun getProvides(): Collection<String> = emptyList()
                override fun getVersion(): Version = Version.parse("1.0.0")
                override fun getEnvironment() = ModEnvironment.CLIENT
                override fun getDependencies(): Collection<ModDependency> = emptyList()
                override fun getName() = "${addon.addonId} resources"
                override fun getDescription() = "Lucky Block add-on resources"
                override fun getAuthors(): Collection<Person> = emptyList()
                override fun getContributors(): Collection<Person> = emptyList()
                override fun getContact(): ContactInformation = ContactInformation.EMPTY
                override fun getLicense(): Collection<String> = emptyList()
                override fun getIconPath(size: Int): Optional<String> = Optional.empty()
                override fun containsCustomValue(key: String) = false
                override fun getCustomValue(key: String): CustomValue? = null
                override fun getCustomValues(): Map<String, CustomValue> = emptyMap()
                @Deprecated("Legacy metadata API")
                override fun containsCustomElement(key: String) = false
            }

            val container = object : ModContainer {
                private val roots: List<Path> by lazy {
                    if (addon.file.isDirectory) listOf(addon.file.toPath())
                    else listOf(FileSystemUtil.getJarFileSystem(addon.file.toPath(), false).get().rootDirectories.first())
                }

                override fun getMetadata(): ModMetadata = metadata
                override fun getOrigin(): ModOrigin = ModOriginImpl(listOf(addon.file.toPath()))
                override fun getRootPaths(): List<Path> = roots
                override fun getContainingMod(): Optional<ModContainer> = Optional.empty()
                override fun getContainedMods(): Collection<ModContainer> = emptyList()
                @Deprecated("Legacy loader API")
                override fun getRootPath(): Path = roots.first()
                @Deprecated("Legacy loader API")
                override fun getPath(file: String): Path = findPath(file).orElseThrow()
            }

            val packId = id("lucky:${addon.addonId.substringAfter(':').replace('/', '_')}_resources")
            val registered = ResourceLoaderImpl.registerBuiltinPack(
                packId,
                "",
                container,
                Component.literal("Resources for ${addon.addonId}"),
                PackActivationType.ALWAYS_ENABLED
            )
            if (!registered) FabricLuckyRegistry.LOGGER.warn("Could not register resources for Lucky Block add-on {}", addon.addonId)
        }
    }
}
