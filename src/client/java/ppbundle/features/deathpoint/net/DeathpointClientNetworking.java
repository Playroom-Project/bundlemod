package ppbundle.features.deathpoint.net;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import ppbundle.features.deathpoint.DeathpointClientState;

public final class DeathpointClientNetworking {

    private DeathpointClientNetworking() {}

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(DeathpointNetworking.CLEAR_DEATHPOINT, (client, handler, buf, responseSender) -> {
            ResourceLocation dim = buf.readResourceLocation();
            BlockPos pos = buf.readBlockPos();
            client.execute(() -> DeathpointClientState.removeDeathpoint(dim, pos));
        });
    }
}