package net.elytraautopilot.xaeromapintegration;

import net.fabricmc.loader.api.FabricLoader;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.world.MinimapWorldManager;

import java.util.ArrayList;

public class XaeromapWaypointReader {
    public static String[] GetXearomapWaypoints() {
        if (!FabricLoader.getInstance().isModLoaded("xaerominimap")) {
            return null;
        }

        ArrayList<String> locations = new ArrayList<>();
        ArrayList<Waypoint> deathWaypoints = new ArrayList<>();

        MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
        MinimapWorldManager manager = session.getWorldManager();

        var waypointSet = manager.getCurrentWorld().getCurrentWaypointSet();
        Iterable<Waypoint> waypoints = waypointSet.getWaypoints();

        for (Waypoint waypoint : waypoints) {
            String wpName = waypoint.getName();
            if ("gui.xaero_deathpoint".equals(wpName) || "gui.xaero_deathpoint_old".equals(wpName)) {
                deathWaypoints.add(waypoint);
            } else {
                String name = wpName.replace(";", ":");
                locations.add(name + ";" + waypoint.getX() + ";" + waypoint.getZ());
            }
        }

        // Sort death waypoints by createdAt descending (newest first)
        deathWaypoints.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

        for (int i = 0; i < deathWaypoints.size(); i++) {
            Waypoint wp = deathWaypoints.get(i);
            String label = (i == 0) ? "Latest death" : "Old death " + i;
            locations.add(label + ";" + wp.getX() + ";" + wp.getZ());
        }

        return locations.toArray(new String[0]);
    }
}
