package net.elytraautopilot.utils;

import net.elytraautopilot.ElytraAutoPilot;
import net.elytraautopilot.config.ModConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class HudRenderer {
    public static void drawHud(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();

        if (client.gui.screen() == null && ElytraAutoPilot.calculateHud && Hud.hudString != null) {
            ModConfig config = ModConfig.INSTANCE;
            Font font = client.font;
            int boxWidth = boxWidth(font, Hud.hudString);

            int x = config.guiAnchorX.resolve(config.guiX, boxWidth, context.guiWidth());
            int y = config.guiAnchorY.resolve(config.guiY, boxHeight(font, Hud.hudString), context.guiHeight());

            drawLines(context, font, Hud.hudString, x, y, boxWidth, config.guiAnchorX);
        }
    }

    public static void drawLines(GuiGraphicsExtractor context, Font font, Component[] lines, int x, int y, int boxWidth,
            HudAnchor alignment) {
        for (Component line : lines) {
            int lineX = alignment.align(x, boxWidth, font.width(line));
            context.text(font, line.getVisualOrderText(), lineX, y, 0xFFFFFFFF);
            y += lineHeight(font);
        }
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
