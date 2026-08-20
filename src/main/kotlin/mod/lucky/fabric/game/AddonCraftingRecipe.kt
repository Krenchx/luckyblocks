package mod.lucky.fabric.game

import mod.lucky.fabric.*
import mod.lucky.java.JavaLuckyRegistry
import mod.lucky.java.loader.CraftingRecipe
import mod.lucky.java.loader.ShapedCraftingRecipe
import mod.lucky.java.loader.ShapelessCraftingRecipe
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.Level

fun registerAddonCraftingRecipes() {
    AddonCraftingRecipe.craftingRecipes = JavaLuckyRegistry.allAddonResources.flatMap { it.blockCraftingRecipes }
}

class AddonCraftingRecipe : CustomRecipe() {
    companion object {
        var craftingRecipes: List<CraftingRecipe> = emptyList()
        private var lastRegistryAccess: HolderLookup.Provider? = null
    }

    override fun matches(input: CraftingInput, world: Level): Boolean {
        lastRegistryAccess = world.registryAccess()
        return craftingRecipes.any { matchesRecipe(it, input) }
    }

    override fun assemble(input: CraftingInput): MCItemStack {
        val access = lastRegistryAccess ?: return MCItemStack.EMPTY
        val recipe = craftingRecipes.firstOrNull { matchesRecipe(it, input) } ?: return MCItemStack.EMPTY
        return toMCItemStack(
            when (recipe) {
                is ShapedCraftingRecipe -> recipe.resultStack
                is ShapelessCraftingRecipe -> recipe.resultStack
                else -> return MCItemStack.EMPTY
            },
            access
        )
    }

    override fun group(): String = "lucky"

    override fun getSerializer(): RecipeSerializer<AddonCraftingRecipe> = FabricLuckyRegistry.addonCraftingRecipe

    private fun matchesRecipe(recipe: CraftingRecipe, input: CraftingInput): Boolean = when (recipe) {
        is ShapelessCraftingRecipe -> matchesShapeless(recipe, input)
        is ShapedCraftingRecipe -> matchesShaped(recipe, input, mirrored = false) || matchesShaped(recipe, input, mirrored = true)
        else -> false
    }

    private fun itemId(input: CraftingInput, index: Int): String? {
        val stack = input.getItem(index)
        return if (stack.isEmpty) null else BuiltInRegistries.ITEM.getKey(stack.item).toString()
    }

    private fun matchesShapeless(recipe: ShapelessCraftingRecipe, input: CraftingInput): Boolean {
        val actual = (0 until input.size()).mapNotNull { itemId(input, it) }.groupingBy { it }.eachCount()
        val expected = recipe.ingredientIds.groupingBy { it }.eachCount()
        return actual == expected
    }

    private fun matchesShaped(recipe: ShapedCraftingRecipe, input: CraftingInput, mirrored: Boolean): Boolean {
        if (input.width() != recipe.width || input.height() != recipe.height) return false
        for (y in 0 until recipe.height) {
            for (x in 0 until recipe.width) {
                val recipeX = if (mirrored) recipe.width - x - 1 else x
                val expected = recipe.ingredientIds[recipeX + y * recipe.width]
                val actual = itemId(input, x + y * input.width())
                if (expected != actual) return false
            }
        }
        return true
    }
}
