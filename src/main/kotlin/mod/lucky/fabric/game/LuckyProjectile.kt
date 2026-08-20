package mod.lucky.fabric.game

import com.mojang.serialization.Codec
import mod.lucky.common.GAME_API
import mod.lucky.common.drop.dropsFromStrList
import mod.lucky.java.JavaLuckyRegistry
import mod.lucky.java.game.LuckyProjectileData
import mod.lucky.java.game.onImpact
import mod.lucky.java.game.readFromTag
import mod.lucky.java.game.tick
import mod.lucky.java.game.writeToTag
import mod.lucky.fabric.*
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.ThrownItemRenderer
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.projectile.ItemSupplier
import net.minecraft.world.entity.projectile.arrow.Arrow
import net.minecraft.world.item.Items
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import kotlin.jvm.optionals.getOrDefault
import kotlin.jvm.optionals.getOrNull

private val defaultDisplayItemStack = MCItemStack(Items.STICK)

class LuckyProjectile(
    type: EntityType<LuckyProjectile> = FabricLuckyRegistry.luckyProjectile,
    world: MCWorld,
    private var data: LuckyProjectileData = LuckyProjectileData(),
) : Arrow(type, world), ItemSupplier {
    companion object {
        val ITEM_STACK: EntityDataAccessor<MCItemStack> = SynchedEntityData.defineId(
            LuckyProjectile::class.java, EntityDataSerializers.ITEM_STACK
        )
    }

    override fun defineSynchedData(p0: SynchedEntityData.Builder) {
        super.defineSynchedData(p0)
        p0.define(ITEM_STACK, MCItemStack.EMPTY)
    }

    override fun tick() {
        super.tick()
        if (!isClientWorld(level())) data.tick(level(), this, owner, tickCount)
    }

    override fun onHit(hitResult: HitResult) {
        super.onHit(hitResult)
        if (hitResult.type != HitResult.Type.MISS){
            if (!isClientWorld(level())) {
                val hitEntity: MCEntity? = (hitResult as? EntityHitResult)?.entity
                data.onImpact(level(), this, owner, hitEntity)
            }
            remove(RemovalReason.DISCARDED)
        }
    }
    override fun readAdditionalSaveData(tag: ValueInput) {
        super.readAdditionalSaveData(tag)

        try {
            val storedData = tag.read("LuckyData", CompoundTag.CODEC)
            data = if (storedData.isPresent) {
                LuckyProjectileData.readFromTag(storedData.get())
            } else {
                LuckyProjectileData(
                    trailFreqPerTick = tag.child("trail").getOrNull()?.getDoubleOr("frequency", 0.0) ?: 0.0,
                    trailDrops = dropsFromStrList(
                        tag.child("trail").getOrNull()?.listOrEmpty("drops", Codec.STRING)?.toList() ?: emptyList()
                    ),
                    impactDrops = dropsFromStrList(tag.listOrEmpty("impact", Codec.STRING).toList()),
                    sourceId = tag.getString("sourceId").getOrNull() ?: JavaLuckyRegistry.blockId,
                )
            }

            val storedItem = tag.read("DisplayItem", MCItemStack.CODEC)
            if (storedItem.isPresent) {
                entityData.set(ITEM_STACK, storedItem.get())
            } else {
                val itemInput = tag.child("item").getOrNull() ?: tag.child("Item").getOrNull()
                if (itemInput != null) {
                    val rawId = itemInput.getString("id").getOrDefault("minecraft:stick")
                    val itemId = MCIdentifier.parse(rawId)
                    val item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.STICK)
                    val stack = MCItemStack(item, 1)
                    itemInput.read("components", DataComponentMap.CODEC).getOrNull()?.let(stack::applyComponents)
                    entityData.set(ITEM_STACK, stack)
                } else {
                    entityData.set(ITEM_STACK, defaultDisplayItemStack)
                }
            }
        } catch (e: Exception) {
            GAME_API.logError("Failed to read LuckyProjectile", e)
            entityData.set(ITEM_STACK, defaultDisplayItemStack)
        }
    }

    override fun addAdditionalSaveData(tag: ValueOutput) {
        super.addAdditionalSaveData(tag)
        val parentNbt = CompoundTag()

        data.writeToTag(parentNbt)
        tag.store("LuckyData", CompoundTag.CODEC, parentNbt)
        tag.store("DisplayItem", MCItemStack.CODEC, entityData.get(ITEM_STACK))
    }

    override fun getItem(): MCItemStack = entityData.get(ITEM_STACK)
}

@OnlyInClient
class LuckyProjectileRenderer(ctx: EntityRendererProvider.Context) : ThrownItemRenderer<LuckyProjectile>(ctx)
