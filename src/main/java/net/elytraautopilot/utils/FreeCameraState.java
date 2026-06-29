package net.elytraautopilot.utils;

import net.elytraautopilot.ElytraAutoPilot;
import net.minecraft.util.Mth;

/**
 * Manages the free camera state when camera is decoupled from entity rotation
 * during autopilot flight.
 */
public final class FreeCameraState {
    private FreeCameraState() {
    }

    /** Free-look yaw, accumulated from mouse input when {@code isflytoActive}. */
    public static float yaw = 0f;
    /** Free-look pitch, accumulated from mouse input in all flight modes. */
    public static float pitch = 0f;

    /** Initialise free camera from the current player rotation. */
    public static void init() {
        var player = ElytraAutoPilot.minecraftClient.player;
        if (player != null) {
            yaw = player.getYRot();
            pitch = player.getXRot();
        }
    }

    /** Reset free camera to defaults when autopilot is turned off. */
    public static void reset() {
        yaw = 0f;
        pitch = 0f;
    }

    /** Add yaw delta (used in full-decouple mode). */
    public static void addYaw(float delta) {
        yaw += delta;
    }

    /** Add pitch delta and clamp to valid range. */
    public static void addPitch(float delta) {
        pitch = Mth.clamp(pitch + delta, -90f, 90f);
    }
}
