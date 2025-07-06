package net.elytraautopilot.utils;

import net.elytraautopilot.ElytraAutoPilot;
import net.elytraautopilot.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

import static net.elytraautopilot.ElytraAutoPilot.*;

public class Hud {
    private static List<Double> velocityList = new ArrayList<>();
    private static List<Double> velocityListHorizontal = new ArrayList<>();
    private static int _tick;
    private static int _index = -1;
    public static Text[] hudString;

    public static void tick() {
        _tick++;
    }

    public static void drawHud(PlayerEntity player) {
        // If GUI is disabled, clear everything
        if (!ModConfig.INSTANCE.showGui) {
            hudString = new Text[0];
            return;
        }

        double altitude = player.getPos().y;
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
            World world = player.getWorld();
            int l = world.getBottomY();
            Vec3d clientPos = player.getPos();
            for (double i = clientPos.getY(); i > l; i--) {
                BlockPos blockPos = BlockPos.ofFloored(clientPos.getX(), i, clientPos.getZ());
                if (world.getBlockState(blockPos).isSolidBlock(world, blockPos)) {
                    groundheight = clientPos.getY() - i;
                    break;
                } else {
                    groundheight = clientPos.getY();
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
            List<Text> lines = new ArrayList<>();

            if (ModConfig.INSTANCE.showEnabled) {
                lines.add(
                        Text.translatable("text.elytraautopilot.hud.toggleAutoFlight")
                                .append(
                                        Text.translatable(autoFlight
                                                ? "text.elytraautopilot.hud.true"
                                                : "text.elytraautopilot.hud.false"
                                        ).formatted(autoFlight ? Formatting.GREEN : Formatting.RED)
                                )
                );
            }

            if (ModConfig.INSTANCE.showAltitude) {
                lines.add(
                        Text.translatable("text.elytraautopilot.hud.altitude", String.format("%.2f", altitude))
                                .formatted(Formatting.AQUA)
                );
            }

            if (ModConfig.INSTANCE.showHeight) {
                String heightStr = groundheight < 0 ? "???" : String.valueOf(Math.round(groundheight));
                lines.add(
                        Text.translatable("text.elytraautopilot.hud.heightFromGround", heightStr)
                                .formatted(Formatting.AQUA)
                );
            }

            if (ModConfig.INSTANCE.showHeightReq) {
                boolean ready = groundheight > ModConfig.INSTANCE.minHeight;
                String req = ready
                        ? "Ready"
                        : String.valueOf(Math.round(ModConfig.INSTANCE.minHeight - groundheight));
                lines.add(
                        Text.translatable("text.elytraautopilot.hud.neededHeight")
                                .formatted(Formatting.AQUA)
                                .append(Text.literal(req).formatted(ready ? Formatting.GREEN : Formatting.RED))
                );
            }

            if (ModConfig.INSTANCE.showSpeed) {
                lines.add(
                        Text.translatable("text.elytraautopilot.hud.speed", String.format("%.2f", currentVelocity * 20))
                                .formatted(Formatting.YELLOW)
                );
            }

            if (ModConfig.INSTANCE.showAvgSpeed) {
                if (avgVelocity == 0) {
                    lines.add(
                            Text.translatable("text.elytraautopilot.hud.calculating")
                                    .formatted(Formatting.WHITE)
                    );
                } else {
                    lines.add(
                            Text.translatable("text.elytraautopilot.hud.avgSpeed", String.format("%.2f", avgVelocity * 20))
                                    .formatted(Formatting.YELLOW)
                    );
                }
            }

            if (ModConfig.INSTANCE.showHorizontalSpeed && avgVelocity != 0) {
                lines.add(
                        Text.translatable("text.elytraautopilot.hud.avgHSpeed", String.format("%.2f", avgHorizontalVelocity * 20))
                                .formatted(Formatting.YELLOW)
                );
            }

            // Fly-to / landing lines (you can also gate these with new config flags if you like)
            if (isflytoActive && !forceLand) {
                if (ModConfig.INSTANCE.showFlyTo) {
                    lines.add(
                            Text.translatable("text.elytraautopilot.flyto", argXpos, argZpos)
                                    .formatted(Formatting.LIGHT_PURPLE)
                    );
                }
                if (distance != 0 && ModConfig.INSTANCE.showEta) {
                    lines.add(
                            Text.translatable("text.elytraautopilot.hud.eta",
                                    String.valueOf(Math.round(distance / (avgHorizontalVelocity * 20)))
                            ).formatted(Formatting.LIGHT_PURPLE)
                    );
                }
                if (ModConfig.INSTANCE.showAutoLand) {
                    lines.add(
                            Text.translatable("text.elytraautopilot.hud.autoLand")
                                    .formatted(Formatting.LIGHT_PURPLE)
                                    .append(
                                            Text.translatable(ModConfig.INSTANCE.autoLanding
                                                    ? "text.elytraautopilot.hud.enabled"
                                                    : "text.elytraautopilot.hud.disabled"
                                            ).formatted(ModConfig.INSTANCE.autoLanding ? Formatting.GREEN : Formatting.RED)
                                    )
                    );
                }
                if (isLanding && ModConfig.INSTANCE.showLandingStatus) {
                    lines.add(
                            Text.translatable("text.elytraautopilot.hud.landing")
                                    .formatted(Formatting.LIGHT_PURPLE)
                    );
                }
            }

            if (forceLand && ModConfig.INSTANCE.showLandingStatus) {
                // Override or add a “forced landing” indicator
                lines.add(
                        Text.translatable("text.elytraautopilot.hud.landing")
                                .formatted(Formatting.LIGHT_PURPLE)
                );
            }

            // Finally, turn the list into your array
            hudString = lines.toArray(new Text[0]);
        }
    }


    public static void clearHud() {
        velocityList.clear();
        velocityListHorizontal.clear();
    }
}
