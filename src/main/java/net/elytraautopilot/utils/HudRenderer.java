package net.elytraautopilot.utils;

import net.elytraautopilot.ElytraAutoPilot;
import net.elytraautopilot.config.ModConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class HudRenderer {
    public static void drawHud(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();

        if (client.screen == null && ElytraAutoPilot.calculateHud && Hud.hudString != null) {
            int screenWidth = context.guiWidth();
            int screenHeight = context.guiHeight();

            int stringX = Mth.clamp(ModConfig.INSTANCE.guiX, 0, screenWidth);
            int stringY = Mth.clamp(ModConfig.INSTANCE.guiY, 0, screenHeight);

            int lineHeight = client.font.lineHeight + 1;

            // Draw the lines
            for (Component line : Hud.hudString) {
                context.text(client.font, line.getVisualOrderText(), stringX, stringY, 0xFFFFFFFF);
                stringY += lineHeight;
            }
        }
    }
}
