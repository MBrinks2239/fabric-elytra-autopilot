package net.elytraautopilot.utils;

import net.elytraautopilot.ElytraAutoPilot;
import net.elytraautopilot.config.ModConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

public class HudRenderer {
    public static void drawHud(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();

        if (client.gui.screen() == null && ElytraAutoPilot.calculateHud && Hud.hudString != null) {
            ModConfig config = ModConfig.INSTANCE;
            Font font = client.font;
            float scale = scaleFactor(config.guiScale);
            int boxWidth = boxWidth(font, Hud.hudString);

            // Anchor using the on-screen (scaled) size so edges line up at any scale
            int x = config.guiAnchorX.resolve(config.guiX, scaled(boxWidth, scale), context.guiWidth());
            int y = config.guiAnchorY.resolve(config.guiY, scaled(boxHeight(font, Hud.hudString), scale),
                    context.guiHeight());

            drawLines(context, font, Hud.hudString, x, y, boxWidth, scale, config.guiAnchorX);
        }
    }

    /**
     * Draws the lines with their top-left at {@code x}/{@code y}, scaled by
     * {@code scale}. {@code boxWidth} is the unscaled width of the widest line.
     */
    public static void drawLines(GuiGraphicsExtractor context, Font font, Component[] lines, int x, int y, int boxWidth,
            float scale, HudAnchor alignment) {
        if (scale <= 0)
            return;
        Matrix3x2fStack pose = context.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
        int lineY = 0;
        for (Component line : lines) {
            int lineX = alignment.align(0, boxWidth, font.width(line));
            context.text(font, line.getVisualOrderText(), lineX, lineY, 0xFFFFFFFF);
            lineY += lineHeight(font);
        }
        pose.popMatrix();
    }

    public static float scaleFactor(int guiScalePercent) {
        return Math.max(0, guiScalePercent) / 100f;
    }

    public static int scaled(int size, float scale) {
        return (int) Math.ceil(size * scale);
    }

    public static int boxWidth(Font font, Component[] lines) {
        int max = 0;
        for (Component line : lines) {
            max = Math.max(max, font.width(line));
        }
        return max;
    }

    public static int boxHeight(Font font, Component[] lines) {
        return Math.max(0, lines.length * lineHeight(font) - 1);
    }

    public static int lineHeight(Font font) {
        return font.lineHeight + 1;
    }
}
