package net.elytraautopilot.types;

import net.elytraautopilot.commands.ClientCommands;
import net.elytraautopilot.config.ModConfig;
import net.elytraautopilot.exceptions.InvalidLocationException;

import static java.lang.Integer.parseInt;

public class FlyToLocation {
    public String Name;
    public int X;
    public int Y;

    public static FlyToLocation ConvertStringToLocation(String configLocation) throws InvalidLocationException {
        String[] tokens = configLocation.split(";");
        if(tokens.length != 3) {
            ModConfig.INSTANCE.flyLocations.remove(configLocation);
            ClientCommands.bufferSave = true;
            throw new InvalidLocationException("Error in reading Fly Location list entry!");
        }
        FlyToLocation location = new FlyToLocation();
        try {
            location.X = parseInt(tokens[1]);
            location.Y = parseInt(tokens[2]);
        } catch (NumberFormatException ignored) {
            ModConfig.INSTANCE.flyLocations.remove(configLocation);
            ClientCommands.bufferSave = true;
            throw new InvalidLocationException("Error in reading Fly Location list entry!");
        }
        location.Name = tokens[0];
        return location;
    }
}
