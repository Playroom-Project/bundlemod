package ppbundle.compat.ftbchunks;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.FTBChunksClientAPI;
import dev.ftb.mods.ftbchunks.api.client.waypoint.Waypoint;
import dev.ftb.mods.ftbchunks.api.client.waypoint.WaypointManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;

public final class FTBChunksDeathpointVisibilityFix {

    private static int tickCounter = 0;

    private FTBChunksDeathpointVisibilityFix() {}

    public static void onClientTick(Minecraft mc) {
        if (mc == null || mc.level == null) return;
        if (!FabricLoader.getInstance().isModLoaded("ftbchunks")) return;

        tickCounter++;
        if (tickCounter < 20) return;
        tickCounter = 0;

        tryForceShowAllDeathWaypoints(mc);
    }

    private static void tryForceShowAllDeathWaypoints(Minecraft mc) {
        FTBChunksClientAPI api = FTBChunksAPI.clientApi();
        Optional<WaypointManager> opt = api.getWaypointManager(mc.level.dimension());
        if (opt.isEmpty()) return;

        WaypointManager manager = opt.get();
        Collection<Waypoint> all = manager.getAllWaypoints();
        if (all == null || all.isEmpty()) return;

        boolean changed = false;

        for (Waypoint wp : all) {
            if (wp == null) continue;
            if (!isDeathWaypoint(wp)) continue;

            changed |= forceVisible(wp);
        }

        if (changed) {
            api.requestMinimapIconRefresh();
        }
    }

    private static boolean isDeathWaypoint(Waypoint wp) {

        if (callBool(wp, "isDeathpoint")) return true;
        if (callBool(wp, "isDeath")) return true;

        String name = callString(wp, "getName");
        if (name == null) name = callString(wp, "name");
        if (name != null && name.toLowerCase().contains("death")) return true;

        return false;
    }

    private static boolean forceVisible(Waypoint wp) {
        boolean changed = false;

        changed |= callVoid(wp, "setVisible", boolean.class, true);
        changed |= callVoid(wp, "setHidden", boolean.class, false);

        changed |= callVoid(wp, "setShowInWorld", boolean.class, true);
        changed |= callVoid(wp, "setShowInWorldMap", boolean.class, true);
        changed |= callVoid(wp, "setShowOnMap", boolean.class, true);

        changed |= callVoid(wp, "setEnabled", boolean.class, true);
        changed |= callVoid(wp, "setActive", boolean.class, true);

        return changed;
    }

    private static boolean callBool(Object target, String methodName) {
        try {
            Method m = target.getClass().getMethod(methodName);
            Object v = m.invoke(target);
            return v instanceof Boolean b && b;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String callString(Object target, String methodName) {
        try {
            Method m = target.getClass().getMethod(methodName);
            Object v = m.invoke(target);
            return v != null ? String.valueOf(v) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean callVoid(Object target, String methodName, Class<?> argType, Object arg) {
        try {
            Method m = target.getClass().getMethod(methodName, argType);
            m.invoke(target, arg);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}