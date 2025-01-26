package net.elytraautopilot.xearomapintegration;

import net.fabricmc.loader.api.FabricLoader;
import xaero.common.XaeroMinimapSession;
import xaero.common.core.XaeroMinimapCore;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointSet;
import xaero.common.minimap.waypoints.WaypointsManager;
import xaero.minimap.XaeroMinimap;

import java.util.ArrayList;
import java.util.List;

public class XearomapWaypointReader {
    public static String[] GetXearomapWaypoints() {
        if(!FabricLoader.getInstance().isModLoaded("xaerominimap")) {
            return null;
        }
        ArrayList<String> locations = new ArrayList<>();
        XaeroMinimapSession minimapSession = XaeroMinimapSession.getCurrentSession();

        if (minimapSession == null) return null;

        WaypointsManager waypointsManager = minimapSession.getWaypointsManager();
        WaypointSet waypointSet = waypointsManager.getWaypoints();
        if (waypointSet == null) return null;

        List<Waypoint> waypoints = waypointSet.getList();
        for(Waypoint waypoint : waypoints ) {
            String name = waypoint.getName().replace(";", ":");
            locations.add(name+";"+waypoint.getX()+";"+waypoint.getZ());
        }
        return locations.toArray(new String[0]);
    }
}
