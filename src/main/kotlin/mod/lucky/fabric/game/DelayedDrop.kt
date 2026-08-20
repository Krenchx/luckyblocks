package mod.lucky.fabric.game

import mod.lucky.common.GAME_API
import mod.lucky.fabric.*
import mod.lucky.java.game.*
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput

class DelayedDrop(
    type: EntityType<DelayedDrop> = FabricLuckyRegistry.delayedDrop,
    world: MCWorld,
    private var data: DelayedDropData = DelayedDropData.createDefault(world),
) : MCEntity(type, world) {
    override fun defineSynchedData(builder: SynchedEntityData.Builder) {}

    override fun tick() {
        super.tick()
        data.tick(level())
        if (data.ticksRemaining <= 0) remove(RemovalReason.DISCARDED)
    }

    override fun hurtServer(p0: ServerLevel, p1: DamageSource, p2: Float): Boolean {
        return false
    }

    override fun readAdditionalSaveData(tag: ValueInput) {
        try {
            val dataTag = tag.read("LuckyData", CompoundTag.CODEC).orElseThrow()
            data = DelayedDropData.readFromTag(dataTag, level())
        } catch (e: Exception) {
            GAME_API.logError("Failed to read DelayedDrop", e)
            data = DelayedDropData.createDefault(level())
        }
    }
    override fun addAdditionalSaveData(tag: ValueOutput) {
        val parentTag = CompoundTag()
        data.writeToTag(parentTag)
        tag.store("LuckyData", CompoundTag.CODEC, parentTag)
    }
}

@OnlyInClient
open class DelayedDropRenderState : EntityRenderState() {}

@OnlyInClient
class DelayedDropRenderer(ctx: EntityRendererProvider.Context) : EntityRenderer<DelayedDrop, DelayedDropRenderState>(ctx) {
    override fun createRenderState(): DelayedDropRenderState {
        return DelayedDropRenderState()
    }
}
