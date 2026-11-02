package net.elytraautopilot;

import net.elytraautopilot.commands.ClientCommands;
import net.elytraautopilot.config.ModConfig;
import net.elytraautopilot.input.FlightInputController;
import net.elytraautopilot.strategy.FlightPhase;
import net.elytraautopilot.strategy.FlightStrategy;
import net.elytraautopilot.utils.ElytraManager;
import net.elytraautopilot.utils.FreeCameraState;
import net.elytraautopilot.utils.Hud;
import net.elytraautopilot.utils.HudRenderer;
import net.elytraautopilot.utils.KeyBindings;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.elytraautopilot.utils.ElytraManager.*;

public class ElytraAutoPilot implements ClientModInitializer {
    public static final String MODID = "elytraautopilot";
    public static final Logger LOGGER = LoggerFactory.getLogger("ElytraAutoPilot");
    private static boolean configPressed = false;
    private static boolean landPressed = false;
    private static boolean takeoffPressed = false;
    public static Minecraft minecraftClient;
    public static boolean calculateHud;
    public static boolean autoFlight;
    private static final int TAKEOFF_COOLDOWN_TICKS = 5;
    private static int takeoffCooldown = 0;
    private static boolean onTakeoff;
    public static double pitchMod = 1f;

    public static Vec3 previousPosition;
    public static double currentVelocity;
    public static double currentVelocityHorizontal;

    public static boolean isDescending;
    public static boolean pullUp;
    public static boolean pullDown;

    private static double velHigh = 0f;
    private static double velLow = 0f;

    public static int argXpos;
    public static int argZpos;
    public static boolean isChained = false;
    public static boolean isflytoActive = false;
    public static boolean forceLand = false;
    public static boolean isLanding = false;
    public static float GLIDE_ANGLE = 0.0f;
    public static boolean doGlide = false;
    public static double distance = 0f;
    public static double groundheight;

    // Strategy mode fields
    private static FlightStrategy climbStrategy;
    private static FlightStrategy cruiseStrategy;
    public static FlightPhase strategyPhase = FlightPhase.CLIMB;
    public static int climbTick = 0;
    public static int cruiseTick = 0;
    public static boolean strategyActive = false;

    @Override
    public void onInitializeClient() {
        minecraftClient = Minecraft.getInstance();

        // Load precomputed pitch strategies
        climbStrategy = FlightStrategy.loadResource("climb.csv", 254);
        cruiseStrategy = FlightStrategy.loadResource("cruise.csv", 357);
        if (climbStrategy == null || cruiseStrategy == null) {
            LOGGER.warn("Strategy mode unavailable — falling back to classic mode. "
                    + "Check that strategy CSV resources are present in the mod JAR.");
        }

        KeyBindings.init();
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MODID, "hud"), (context, tickCounter) -> {
            ElytraAutoPilot.this.onScreenTick();
            HudRenderer.drawHud(context, tickCounter);
        });

        ClientTickEvents.END_CLIENT_TICK.register(e -> this.onClientTick());

        ClientCommands.register(minecraftClient);
    }

    public static String getModId() {
        return MODID;
    }

    public static void takeoff() {
        LocalPlayer player = minecraftClient.player;
        if (!onTakeoff) {
            if (player != null) {
                if (ModConfig.INSTANCE.elytraAutoSwap) {
                    int elytraSlot = getElytraIndex(player);
                    if (elytraSlot == -100) {
                        player.sendOverlayMessage(
                                Component.translatable("text." + MODID + ".takeoffFail.noElytraInInventory")
                                        .withStyle(ChatFormatting.RED));
                        return;
                    }
                    equipElytra(player);
                } else {
                    ItemStack itemStack = ElytraManager.getChestplateSlot(player);

                    if (itemStack.getItem() != Items.ELYTRA) {
                        player.sendOverlayMessage(
                                Component.translatable("text." + MODID + ".takeoffFail.noElytraEquipped")
                                        .withStyle(ChatFormatting.RED));
                        return;
                    }
                    int elytraDamage = itemStack.getMaxDamage() - itemStack.getDamageValue();
                    if (elytraDamage == 1) {
                        player.sendOverlayMessage(Component.translatable("text." + MODID + ".takeoffFail.elytraBroken")
                                .withStyle(ChatFormatting.RED));
                        return;
                    }
                }
                Item itemMain = player.getMainHandItem().getItem();
                Item itemOff = player.getOffhandItem().getItem();
                var chestplateSlot = ElytraManager.getChestplateSlot(player);
                Item itemChest = chestplateSlot.getItem();
                int elytraDamage = chestplateSlot.getMaxDamage() - chestplateSlot.getDamageValue();
                if (itemChest != Items.ELYTRA) {
                    player.sendOverlayMessage(
                            Component.translatable("text.elytraautopilot.takeoffFail.noElytraEquipped")
                                    .withStyle(ChatFormatting.RED));
                    return;
                }
                if (elytraDamage == 1) {
                    player.sendOverlayMessage(Component.translatable("text.elytraautopilot.takeoffFail.elytraBroken")
                            .withStyle(ChatFormatting.RED));
                    return;
                }
                if (itemMain != Items.FIREWORK_ROCKET && itemOff != Items.FIREWORK_ROCKET) {
                    player.sendOverlayMessage(
                            Component.translatable("text.elytraautopilot.takeoffFail.fireworkRequired")
                                    .withStyle(ChatFormatting.RED));
                    return;
                }

                Level world = player.level();
                Vec3 clientPos = player.position();
                int l = world.getMaxY();
                int n = 2;
                double c = clientPos.y();
                for (double i = c; i < l; i++) {
                    BlockPos blockPos = BlockPos.containing(clientPos.x(), clientPos.y() + n, clientPos.z());
                    if (!world.getBlockState(blockPos).isAir()) {
                        player.sendOverlayMessage(
                                Component.translatable("text.elytraautopilot.takeoffFail.clearSkyNeeded")
                                        .withStyle(ChatFormatting.RED));
                        return;
                    }
                    n++;
                }
                takeoffCooldown = TAKEOFF_COOLDOWN_TICKS;
                FlightInputController.requestTakeoffJump();
            }
            return;
        }
        if (player != null) {
            if (groundheight > ModConfig.INSTANCE.minHeight) {
                completeTakeoff(player);
                return;
            }
            boolean boost = currentVelocity < 0.75f && player.getXRot() <= -89.5f;
            FlightInputController.requestTakeoff(boost);
        }
    }

    private static void completeTakeoff(LocalPlayer player) {
        onTakeoff = false;
        FlightInputController.reset();
        autoFlight = true;
        pitchMod = 3f;
        if (ModConfig.INSTANCE.strategyMode && climbStrategy != null && cruiseStrategy != null) {
            strategyPhase = player.position().y >= ModConfig.INSTANCE.cruiseAltitudeMax
                    ? FlightPhase.CRUISE
                    : FlightPhase.CLIMB;
            climbTick = 0;
            cruiseTick = 0;
        }
        FreeCameraState.init();
        if (isChained) {
            isflytoActive = true;
            isChained = false;
            player.sendOverlayMessage(Component.translatable("text.elytraautopilot.flyto", argXpos, argZpos)
                    .withStyle(ChatFormatting.GREEN));
        }
    }

    private static void abortTakeoff(LocalPlayer player) {
        onTakeoff = false;
        takeoffCooldown = 0;
        FlightInputController.reset();
        player.sendOverlayMessage(
                Component.translatable("text.elytraautopilot.takeoffAbort.noFirework").withStyle(ChatFormatting.RED));
        doGlide = true;
    }

    private void onScreenTick() // Once every screen frame
    {
        // Stops logic when paused.
        if (minecraftClient.isPaused()) {
            doGlide = false;
            if (minecraftClient.isLocalServer())
                return;
        }

        // Player is null when it isn't currently in a world. Optimization spot here.
        Player player = minecraftClient.player;
        if (player == null)
            return;

        // Fps adaptation (not perfect but works nicely most of the time)
        float fps_delta = minecraftClient.getDeltaTracker().getGameTimeDeltaTicks();
        float fps_result = 20 / fps_delta;
        double speedMod = 60 / fps_result; // Adapt to base 60 FPS

        // Calculate hard coded flight modes based on pitch.
        float pitch = player.getXRot();
        // if (doGlide) {
        // if (pitch < GLIDE_ANGLE) {
        // player.setPitch((float) (pitch +
        // ModConfig.INSTANCE.pullDownSpeed*speedMod*3));
        // pitch = player.getPitch();
        // if (pitch >= GLIDE_ANGLE) {
        // player.setPitch(GLIDE_ANGLE);
        // doGlide = false;
        // }
        // }
        // else if (pitch > GLIDE_ANGLE){
        // player.setPitch((float) (pitch -
        // ModConfig.INSTANCE.pullDownSpeed*speedMod)*3);
        // pitch = player.getPitch();
        // if (pitch <= GLIDE_ANGLE) {
        // player.setPitch(GLIDE_ANGLE);
        // doGlide = false;
        // }
        // }
        // }
        if (onTakeoff) {
            if (pitch > -90f) {
                player.setXRot((float) (pitch - ModConfig.INSTANCE.takeOffPull * speedMod));
                pitch = player.getXRot();
            }
            if (pitch <= -90f)
                player.setXRot(-90f); // Very stiff and unnatural movement
        }
        if (autoFlight) {
            // Flyto behavior
            if (isflytoActive || forceLand) {
                if (isLanding || forceLand) {
                    if (!forceLand && !ModConfig.INSTANCE.autoLanding) {
                        isflytoActive = false;
                        isLanding = false;
                        return;
                    }
                    isDescending = true;
                    if (ModConfig.INSTANCE.riskyLanding && groundheight > 60) {
                        riskyLanding(player, speedMod);
                    } else {
                        smoothLanding(player, speedMod);
                    }
                } else {
                    Vec3 playerPosition = player.position();
                    double f = (double) argXpos - playerPosition.x;
                    double d = (double) argZpos - playerPosition.z;
                    float targetYaw = Mth.wrapDegrees((float) (Mth.atan2(d, f) * 57.2957763671875D) - 90.0F);
                    float yaw = Mth.wrapDegrees(player.getYRot());
                    if (Math.abs(yaw - targetYaw) < ModConfig.INSTANCE.turningSpeed * 2 * speedMod)
                        player.setYRot(targetYaw);
                    else {
                        if (yaw < targetYaw)
                            player.setYRot((float) (yaw + ModConfig.INSTANCE.turningSpeed * speedMod));
                        if (yaw > targetYaw)
                            player.setYRot((float) (yaw - ModConfig.INSTANCE.turningSpeed * speedMod));
                    }
                    distance = Math.sqrt(f * f + d * d);
                    if (distance < 20) {
                        minecraftClient.player.sendOverlayMessage(
                                Component.translatable("text.elytraautopilot.landing").withStyle(ChatFormatting.BLUE));
                        SoundEvent soundEvent = SoundEvent
                                .createVariableRangeEvent(Identifier.parse(ModConfig.INSTANCE.playSoundOnLanding));
                        player.playSound(soundEvent, 1.3f, 1f);
                        isLanding = true;
                    }
                }
            }
            // Flight pitch behavior (classic mode only — strategy mode controls
            // pitch in onClientTick at 20 TPS)
            if (pullUp && !(isLanding || forceLand) && !strategyActive) {
                player.setXRot((float) (pitch - ModConfig.INSTANCE.pullUpSpeed * speedMod));
                pitch = player.getXRot();
                if (pitch <= ModConfig.INSTANCE.pullUpAngle) {
                    player.setXRot((float) ModConfig.INSTANCE.pullUpAngle);
                }
            }
            if (pullDown && !(isLanding || forceLand) && !strategyActive) {
                player.setXRot((float) (pitch + ModConfig.INSTANCE.pullDownSpeed * pitchMod * speedMod));
                pitch = player.getXRot();
                if (pitch >= ModConfig.INSTANCE.pullDownAngle) {
                    player.setXRot((float) ModConfig.INSTANCE.pullDownAngle);
                }
            }
        } else {
            velHigh = 0f;
            velLow = 0f;
            isLanding = false;
            forceLand = false;
            isflytoActive = false;
            pullUp = false;
            pitchMod = 1f;
            pullDown = false;
            strategyActive = false;
            FreeCameraState.reset();
        }
    }

    private void onClientTick() // 20 times a second, before first screen tick
    {
        if (!(minecraftClient.isPaused() && minecraftClient.isLocalServer()))
            Hud.tick();
        double velMod;

        if (ClientCommands.bufferSave) {
            ModConfig.INSTANCE.saveConfig(ModConfig.CONFIG_FILE.toFile());
            ClientCommands.bufferSave = false;
        }

        LocalPlayer player = minecraftClient.player;

        if (player == null) {
            autoFlight = false;
            onTakeoff = false;
            takeoffCooldown = 0;
            FlightInputController.reset();
            return;
        }

        if (player.isFallFlying())
            calculateHud = true;
        else {
            calculateHud = false;
            autoFlight = false;
            groundheight = -1f;
            climbTick = 0;
            cruiseTick = 0;
            strategyActive = false;
            if (!onTakeoff && takeoffCooldown == 0) {
                FlightInputController.reset();
            }
        }

        double altitude;
        if (autoFlight) {
            var durability = getElytraDurability(player);
            if (ModConfig.INSTANCE.emergencyLand && durability < ModConfig.INSTANCE.elytraReplaceDurability) {
                if (ModConfig.INSTANCE.elytraAutoSwap) {
                    if (canRestockElytra(player)) {
                        forceLand = !tryRestockElytra(player);
                    } else {
                        forceLand = true;
                    }
                } else {
                    forceLand = true;
                }
            }

            altitude = player.position().y;

            if (player.isInWater() || player.isInLava()) {
                isflytoActive = false;
                isLanding = false;
                autoFlight = false;
                FlightInputController.reset();
                return;
            }

            if (ModConfig.INSTANCE.strategyMode && climbStrategy != null && cruiseStrategy != null && !onTakeoff
                    && !isLanding && !forceLand) {
                // Strategy mode: altitude-based phase switching + precomputed pitch waveform
                strategyActive = true;

                // Hysteresis phase switching based on absolute altitude
                if (strategyPhase == FlightPhase.CLIMB && altitude >= ModConfig.INSTANCE.cruiseAltitudeMax) {
                    strategyPhase = FlightPhase.CRUISE;
                    cruiseTick = 0;
                } else if (strategyPhase == FlightPhase.CRUISE && altitude <= ModConfig.INSTANCE.cruiseAltitudeMin) {
                    strategyPhase = FlightPhase.CLIMB;
                    climbTick = 0;
                }

                // Apply precomputed pitch for the current phase
                FlightStrategy strategy;
                int tick;
                if (strategyPhase == FlightPhase.CLIMB) {
                    strategy = climbStrategy;
                    tick = climbTick;
                } else {
                    strategy = cruiseStrategy;
                    tick = cruiseTick;
                }
                double angle = strategy.angleAt(tick);
                player.setXRot((float) Math.max(-90.0, Math.min(90.0, -angle)));

                // Advance the waveform tick index
                if (strategyPhase == FlightPhase.CLIMB) {
                    climbTick = strategy.nextTick(climbTick);
                } else {
                    cruiseTick = strategy.nextTick(cruiseTick);
                }
            } else {
                // Classic mode: velocity-thresholded hysteresis controller
                strategyActive = false;
                if (isDescending) {
                    pullUp = false;
                    pullDown = true;
                    if (altitude > ModConfig.INSTANCE.maxHeight) {
                        velHigh = 0.3f;
                    } else if (altitude > ModConfig.INSTANCE.maxHeight - 10) {
                        velLow = 0.28475f;
                    }
                    velMod = Math.max(velHigh, velLow);
                    if (currentVelocity >= ModConfig.INSTANCE.pullDownMaxVelocity + velMod) {
                        isDescending = false;
                        pullDown = false;
                        pullUp = true;
                        pitchMod = 1f;
                    }
                } else {
                    velHigh = 0f;
                    velLow = 0f;
                    pullUp = true;
                    pullDown = false;
                    if (currentVelocity <= ModConfig.INSTANCE.pullUpMinVelocity
                            || altitude > ModConfig.INSTANCE.maxHeight - 10) {
                        isDescending = true;
                        pullDown = true;
                        pullUp = false;
                    }
                }
            }
        }
        if (!takeoffPressed && KeyBindings.takeoffBinding.isDown()) {
            if (onTakeoff) {
                onTakeoff = false;
                takeoffCooldown = 0;
                FlightInputController.reset();
                doGlide = true;
            } else {
                takeoff();
            }
        }

        if (!landPressed && KeyBindings.landBinding.isDown() && autoFlight) {
            player.sendOverlayMessage(
                    Component.translatable("text.elytraautopilot.landing").withStyle(ChatFormatting.BLUE));
            SoundEvent soundEvent = SoundEvent
                    .createVariableRangeEvent(Identifier.parse(ModConfig.INSTANCE.playSoundOnLanding));
            player.playSound(soundEvent, 1.3f, 1f);
            FlightInputController.reset();
            forceLand = true;
        }

        if (!configPressed && KeyBindings.configBinding.isDown()) {
            if (player.isFallFlying()) {
                if (!autoFlight && groundheight < ModConfig.INSTANCE.minHeight) {
                    player.sendOverlayMessage(Component.translatable("text.elytraautopilot.autoFlightFail.tooLow")
                            .withStyle(ChatFormatting.RED));
                    doGlide = true;
                } else {
                    // If the player is flying an elytra, we start the auto flight
                    autoFlight = !autoFlight;
                    FlightInputController.reset();
                    if (autoFlight) {
                        isDescending = true;
                        pitchMod = 3f;
                        FreeCameraState.init();
                        // Initialize strategy phase based on current altitude
                        if (ModConfig.INSTANCE.strategyMode && climbStrategy != null && cruiseStrategy != null) {
                            strategyPhase = player.position().y >= ModConfig.INSTANCE.cruiseAltitudeMax
                                    ? FlightPhase.CRUISE
                                    : FlightPhase.CLIMB;
                            climbTick = 0;
                            cruiseTick = 0;
                        }
                    }
                }
            } else {
                // Otherwise, we open the settings if cloth is loaded
                Screen configScreen = ModConfig.createConfigScreen(minecraftClient.gui.screen());
                minecraftClient.gui.setScreen(configScreen);
            }
        }
        configPressed = KeyBindings.configBinding.isDown();
        landPressed = KeyBindings.landBinding.isDown();
        takeoffPressed = KeyBindings.takeoffBinding.isDown();

        if (takeoffCooldown > 0) {
            if (--takeoffCooldown == 0)
                onTakeoff = true;
        }

        if (onTakeoff) {
            takeoff();
        }

        tickFlightInput(player);

        if (calculateHud) {
            computeVelocity();
            Hud.drawHud(player);
        } else {
            previousPosition = null;
            Hud.clearHud();
        }
    }

    private static void tickFlightInput(LocalPlayer player) {
        if (takeoffCooldown > 0) {
            FlightInputController.requestTakeoffJump();
        }
        if (autoFlight && !onTakeoff && !isLanding && !forceLand && !strategyActive && ModConfig.INSTANCE.poweredFlight
                && currentVelocity < 1.25f) {
            FlightInputController.requestPoweredFlight();
        }
        FlightInputController.tick(minecraftClient);
        if (onTakeoff && FlightInputController.consumeTakeoffFireworkFailure()) {
            abortTakeoff(player);
        }
    }

    private static boolean tryRestockElytra(LocalPlayer player) {
        if (ModConfig.INSTANCE.elytraHotswap) {
            return equipElytra(player);
        }
        return false;
    }

    private static boolean canRestockElytra(LocalPlayer player) {
        var result = getElytraIndex(player);
        return result != -100;
    }

    private void computeVelocity() {
        Vec3 newPosition;
        Player player = minecraftClient.player;
        if (player != null && !(minecraftClient.isPaused() && minecraftClient.isLocalServer())) {
            newPosition = player.position();
            if (previousPosition == null)
                previousPosition = newPosition;

            Vec3 difference = new Vec3(newPosition.x - previousPosition.x, newPosition.y - previousPosition.y,
                    newPosition.z - previousPosition.z);
            Vec3 difference_horizontal = new Vec3(newPosition.x - previousPosition.x, 0,
                    newPosition.z - previousPosition.z);
            previousPosition = newPosition;

            currentVelocity = difference.length();
            currentVelocityHorizontal = difference_horizontal.length();
        }
    }

    private void smoothLanding(Player player, double speedMod) {
        float yaw = Mth.wrapDegrees(player.getYRot());
        float pitch = Mth.wrapDegrees(player.getXRot());
        float fallPitchMax = 50f;
        float fallPitchMin = 30f;
        float fallPitch;
        if (groundheight > 50) {
            fallPitch = fallPitchMax;
        } else if (groundheight < 20) {
            fallPitch = fallPitchMin;
        } else {
            fallPitch = (float) ((groundheight - 20) / 30) * 20 + fallPitchMin;
        }
        pitchMod = 3f;
        player.setYRot((float) (yaw + ModConfig.INSTANCE.autoLandSpeed * speedMod));
        player.setXRot((float) (pitch + ModConfig.INSTANCE.pullDownSpeed * pitchMod * speedMod));
        pitch = player.getXRot();
        if (pitch >= fallPitch) {
            player.setXRot(fallPitch);
        }
    }

    private void riskyLanding(Player player, double speedMod) {
        float pitch = player.getXRot();
        player.setXRot((float) (pitch + ModConfig.INSTANCE.takeOffPull * speedMod));
        pitch = player.getXRot();
        if (pitch > 90f)
            player.setXRot(90f);
    }
}
