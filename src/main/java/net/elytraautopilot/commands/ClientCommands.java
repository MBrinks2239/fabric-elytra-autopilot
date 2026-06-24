package net.elytraautopilot.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.elytraautopilot.ElytraAutoPilot;
import net.elytraautopilot.config.ModConfig;
import net.elytraautopilot.exceptions.InvalidLocationException;
import net.elytraautopilot.types.FlyToLocation;
import net.elytraautopilot.utils.CommandSuggestionProvider;
import net.elytraautopilot.xaeromapintegration.XaeromapWaypointReader;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class ClientCommands {
    public static boolean bufferSave = false;
    private static final boolean isXaeroMinimapInstalled = FabricLoader.getInstance().isModLoaded("xaerominimap");

    public static void register(Minecraft minecraftClient) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher
                .register(literal("flyto").then(argument("Name", StringArgumentType.string())
                        .suggests(new CommandSuggestionProvider()).executes(context -> { // With name
                            if (minecraftClient.player == null)
                                return 0;

                            String locationName = StringArgumentType.getString(context, "Name");
                            locationName = locationName.replace(";", ":");

                            int result = TryFlyTo(ModConfig.INSTANCE.flyLocations.toArray(new String[0]), locationName,
                                    minecraftClient, context);
                            if (result == 1) {
                                return 1;
                            }

                            if (isXaeroMinimapInstalled) {
                                String[] xaeroLocations = XaeromapWaypointReader.GetXearomapWaypoints();
                                if (xaeroLocations != null) {
                                    int xaeroResult = TryFlyTo(xaeroLocations, locationName, minecraftClient, context);
                                    if (xaeroResult == 1) {
                                        return 1;
                                    }
                                }
                            }

                            minecraftClient.player.sendOverlayMessage(Component
                                    .translatable("text.elytraautopilot.flylocationFail.notFound", locationName)
                                    .withStyle(ChatFormatting.RED));
                            return 0;
                        }))
                        .then(argument("X", IntegerArgumentType.integer(-2000000000, 2000000000))
                                .then(argument("Z", IntegerArgumentType.integer(-2000000000, 2000000000))
                                        .executes(context -> {
                                            if (minecraftClient.player == null)
                                                return 0;
                                            if (minecraftClient.player.isFallFlying()) { // If the player is flying
                                                if (ElytraAutoPilot.groundheight > ModConfig.INSTANCE.minHeight) { // If
                                                                                                                    // above
                                                                                                                    // required
                                                                                                                    // height
                                                    ElytraAutoPilot.autoFlight = true;
                                                    ElytraAutoPilot.argXpos = IntegerArgumentType.getInteger(context,
                                                            "X");
                                                    ElytraAutoPilot.argZpos = IntegerArgumentType.getInteger(context,
                                                            "Z");
                                                    ElytraAutoPilot.isflytoActive = true;
                                                    ElytraAutoPilot.pitchMod = 3f;
                                                    context.getSource().sendFeedback(Component
                                                            .translatable("text.elytraautopilot.flyto",
                                                                    ElytraAutoPilot.argXpos, ElytraAutoPilot.argZpos)
                                                            .withStyle(ChatFormatting.GREEN));
                                                } else {
                                                    minecraftClient.player.sendOverlayMessage(Component
                                                            .translatable("text.elytraautopilot.autoFlightFail.tooLow")
                                                            .withStyle(ChatFormatting.RED));
                                                }
                                            } else {
                                                minecraftClient.player.sendOverlayMessage(Component
                                                        .translatable("text.elytraautopilot.flytoFail.flyingRequired")
                                                        .withStyle(ChatFormatting.RED));
                                            }
                                            return 1;
                                        })))));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher
                .register(literal("takeoff").then(argument("Name", StringArgumentType.string())
                        .suggests(new CommandSuggestionProvider()).executes(context -> { // With name
                            if (minecraftClient.player == null)
                                return 0;

                            String locationName = StringArgumentType.getString(context, "Name");
                            locationName = locationName.replace(";", ":");

                            int successLocalTakeOff = TryTakeoff(ModConfig.INSTANCE.flyLocations.toArray(new String[0]),
                                    locationName);
                            if (successLocalTakeOff == 1)
                                return 1;

                            if (isXaeroMinimapInstalled) {
                                String[] xaeroLocations = XaeromapWaypointReader.GetXearomapWaypoints();
                                if (xaeroLocations != null) {
                                    int successXaeroTakeOff = TryTakeoff(xaeroLocations, locationName);
                                    if (successXaeroTakeOff == 1)
                                        return 1;
                                }
                            }
                            minecraftClient.player.sendOverlayMessage(Component
                                    .translatable("text.elytraautopilot.flylocationFail.notFound", locationName)
                                    .withStyle(ChatFormatting.RED));
                            return 0;
                        }))
                        .then(argument("X", IntegerArgumentType.integer(-2000000000, 2000000000))
                                .then(argument("Z", IntegerArgumentType.integer(-2000000000, 2000000000))
                                        .executes(context -> { // With coordinates
                                            ElytraAutoPilot.argXpos = IntegerArgumentType.getInteger(context, "X");
                                            ElytraAutoPilot.argZpos = IntegerArgumentType.getInteger(context, "Z");
                                            ElytraAutoPilot.isChained = true; // Chains fly-to command
                                            ElytraAutoPilot.takeoff();
                                            return 1;
                                        })))
                        .executes(context -> { // Without coordinates
                            ElytraAutoPilot.takeoff();
                            return 1;
                        })));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                literal("flylocation").then(literal("remove").then(argument("Name", StringArgumentType.string())
                        .suggests(new CommandSuggestionProvider()).executes(context -> {
                            if (minecraftClient.player == null)
                                return 0;
                            String locationName = StringArgumentType.getString(context, "Name");
                            locationName = locationName.replace(";", ":");

                            int index = 0;
                            for (String s : ModConfig.INSTANCE.flyLocations) {
                                try {
                                    FlyToLocation location = FlyToLocation.ConvertStringToLocation(s);
                                    if (location.Name.equals(locationName)) {
                                        ModConfig.INSTANCE.flyLocations.remove(index);
                                        minecraftClient.player.sendOverlayMessage(Component
                                                .translatable("text.elytraautopilot.flylocation.removed", locationName)
                                                .withStyle(ChatFormatting.GREEN));
                                        return 1;
                                    }
                                } catch (InvalidLocationException e) {
                                    ElytraAutoPilot.LOGGER.error("Error in reading Fly Location list entry!");
                                    break;
                                }
                            }
                            minecraftClient.player.sendOverlayMessage(Component
                                    .translatable("text.elytraautopilot.flylocationFail.notFound", locationName)
                                    .withStyle(ChatFormatting.RED));
                            return 0;
                        })))
                        .then(literal("set").then(argument("Name", StringArgumentType.string())
                                .then(argument("X", IntegerArgumentType.integer(-2000000000, 2000000000))
                                        .then(argument("Z", IntegerArgumentType.integer(-2000000000, 2000000000))
                                                .executes(context -> {
                                                    if (minecraftClient.player == null)
                                                        return 0;
                                                    String locationName = StringArgumentType.getString(context, "Name");
                                                    locationName = locationName.replace(";", ":");
                                                    int locationX = IntegerArgumentType.getInteger(context, "X");
                                                    int locationZ = IntegerArgumentType.getInteger(context, "Z");

                                                    FlyToLocation newLocation = new FlyToLocation();
                                                    newLocation.Name = locationName;
                                                    newLocation.X = locationX;
                                                    newLocation.Z = locationZ;

                                                    for (String s : ModConfig.INSTANCE.flyLocations) {
                                                        try {
                                                            FlyToLocation location = FlyToLocation
                                                                    .ConvertStringToLocation(s);
                                                            if (location.Name.equals(locationName)) {
                                                                minecraftClient.player.sendOverlayMessage(Component
                                                                        .translatable(
                                                                                "text.elytraautopilot.flylocationFail.nameExists")
                                                                        .withStyle(ChatFormatting.RED));
                                                                return 0;
                                                            }
                                                        } catch (InvalidLocationException ignored) {
                                                            ElytraAutoPilot.LOGGER
                                                                    .error("Error in reading Fly Location list entry!");
                                                            break;
                                                        }
                                                    }
                                                    ModConfig.INSTANCE.flyLocations
                                                            .add(newLocation.ConvertLocationToString());
                                                    minecraftClient.player.sendOverlayMessage(Component
                                                            .translatable("text.elytraautopilot.flylocation.saved",
                                                                    locationName, locationX, locationZ)
                                                            .withStyle(ChatFormatting.GREEN));
                                                    bufferSave = true;
                                                    return 1;
                                                })))))));

        ClientCommandRegistrationCallback.EVENT
                .register((dispatcher, registryAccess) -> dispatcher.register(literal("land").executes(context -> {
                    if (ElytraAutoPilot.autoFlight) {
                        LocalPlayer player = minecraftClient.player;
                        if (player == null)
                            return 0;
                        player.sendOverlayMessage(
                                Component.translatable("text.elytraautopilot.landing").withStyle(ChatFormatting.BLUE));
                        SoundEvent soundEvent = SoundEvent
                                .createVariableRangeEvent(Identifier.parse(ModConfig.INSTANCE.playSoundOnLanding));
                        player.playSound(soundEvent, 1.3f, 1f);
                        minecraftClient.options.keyUse.setDown(false);
                        ElytraAutoPilot.forceLand = true;
                        return 1;
                    }
                    return 0;
                })));

    }

    private static int TryTakeoff(String[] locations, String locationName) {
        for (String s : locations) {
            try {
                FlyToLocation location = FlyToLocation.ConvertStringToLocation(s);
                if (location.Name.equals(locationName)) {
                    ElytraAutoPilot.argXpos = location.X;
                    ElytraAutoPilot.argZpos = location.Z;
                    ElytraAutoPilot.isChained = true;
                    ElytraAutoPilot.takeoff();
                    return 1;
                }
            } catch (InvalidLocationException ignored) {
                ElytraAutoPilot.LOGGER.error("Error in reading Fly Location list entry!");
                break;
            }
        }
        return 0;
    }

    private static int TryFlyTo(String[] locations, String locationName, Minecraft minecraftClient,
            CommandContext<FabricClientCommandSource> context) {
        for (String s : locations) {
            try {
                FlyToLocation location = FlyToLocation.ConvertStringToLocation(s);

                if (!location.Name.equals(locationName))
                    continue;

                assert minecraftClient.player != null;
                if (!minecraftClient.player.isFallFlying()) {
                    minecraftClient.player
                            .sendOverlayMessage(Component.translatable("text.elytraautopilot.flytoFail.flyingRequired")
                                    .withStyle(ChatFormatting.RED));
                    return 1;
                }

                if (ElytraAutoPilot.groundheight <= ModConfig.INSTANCE.minHeight) {
                    minecraftClient.player.sendOverlayMessage(Component
                            .translatable("text.elytraautopilot.autoFlightFail.tooLow").withStyle(ChatFormatting.RED));
                    return 1;
                }

                ElytraAutoPilot.autoFlight = true;
                ElytraAutoPilot.argXpos = location.X;
                ElytraAutoPilot.argZpos = location.Z;
                ElytraAutoPilot.isflytoActive = true;
                ElytraAutoPilot.pitchMod = 3f;

                context.getSource().sendFeedback(Component
                        .translatable("text.elytraautopilot.flyto", ElytraAutoPilot.argXpos, ElytraAutoPilot.argZpos)
                        .withStyle(ChatFormatting.GREEN));

                return 1;
            } catch (InvalidLocationException e) {
                ElytraAutoPilot.LOGGER.error(e.getMessage());
                break;
            }
        }

        return 0;
    }
}
