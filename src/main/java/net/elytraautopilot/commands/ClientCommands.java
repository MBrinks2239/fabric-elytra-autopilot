package net.elytraautopilot.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.elytraautopilot.ElytraAutoPilot;
import net.elytraautopilot.config.ModConfig;
import net.elytraautopilot.exceptions.InvalidLocationException;
import net.elytraautopilot.types.FlyToLocation;
import net.elytraautopilot.utils.CommandSuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import static java.lang.Integer.parseInt;

public class ClientCommands {
    public static boolean bufferSave = false;

    public static void register(MinecraftClient minecraftClient) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("flyto")
                        .then(ClientCommandManager.argument("Name", StringArgumentType.string())
                                .suggests(new CommandSuggestionProvider())
                                .executes(context -> { // With name
                                    if (minecraftClient.player == null)
                                        return 0;

                                    String locationName = StringArgumentType.getString(context, "Name");
                                    locationName = locationName.replace(";", ":");

                                    for (String s : ModConfig.INSTANCE.flyLocations) {
                                        try {
                                            FlyToLocation location = FlyToLocation.ConvertStringToLocation(s);

                                            if (!location.Name.equals(locationName))
                                                continue;

                                            if (!minecraftClient.player.isGliding()) {
                                                minecraftClient.player.sendMessage(Text
                                                        .translatable("text.elytraautopilot.flytoFail.flyingRequired")
                                                        .formatted(Formatting.RED), true);
                                                return 1;
                                            }

                                            if (ElytraAutoPilot.groundheight <= ModConfig.INSTANCE.minHeight) {
                                                minecraftClient.player.sendMessage(Text
                                                        .translatable("text.elytraautopilot.autoFlightFail.tooLow")
                                                        .formatted(Formatting.RED), true);
                                                return 1;
                                            }

                                            ElytraAutoPilot.autoFlight = true;
                                            ElytraAutoPilot.argXpos = location.X;
                                            ElytraAutoPilot.argZpos = location.Z;
                                            ElytraAutoPilot.isflytoActive = true;
                                            ElytraAutoPilot.pitchMod = 3f;

                                            context.getSource().sendFeedback(Text
                                                    .translatable("text.elytraautopilot.flyto",
                                                            ElytraAutoPilot.argXpos, ElytraAutoPilot.argZpos)
                                                    .formatted(Formatting.GREEN));

                                            return 1;
                                        } catch (InvalidLocationException e) {
                                            ElytraAutoPilot.LOGGER.error(e.getMessage());
                                            break;
                                        }
                                    }

                                    minecraftClient.player.sendMessage(Text
                                            .translatable("text.elytraautopilot.flylocationFail.notFound", locationName)
                                            .formatted(Formatting.RED), true);
                                    return 0;
                                }))
                        .then(ClientCommandManager.argument("X", IntegerArgumentType.integer(-2000000000, 2000000000))
                                .then(ClientCommandManager
                                        .argument("Z", IntegerArgumentType.integer(-2000000000, 2000000000))
                                        .executes(context -> {
                                            if (minecraftClient.player == null)
                                                return 0;
                                            if (minecraftClient.player.isGliding()) { // If the player is flying
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
                                                    context.getSource().sendFeedback(Text
                                                            .translatable("text.elytraautopilot.flyto",
                                                                    ElytraAutoPilot.argXpos, ElytraAutoPilot.argZpos)
                                                            .formatted(Formatting.GREEN));
                                                } else {
                                                    minecraftClient.player.sendMessage(Text
                                                            .translatable("text.elytraautopilot.autoFlightFail.tooLow")
                                                            .formatted(Formatting.RED), true);
                                                }
                                            } else {
                                                minecraftClient.player.sendMessage(Text
                                                        .translatable("text.elytraautopilot.flytoFail.flyingRequired")
                                                        .formatted(Formatting.RED), true);
                                            }
                                            return 1;
                                        })))));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("takeoff")
                        .then(ClientCommandManager.argument("Name", StringArgumentType.string())
                                .suggests(new CommandSuggestionProvider())
                                .executes(context -> { // With name
                                    if (minecraftClient.player == null)
                                        return 0;

                                    String locationName = StringArgumentType.getString(context, "Name");
                                    locationName = locationName.replace(";", ":");

                                    for (String s : ModConfig.INSTANCE.flyLocations) {
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
                                    minecraftClient.player.sendMessage(Text
                                            .translatable("text.elytraautopilot.flylocationFail.notFound", locationName)
                                            .formatted(Formatting.RED), true);
                                    return 0;
                                }))
                        .then(ClientCommandManager.argument("X", IntegerArgumentType.integer(-2000000000, 2000000000))
                                .then(ClientCommandManager
                                        .argument("Z", IntegerArgumentType.integer(-2000000000, 2000000000))
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
                ClientCommandManager.literal("flylocation")
                        .then(ClientCommandManager.literal("remove")
                                .then(ClientCommandManager.argument("Name", StringArgumentType.string())
                                        .suggests(new CommandSuggestionProvider())
                                        .executes(context -> {
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
                                                        minecraftClient.player.sendMessage(Text
                                                                .translatable("text.elytraautopilot.flylocation.removed",
                                                                        locationName)
                                                                .formatted(Formatting.GREEN), true);
                                                        return 1;
                                                    }
                                                } catch (InvalidLocationException e) {
                                                    ElytraAutoPilot.LOGGER.error("Error in reading Fly Location list entry!");
                                                    break;
                                                }
                                            }
                                            minecraftClient.player.sendMessage(
                                                    Text.translatable("text.elytraautopilot.flylocationFail.notFound",
                                                            locationName).formatted(Formatting.RED),
                                                    true);
                                            return 0;
                                        })))
                        .then(ClientCommandManager.literal("set")
                                .then(ClientCommandManager.argument("Name", StringArgumentType.string())
                                        .then(ClientCommandManager
                                                .argument("X", IntegerArgumentType.integer(-2000000000, 2000000000))
                                                .then(ClientCommandManager
                                                        .argument("Z",
                                                                IntegerArgumentType.integer(-2000000000, 2000000000))
                                                        .executes(context -> {
                                                            if (minecraftClient.player == null)
                                                                return 0;
                                                            String locationName = StringArgumentType.getString(context,
                                                                    "Name");
                                                            locationName = locationName.replace(";", ":");
                                                            int locationX = IntegerArgumentType.getInteger(context,
                                                                    "X");
                                                            int locationZ = IntegerArgumentType.getInteger(context,
                                                                    "Z");

                                                            FlyToLocation newLocation = new FlyToLocation();
                                                            newLocation.Name = locationName;
                                                            newLocation.X = locationX;
                                                            newLocation.Z = locationZ;

                                                            for (String s : ModConfig.INSTANCE.flyLocations) {
                                                                try {
                                                                    FlyToLocation location = FlyToLocation.ConvertStringToLocation(s);
                                                                    if (location.Name.equals(locationName)) {
                                                                        minecraftClient.player.sendMessage(Text
                                                                                .translatable(
                                                                                        "text.elytraautopilot.flylocationFail.nameExists")
                                                                                .formatted(Formatting.RED), true);
                                                                        return 0;
                                                                    }
                                                                } catch (InvalidLocationException ignored) {
                                                                    ElytraAutoPilot.LOGGER.error("Error in reading Fly Location list entry!");
                                                                    break;
                                                                }
                                                            }
                                                            ModConfig.INSTANCE.flyLocations.add(newLocation.ConvertLocationToString());
                                                            minecraftClient.player.sendMessage(Text
                                                                    .translatable(
                                                                            "text.elytraautopilot.flylocation.saved",
                                                                            locationName, locationX, locationZ)
                                                                    .formatted(Formatting.GREEN), true);
                                                            bufferSave = true;
                                                            return 1;
                                                        })))))));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("land")
                        .executes(context -> {
                            if (ElytraAutoPilot.autoFlight) {
                                ClientPlayerEntity player = minecraftClient.player;
                                if (player == null)
                                    return 0;
                                player.sendMessage(
                                        Text.translatable("text.elytraautopilot.landing").formatted(Formatting.BLUE),
                                        true);
                                SoundEvent soundEvent = SoundEvent
                                        .of(Identifier.of(ModConfig.INSTANCE.playSoundOnLanding));
                                player.playSound(soundEvent, 1.3f, 1f);
                                minecraftClient.options.useKey.setPressed(false);
                                ElytraAutoPilot.forceLand = true;
                                return 1;
                            }
                            return 0;
                        })));

    }
}
