package mod.lucky.fabric.game

import mod.lucky.fabric.*
import mod.lucky.java.game.getLuckModifierCraftingResult
import mod.lucky.java.game.matchesLuckModifierCraftingRecipe
import net.minecraft.core.HolderLookup
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.Level

class LuckModifierCraftingRecipe : CustomRecipe() {
    companion object {
        private var lastRegistryAccess: HolderLookup.Provider? = null
    }

    override fun matches(input: CraftingInput, world: Level): Boolean {
        lastRegistryAccess = world.registryAccess()
        val stacks = (0 until input.size()).map {
            toItemStack(input.getItem(it), world.registryAccess(), skipComponents = true)
        }
        return matchesLuckModifierCraftingRecipe(stacks)
    }

    override fun assemble(input: CraftingInput): MCItemStack {
        val access = lastRegistryAccess ?: return MCItemStack.EMPTY
        val stacks = (0 until input.size()).map { toItemStack(input.getItem(it), access) }
        return getLuckModifierCraftingResult(stacks)?.let { toMCItemStack(it, access) } ?: MCItemStack.EMPTY
    }

    override fun getSerializer(): RecipeSerializer<LuckModifierCraftingRecipe> =
        FabricLuckyRegistry.luckModifierCraftingRecipe
}
