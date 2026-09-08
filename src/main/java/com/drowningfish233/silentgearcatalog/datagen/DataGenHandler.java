package com.drowningfish233.silentgearcatalog.datagen;

import com.drowningfish233.silentgearcatalog.SilentGearCatalog;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class DataGenHandler {

    public static void handleGatherDataEvent(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        DatapackBuiltinEntriesProvider datapackBuiltinEntriesProvider = createDatapackBuiltinEntriesProvider(packOutput, lookupProvider);
        generator.addProvider(event.includeServer(), datapackBuiltinEntriesProvider);

        lookupProvider = datapackBuiltinEntriesProvider.getRegistryProvider();

        generator.addProvider(
                event.includeServer(),
                new SalvageRecipeProvider(packOutput, lookupProvider)
        );

        // 语言文件提供器 - 英文
        generator.addProvider(
                event.includeClient(),
                new ModLanguageProvider(packOutput, "en_us")
        );

        // 语言文件提供器 - 中文
        generator.addProvider(
                event.includeClient(),
                new ModLanguageProvider(packOutput, "zh_cn")
        );
    }

    private static DatapackBuiltinEntriesProvider createDatapackBuiltinEntriesProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        RegistrySetBuilder registrySetBuilder = new RegistrySetBuilder();
        return new DatapackBuiltinEntriesProvider(packOutput, lookupProvider, registrySetBuilder, Set.of(SilentGearCatalog.MODID));
    }
}