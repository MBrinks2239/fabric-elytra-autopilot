package net.elytraautopilot.utils;

import net.elytraautopilot.ElytraAutoPilot;
import net.elytraautopilot.config.ModConfig;
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

    /** Whether free look currently owns the camera. */
    private static boolean active = false;

    /**
     * Whether the camera is decoupled from the flight controller right now. Shared
     * by the camera and mouse mixins so both always agree.
     */
    public static boolean isActive() {
        return ElytraAutoPilot.autoFlight && ModConfig.INSTANCE.cameraDecoupled && !ElytraAutoPilot.isLanding
                && !ElytraAutoPilot.forceLand;
    }

    /**
     * Enter free look. The first activation of a flight seeds {@link #yaw} and
     * {@link #pitch} from the current player rotation so the camera does not jump
     * when the option is toggled mid-flight.
     */
    public static void begin() {
        if (active) {
            return;
        }
        active = true;
        init();
    }

    /** Leave free look; the next {@link #begin()} re-seeds the angles. */
    public static void end() {
        active = false;
    }

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
        active = false;
        yaw = 0f;
        pitch = 0f;
    }

    /**
     * Add yaw delta (used in full-decouple mode), keeping it wrapped to [-180,
     * 180].
     */
    public static void addYaw(float delta) {
        yaw = Mth.wrapDegrees(yaw + delta);
    }

    /** Add pitch delta and clamp to valid range. */
    public static void addPitch(float delta) {
        pitch = Mth.clamp(pitch + delta, -90f, 90f);
    }
}
