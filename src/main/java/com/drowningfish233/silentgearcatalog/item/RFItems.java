package com.drowningfish233.silentgearcatalog.item;

import com.drowningfish233.silentgearcatalog.SilentGearCatalog;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.silentchaos512.gear.item.GearItemSet;
import net.silentchaos512.gear.item.MainPartItem;
import net.silentchaos512.gear.item.blueprint.BlueprintType;
import net.silentchaos512.gear.item.blueprint.GearBlueprintItem;

import java.util.function.Supplier;

public class RFItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(SilentGearCatalog.MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SilentGearCatalog.MODID);

    public static final DeferredHolder<Item, CatalogItem> CATALOG = ITEMS.register("catalog",
            () -> new CatalogItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)
            ));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB_REVERIE_FOUNDRY =
            CREATIVE_MODE_TABS.register("reverie_foundry", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + SilentGearCatalog.MODID))
                    .icon(() -> CATALOG.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(CATALOG.get());
                    })
                    .build()
            );
}