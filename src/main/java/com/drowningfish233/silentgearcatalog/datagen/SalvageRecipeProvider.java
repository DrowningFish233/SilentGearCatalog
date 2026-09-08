package com.drowningfish233.silentgearcatalog.datagen;

import com.drowningfish233.silentgearcatalog.SilentGearCatalog;
import com.drowningfish233.silentgearcatalog.item.RFItems;
import com.drowningfish233.silentgearcatalog.SilentGearCatalog;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.silentchaos512.gear.crafting.recipe.salvage.GearSalvagingRecipe;
import net.silentchaos512.gear.item.GearItemSet;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;

public class SalvageRecipeProvider extends RecipeProvider {

    public SalvageRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        try {
            for (Field field : RFItems.class.getDeclaredFields()) {
                if (GearItemSet.class.isAssignableFrom(field.getType())) {
                    GearItemSet<?> gearSet = (GearItemSet<?>) field.get(null);
                    Item gearItem = gearSet.gearItem();

                    registerSalvageRecipe(output, gearItem);
                }
            }
        } catch (IllegalAccessException e) {
        }
    }

    private void registerSalvageRecipe(RecipeOutput output, Item gearItem) {
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(
                SilentGearCatalog.MODID,
                "salvage/" + gearItem.getDescriptionId().replace("item." + SilentGearCatalog.MODID + ".", "")
        );

        GearSalvagingRecipe recipe = new GearSalvagingRecipe(
                Ingredient.of(gearItem)
        );

        output.accept(recipeId, recipe, null);
    }
}