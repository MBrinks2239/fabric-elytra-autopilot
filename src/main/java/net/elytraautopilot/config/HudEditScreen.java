package net.elytraautopilot.config;

import com.mojang.blaze3d.platform.InputConstants;
import net.elytraautopilot.utils.Hud;
import net.elytraautopilot.utils.HudAnchor;
import net.elytraautopilot.utils.HudRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Locale;

/**
 * Lets the player drag a preview of the flight HUD to where they want it. The
 * position is anchored to the nearest screen edge so it stays put when the
 * window is resized. Esc and Done keep the new position, Cancel restores the
 * old one.
 */
public class HudEditScreen extends Screen {
    private static final int SNAP_DISTANCE = 6;
    private static final int PADDING = 2;
    // Gap kept between the HUD text and a screen edge it is snapped to
    private static final int EDGE_MARGIN = ModConfig.guiXDefault;

    public interface PositionConsumer {
        void accept(HudAnchor anchorX, HudAnchor anchorY, int offsetX, int offsetY);
    }

    private final Screen parent;
    private final PositionConsumer onSave;
    private Component[] previewLines;

    // Stored the same way as the config, so resizing the window while editing
    // moves the box exactly like the in-game HUD would
    private HudAnchor anchorX;
    private HudAnchor anchorY;
    private int offsetX;
    private int offsetY;

    private boolean dragging;
    private double dragOffsetX;
    private double dragOffsetY;
    private boolean cancelled;

    public HudEditScreen(Screen parent, HudAnchor anchorX, HudAnchor anchorY, int offsetX, int offsetY,
            PositionConsumer onSave) {
        super(Component.translatable("config.elytraautopilot.hudEditor.title"));
        this.parent = parent;
        this.onSave = onSave;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    @Override
    protected void init() {
        // Prefer the live HUD so the box is the real size, otherwise show
        // representative sample lines
        previewLines = Hud.hudString != null && Hud.hudString.length > 0 ? Hud.hudString : sampleLines();

        int buttonWidth = 80;
        int spacing = 4;
        int left = width / 2 - (buttonWidth * 3 + spacing * 2) / 2;
        int top = height - 28;
        addRenderableWidget(
                Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(left, top, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("config.elytraautopilot.hudEditor.reset"), b -> {
            anchorX = HudAnchor.START;
            anchorY = HudAnchor.START;
            offsetX = ModConfig.guiXDefault;
            offsetY = ModConfig.guiYDefault;
        }).bounds(left + buttonWidth + spacing, top, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> {
            cancelled = true;
            onClose();
        }).bounds(left + (buttonWidth + spacing) * 2, top, buttonWidth, 20).build());
    }

    private static Component[] sampleLines() {
        return new Component[]{
                Component.translatable("text.elytraautopilot.hud.toggleAutoFlight").append(
                        Component.translatable("text.elytraautopilot.hud.true").withStyle(ChatFormatting.GREEN)),
                Component.translatable("text.elytraautopilot.hud.altitude", "250.00").withStyle(ChatFormatting.AQUA),
                Component.translatable("text.elytraautopilot.hud.heightFromGround", "180")
                        .withStyle(ChatFormatting.AQUA),
                Component.translatable("text.elytraautopilot.hud.speed", "33.50").withStyle(ChatFormatting.YELLOW),
                Component.translatable("text.elytraautopilot.hud.avgSpeed", "31.20").withStyle(ChatFormatting.YELLOW),
                Component.translatable("text.elytraautopilot.hud.avgHSpeed", "30.80").withStyle(ChatFormatting.YELLOW),
                Component.translatable("text.elytraautopilot.hud.eta", "120").withStyle(ChatFormatting.AQUA)};
    }

    private int boxWidth() {
        return Math.max(HudRenderer.boxWidth(font, previewLines), 20);
    }

    private int boxHeight() {
        return Math.max(HudRenderer.boxHeight(font, previewLines), font.lineHeight);
    }

    private int boxX() {
        return anchorX.resolve(offsetX, boxWidth(), width);
    }

    private int boxY() {
        return anchorY.resolve(offsetY, boxHeight(), height);
    }

    /**
     * Moves the box to an absolute position and re-anchors it to the nearest edges.
     */
    private void moveTo(int x, int y) {
        int w = boxWidth();
        int h = boxHeight();
        // Keep the whole box, including its padding, on screen
        x = Mth.clamp(x, PADDING, Math.max(PADDING, width - w - PADDING));
        y = Mth.clamp(y, PADDING, Math.max(PADDING, height - h - PADDING));
        anchorX = HudAnchor.nearest(x, w, width, 1);
        anchorY = HudAnchor.nearest(y, h, height, 1);
        offsetX = anchorX.toOffset(x, w, width);
        offsetY = anchorY.toOffset(y, h, height);
    }

    private static int snap(int pos, int size, int screenSize) {
        if (pos <= EDGE_MARGIN + SNAP_DISTANCE)
            return EDGE_MARGIN;
        if (pos + size >= screenSize - EDGE_MARGIN - SNAP_DISTANCE)
            return screenSize - size - EDGE_MARGIN;
        int centered = (screenSize - size) / 2;
        if (Math.abs(pos - centered) <= SNAP_DISTANCE)
            return centered;
        return pos;
    }

    private boolean isOverBox(double mouseX, double mouseY) {
        int x = boxX();
        int y = boxY();
        return mouseX >= x - PADDING && mouseX < x + boxWidth() + PADDING && mouseY >= y - PADDING
                && mouseY < y + boxHeight() + PADDING;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && isOverBox(event.x(), event.y())) {
            dragging = true;
            dragOffsetX = event.x() - boxX();
            dragOffsetY = event.y() - boxY();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging) {
            int x = (int) Math.round(event.x() - dragOffsetX);
            int y = (int) Math.round(event.y() - dragOffsetY);
            // Hold shift for pixel-precise placement without snapping
            if (!event.hasShiftDown()) {
                x = snap(x, boxWidth(), width);
                y = snap(y, boxHeight(), height);
            }
            moveTo(x, y);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int step = event.hasShiftDown() ? 10 : 1;
        int x = boxX();
        int y = boxY();
        switch (event.key()) {
            case InputConstants.KEY_LEFT -> x -= step;
            case InputConstants.KEY_RIGHT -> x += step;
            case InputConstants.KEY_UP -> y -= step;
            case InputConstants.KEY_DOWN -> y += step;
            default -> {
                return super.keyPressed(event);
            }
        }
        moveTo(x, y);
        return true;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (minecraft.level == null) {
            super.extractBackground(graphics, mouseX, mouseY, a);
        } else {
            // Keep the world visible so the HUD can be placed relative to
            // what's on screen
            graphics.fill(0, 0, width, height, 0x40000000);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int x = boxX();
        int y = boxY();
        int x0 = x - PADDING;
        int y0 = y - PADDING;
        int w = boxWidth() + PADDING * 2;
        int h = boxHeight() + PADDING * 2;
        boolean highlighted = dragging || isOverBox(mouseX, mouseY);
        graphics.fill(x0, y0, x0 + w, y0 + h, highlighted ? 0x80FFFFFF : 0x60000000);
        graphics.outline(x0, y0, w, h, highlighted ? 0xFFFFFF55 : 0xFFFFFFFF);
        HudRenderer.drawLines(graphics, font, previewLines, x, y, boxWidth(), anchorX);

        if (!dragging) {
            graphics.centeredText(font, title, width / 2, height / 2 - 20, 0xFFFFFFFF);
            graphics.centeredText(font, Component.translatable("config.elytraautopilot.hudEditor.hint"), width / 2,
                    height / 2 - 8, 0xFFAAAAAA);
            graphics.centeredText(font, Component.translatable("config.elytraautopilot.hudEditor.anchor",
                    anchorName(anchorY, true), anchorName(anchorX, false), offsetX, offsetY), width / 2, height / 2 + 4,
                    0xFFAAAAAA);
            super.extractRenderState(graphics, mouseX, mouseY, a);
        }
    }

    public static Component anchorName(HudAnchor anchor, boolean vertical) {
        String axis = vertical ? "vertical" : "horizontal";
        return Component.translatable(
                "config.elytraautopilot.gui.anchor." + axis + "." + anchor.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public void onClose() {
        if (!cancelled) {
            onSave.accept(anchorX, anchorY, offsetX, offsetY);
        }
        minecraft.gui.setScreen(parent);
    }
}
