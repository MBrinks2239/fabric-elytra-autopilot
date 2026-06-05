package net.elytraautopilot.utils;

import net.elytraautopilot.ElytraAutoPilot;
import net.elytraautopilot.config.ModConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

import static net.elytraautopilot.ElytraAutoPilot.*;

public class Hud {
    private static List<Double> velocityList = new ArrayList<>();
    private static List<Double> velocityListHorizontal = new ArrayList<>();
    private static int _tick;
    private static int _index = -1;
    public static Component[] hudString;

    // Cycle-based averaging fields
    private static boolean previousIsDescending = false;
    private static List<Double> cycleVelocitySamples = new ArrayList<>();
    private static List<Double> cycleHorizontalVelocitySamples = new ArrayList<>();
    private static double displayCycleAvgVelocity = 0.0;
    private static double displayCycleAvgHorizontalVelocity = 0.0;
    private static double cycleETA = 0.0;
    private static boolean cycleInitialized = false;

    public static void tick() {
        _tick++;
    }

    public static void drawHud(Player player) {
        // If GUI is disabled, clear everything
        if (!ModConfig.INSTANCE.showGui) {
            hudString = new Component[0];
            return;
        }

        double altitude = player.position().y;
        int gticks = Math.max(1, ModConfig.INSTANCE.groundCheckTicks);

        if (_tick >= gticks) {
            _index++;
            if (_index >= 1200 / gticks) _index = 0;
            if (velocityList.size() < 1200 / gticks) {
                velocityList.add(currentVelocity);
                velocityListHorizontal.add(currentVelocityHorizontal);
            } else {
                velocityList.set(_index, currentVelocity);
                velocityListHorizontal.set(_index, currentVelocityHorizontal);
            }

            // --- Cycle-based averaging ---
            if (autoFlight && !isLanding && !forceLand) {
                cycleVelocitySamples.add(currentVelocity);
                cycleHorizontalVelocitySamples.add(currentVelocityHorizontal);

                if (previousIsDescending && !isDescending && !cycleVelocitySamples.isEmpty()) {
                    // Dive→Climb transition: one complete cycle finished
                    displayCycleAvgVelocity = cycleVelocitySamples.stream()
                            .mapToDouble(d -> d).average().orElse(0.0);
                    displayCycleAvgHorizontalVelocity = cycleHorizontalVelocitySamples.stream()
                            .mapToDouble(d -> d).average().orElse(0.0);
                    cycleInitialized = true;
                    if (displayCycleAvgHorizontalVelocity != 0) {
                        cycleETA = distance / (displayCycleAvgHorizontalVelocity * 20);
                    } else {
                        cycleETA = 0.0;
                    }
                    cycleVelocitySamples.clear();
                    cycleHorizontalVelocitySamples.clear();
                }
                previousIsDescending = isDescending;
            }

            Level world = player.level();
            int l = world.getMinY();
            Vec3 clientPos = player.position();
            for (double i = clientPos.y(); i > l; i--) {
                BlockPos blockPos = BlockPos.containing(clientPos.x(), i, clientPos.z());
                if (world.getBlockState(blockPos).isRedstoneConductor(world, blockPos)) {
                    groundheight = clientPos.y() - i;
                    break;
                } else {
                    groundheight = clientPos.y();
                }
            }
            _tick = 0;

            // Compute averages — use cycle-based average when enabled and initialized
            double avgVelocity = 0, avgHorizontalVelocity = 0;
            if (ModConfig.INSTANCE.useCycleAvgSpeed && cycleInitialized) {
                avgVelocity = displayCycleAvgVelocity;
                avgHorizontalVelocity = displayCycleAvgHorizontalVelocity;
            } else if (velocityList.size() >= 10) {
                avgVelocity = velocityList.stream().mapToDouble(d -> d).average().orElse(0.0);
                avgHorizontalVelocity = velocityListHorizontal.stream().mapToDouble(d -> d).average().orElse(0.0);
            }

            // Build up a dynamic list of lines
            List<Component> lines = new ArrayList<>();

            if (ModConfig.INSTANCE.showEnabled) {
                lines.add(
                        Component.translatable("text.elytraautopilot.hud.toggleAutoFlight")
                                .append(
                                        Component.translatable(autoFlight
                                                ? "text.elytraautopilot.hud.true"
                                                : "text.elytraautopilot.hud.false"
                                        ).withStyle(autoFlight ? ChatFormatting.GREEN : ChatFormatting.RED)
                                )
                );
            }

            if (ModConfig.INSTANCE.showAltitude) {
                lines.add(
                        Component.translatable("text.elytraautopilot.hud.altitude", String.format("%.2f", altitude))
                                .withStyle(ChatFormatting.AQUA)
                );
            }

            if (ModConfig.INSTANCE.showHeight) {
                String heightStr = groundheight < 0 ? "???" : String.valueOf(Math.round(groundheight));
                lines.add(
                        Component.translatable("text.elytraautopilot.hud.heightFromGround", heightStr)
                                .withStyle(ChatFormatting.AQUA)
                );
            }

            if (ModConfig.INSTANCE.showHeightReq) {
                boolean ready = groundheight > ModConfig.INSTANCE.minHeight;
                String req = ready
                        ? "Ready"
                        : String.valueOf(Math.round(ModConfig.INSTANCE.minHeight - groundheight));
                lines.add(
                        Component.translatable("text.elytraautopilot.hud.neededHeight")
                                .withStyle(ChatFormatting.AQUA)
                                .append(Component.literal(req).withStyle(ready ? ChatFormatting.GREEN : ChatFormatting.RED))
                );
            }

            if (ModConfig.INSTANCE.showSpeed) {
                lines.add(
                        Component.translatable("text.elytraautopilot.hud.speed", String.format("%.2f", currentVelocity * 20))
                                .withStyle(ChatFormatting.YELLOW)
                );
            }

            if (ModConfig.INSTANCE.showAvgSpeed) {
                if (avgVelocity == 0) {
                    lines.add(
                            Component.translatable("text.elytraautopilot.hud.calculating")
                                    .withStyle(ChatFormatting.WHITE)
                    );
                } else {
                    lines.add(
                            Component.translatable("text.elytraautopilot.hud.avgSpeed", String.format("%.2f", avgVelocity * 20))
                                    .withStyle(ChatFormatting.YELLOW)
                    );
                }
            }

            if (ModConfig.INSTANCE.showHorizontalSpeed && avgVelocity != 0) {
                lines.add(
                        Component.translatable("text.elytraautopilot.hud.avgHSpeed", String.format("%.2f", avgHorizontalVelocity * 20))
                                .withStyle(ChatFormatting.YELLOW)
                );
            }

            // Fly-to / landing lines (you can also gate these with new config flags if you like)
            if (isflytoActive && !forceLand) {
                if (ModConfig.INSTANCE.showFlyTo) {
                    lines.add(
                            Component.translatable("text.elytraautopilot.flyto", argXpos, argZpos)
                                    .withStyle(ChatFormatting.LIGHT_PURPLE)
                    );
                }
                if (distance != 0 && ModConfig.INSTANCE.showEta && avgHorizontalVelocity != 0) {
                    long displayETA;
                    if (ModConfig.INSTANCE.smoothEta && cycleInitialized && cycleETA > 0) {
                        // Dead reckoning: count down linearly, independent of speed fluctuations
                        cycleETA = Math.max(0, cycleETA - gticks / 20.0);
                        displayETA = Math.round(cycleETA);
                    } else {
                        displayETA = Math.round(distance / (avgHorizontalVelocity * 20));
                    }
                    lines.add(
                            Component.translatable("text.elytraautopilot.hud.eta",
                                    String.valueOf(displayETA)
                            ).withStyle(ChatFormatting.LIGHT_PURPLE)
                    );
                }
                if (ModConfig.INSTANCE.showAutoLand) {
                    lines.add(
                            Component.translatable("text.elytraautopilot.hud.autoLand")
                                    .withStyle(ChatFormatting.LIGHT_PURPLE)
                                    .append(
                                            Component.translatable(ModConfig.INSTANCE.autoLanding
                                                    ? "text.elytraautopilot.hud.enabled"
                                                    : "text.elytraautopilot.hud.disabled"
                                            ).withStyle(ModConfig.INSTANCE.autoLanding ? ChatFormatting.GREEN : ChatFormatting.RED)
                                    )
                    );
                }
                if (isLanding && ModConfig.INSTANCE.showLandingStatus) {
                    lines.add(
                            Component.translatable("text.elytraautopilot.hud.landing")
                                    .withStyle(ChatFormatting.LIGHT_PURPLE)
                    );
                }
            }

            if (forceLand && ModConfig.INSTANCE.showLandingStatus) {
                // Override or add a “forced landing” indicator
                lines.add(
                        Component.translatable("text.elytraautopilot.hud.landing")
                                .withStyle(ChatFormatting.LIGHT_PURPLE)
                );
            }

            // Finally, turn the list into your array
            hudString = lines.toArray(new Component[0]);
        }
    }


    public static void clearHud() {
        velocityList.clear();
        velocityListHorizontal.clear();
        cycleVelocitySamples.clear();
        cycleHorizontalVelocitySamples.clear();
        displayCycleAvgVelocity = 0.0;
        displayCycleAvgHorizontalVelocity = 0.0;
        cycleETA = 0.0;
        cycleInitialized = false;
        previousIsDescending = false;
    }
}
