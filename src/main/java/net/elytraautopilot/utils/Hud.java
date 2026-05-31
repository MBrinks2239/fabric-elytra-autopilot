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

            // Compute averages if we have enough samples
            double avgVelocity = 0, avgHorizontalVelocity = 0;
            if (velocityList.size() >= 10) {
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
                if (distance != 0 && ModConfig.INSTANCE.showEta) {
                    lines.add(
                            Component.translatable("text.elytraautopilot.hud.eta",
                                    String.valueOf(Math.round(distance / (avgHorizontalVelocity * 20)))
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
    }
}
