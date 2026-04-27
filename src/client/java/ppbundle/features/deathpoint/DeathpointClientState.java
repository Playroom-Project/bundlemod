package ppbundle.features.deathpoint;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import ppbundle.compat.ftbchunks.FTBChunksCompatClient;

public final class DeathpointClientState {

    private DeathpointClientState() {}

    public static void removeDeathpoint(ResourceLocation dimension, BlockPos pos) {
        if (dimension == null || pos == null) return;
        if (!FabricLoader.getInstance().isModLoaded("ftbchunks")) return;

        FTBChunksCompatClient.tryClearDeathpoint(dimension, pos);
    }
}