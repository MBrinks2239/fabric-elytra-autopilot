package net.elytraautopilot.utils;

import net.minecraft.util.Mth;

/**
 * Which screen edge the HUD is attached to on one axis. The configured X/Y is
 * the distance from that edge, so the HUD stays in the same corner when the
 * window or GUI scale changes.
 */
public enum HudAnchor {
    START, CENTER, END;

    /**
     * Top-left coordinate of a box of {@code size} placed {@code offset} from this
     * anchor.
     */
    public int resolve(int offset, int size, int screenSize) {
        int pos = switch (this) {
            case START -> offset;
            case CENTER -> (screenSize - size) / 2 + offset;
            case END -> screenSize - size - offset;
        };
        return Mth.clamp(pos, 0, Math.max(0, screenSize - size));
    }

    /**
     * Inverse of {@link #resolve}: offset from this anchor for a box at
     * {@code pos}.
     */
    public int toOffset(int pos, int size, int screenSize) {
        return switch (this) {
            case START -> pos;
            case CENTER -> pos - (screenSize - size) / 2;
            case END -> screenSize - size - pos;
        };
    }

    /**
     * X of a line of {@code lineWidth} inside a box, aligned towards this anchor.
     */
    public int align(int boxX, int boxWidth, int lineWidth) {
        return switch (this) {
            case START -> boxX;
            case CENTER -> boxX + (boxWidth - lineWidth) / 2;
            case END -> boxX + boxWidth - lineWidth;
        };
    }

    /**
     * The anchor closest to a box at {@code pos}. Only picks CENTER when the box is
     * (nearly) exactly centered, otherwise whichever half the box is in.
     */
    public static HudAnchor nearest(int pos, int size, int screenSize, int centerTolerance) {
        int boxCenter = pos + size / 2;
        int screenCenter = screenSize / 2;
        if (Math.abs(boxCenter - screenCenter) <= centerTolerance)
            return CENTER;
        return boxCenter < screenCenter ? START : END;
    }
}
