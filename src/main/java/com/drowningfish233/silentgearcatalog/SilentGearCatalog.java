package com.drowningfish233.silentgearcatalog;

import com.drowningfish233.silentgearcatalog.client.gui.catalog.CatalogDataBuilder;
import com.drowningfish233.silentgearcatalog.datagen.DataGenHandler;
import com.drowningfish233.silentgearcatalog.item.RFItems;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mod(SilentGearCatalog.MODID)
public class SilentGearCatalog {
    public static final String MODID = "silentgearcatalog";

    public SilentGearCatalog(IEventBus modEventBus, ModContainer modContainer) {

        RFItems.ITEMS.register(modEventBus);
        RFItems.CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(this::onGatherData);
    }


    private void onGatherData(GatherDataEvent event) {
        DataGenHandler.handleGatherDataEvent(event);
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new PreparableReloadListener() {
            @Override
            public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier barrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor syncExecutor) {
                CatalogDataBuilder.invalidateCache();
                return barrier.wait(null);
            }

            @Override
            public String getName() {
                return "silentgearcatalog_catalog_cache";
            }
        });
    }

}