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

        double altitude = player.getPos().y;
        double avgVelocity = 0f;
        double avgHorizontalVelocity = 0f;
        int gticks = Math.max(1, ModConfig.INSTANCE.groundCheckTicks);

        if (_tick >= gticks) {
            _index++;
            if (_index >= 1200/gticks) _index = 0;
            if (velocityList.size()< 1200/gticks) {
                velocityList.add(currentVelocity);
                velocityListHorizontal.add(currentVelocityHorizontal);
            }
            else {
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
                }
                else {
                    groundheight = clientPos.getY();
                }
            }
            _tick = 0;
        }
        if (velocityList.size() >= 10) {
            avgVelocity = velocityList.stream().mapToDouble(val -> val).average().orElse(0.0);
            avgHorizontalVelocity = velocityListHorizontal.stream().mapToDouble(val -> val).average().orElse(0.0);
        }
        if (hudString == null) hudString = new Text[10];
        if (!ModConfig.INSTANCE.showGui) {
            hudString[0] = Text.of("");
            hudString[1] = Text.of("");
            hudString[2] = Text.of("");
            hudString[3] = Text.of("");
            hudString[4] = Text.of("");
            hudString[5] = Text.of("");
            hudString[6] = Text.of("");
            hudString[7] = Text.of("");
            hudString[8] = Text.of("");
            hudString[9] = Text.of("");
            return;
        }
        hudString[0] = Text.translatable("text.elytraautopilot.hud.toggleAutoFlight")
                .append(Text.translatable(autoFlight ? "text.elytraautopilot.hud.true" : "text.elytraautopilot.hud.false")
                        .formatted(autoFlight ? Formatting.GREEN : Formatting.RED));

        hudString[1] = Text.translatable("text.elytraautopilot.hud.altitude", String.format("%.2f", altitude))
                .formatted(Formatting.AQUA);
        hudString[2] = Text.translatable("text.elytraautopilot.hud.heightFromGround", (groundheight == -1f ? "???" : String.valueOf(Math.round(groundheight))))
                .formatted(Formatting.AQUA);
        hudString[3] = Text.translatable("text.elytraautopilot.hud.neededHeight")
                .formatted(Formatting.AQUA)
                .append(Text.literal(groundheight > ModConfig.INSTANCE.minHeight ? "Ready" : String.valueOf(Math.round(ModConfig.INSTANCE.minHeight-groundheight)))
                        .formatted(groundheight > ModConfig.INSTANCE.minHeight ? Formatting.GREEN : Formatting.RED));
        hudString[4] = Text.translatable("text.elytraautopilot.hud.speed", String.format("%.2f", currentVelocity * 20))
                .formatted(Formatting.YELLOW);
        if (avgVelocity == 0f) {
            hudString[5] = Text.translatable("text.elytraautopilot.hud.calculating")
                    .formatted(Formatting.WHITE);
            hudString[6] = Text.of("");
        }
        else {
            hudString[5] = Text.translatable("text.elytraautopilot.hud.avgSpeed", String.format("%.2f", avgVelocity * 20))
                    .formatted(Formatting.YELLOW);
            hudString[6] = Text.translatable("text.elytraautopilot.hud.avgHSpeed", String.format("%.2f", avgHorizontalVelocity * 20))
                    .formatted(Formatting.YELLOW);
        }
        if (isflytoActive && !forceLand) {
            hudString[7] = Text.translatable("text.elytraautopilot.flyto", argXpos, argZpos)
                    .formatted(Formatting.LIGHT_PURPLE);
            if (distance != 0f) {
                hudString[8] = Text.translatable("text.elytraautopilot.hud.eta", String.valueOf(Math.round(distance/(avgHorizontalVelocity * 20))))
                        .formatted(Formatting.LIGHT_PURPLE);
            }
            hudString[9] = Text.translatable("text.elytraautopilot.hud.autoLand")
                    .formatted(Formatting.LIGHT_PURPLE)
                    .append(Text.translatable(ModConfig.INSTANCE.autoLanding ? "text.elytraautopilot.hud.enabled" : "text.elytraautopilot.hud.disabled")
                            .formatted(ModConfig.INSTANCE.autoLanding ? Formatting.GREEN : Formatting.RED));
            if (isLanding) {
                hudString[8] = Text.translatable("text.elytraautopilot.hud.landing")
                        .formatted(Formatting.LIGHT_PURPLE);
            }
        }
        else {
            hudString[7] = Text.of("");
            hudString[8] = Text.of("");
            hudString[9] = Text.of("");
        }
        if (forceLand) {
            hudString[7] = Text.translatable("text.elytraautopilot.hud.landing")
                    .formatted(Formatting.LIGHT_PURPLE);
        }
    }

    public static void clearHud() {
        velocityList.clear();
        velocityListHorizontal.clear();
    }
}
