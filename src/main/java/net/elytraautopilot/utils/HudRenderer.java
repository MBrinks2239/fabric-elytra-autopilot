package net.elytraautopilot.utils;

import net.elytraautopilot.ElytraAutoPilot;
import net.elytraautopilot.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class HudRenderer {
    public static void drawHud(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.currentScreen == null && ElytraAutoPilot.calculateHud && Hud.hudString != null) {
            int screenWidth = context.getScaledWindowWidth();
            int screenHeight = context.getScaledWindowHeight();

            int stringX = MathHelper.clamp(ModConfig.INSTANCE.guiX, 0, screenWidth);
            int stringY = MathHelper.clamp(ModConfig.INSTANCE.guiY, 0, screenHeight);

            int lineHeight = client.textRenderer.fontHeight + 1;

            // Draw the lines
            for (Text line : Hud.hudString) {
                context.drawTextWithShadow(
                        client.textRenderer,
                        line.asOrderedText(),
                        stringX,
                        stringY,
                        0xFFFFFFFF
                );
                stringY += lineHeight;
            }
        }
    }
}

