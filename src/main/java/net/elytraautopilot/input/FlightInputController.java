package net.elytraautopilot.input;

import net.elytraautopilot.utils.FireworkHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

/**
 * Tick-driven use/jump actuators for takeoff and powered flight.
 *
 * <p>
 * Never writes {@code KeyMapping.setDown}. Fireworks go through
 * {@code MultiPlayerGameMode.useItem} on the rocket hand only, so a food item
 * in the other hand is not consumed. Jump is OR'd into {@code ClientInput} via
 * {@link #shouldJumpThisTick()} after {@code ClientInput.tick()}, matching
 * vanilla auto-jump.
 *
 * <p>
 * Player right-click / item-use always wins: while {@code keyUse} is down or
 * the player is using an item, this controller will not restock or fire.
 */
public final class FlightInputController {
    private static final int USE_COOLDOWN_TICKS = 4;

    private enum JumpState {
        IDLE, HOLD, PULSE
    }

    private static JumpState jumpState = JumpState.IDLE;
    private static boolean pulseJump;
    private static int useCooldownTicks;
    private static boolean takeoffJumpRequested;
    private static boolean takeoffRestockRequested;
    private static boolean takeoffBoostRequested;
    private static boolean poweredFlightRequested;
    private static boolean takeoffFireworkFailed;

    private FlightInputController() {
    }

    public static void reset() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            FireworkHelper.restoreOriginalItem(client.player);
        } else {
            FireworkHelper.clearRestore();
        }
        jumpState = JumpState.IDLE;
        pulseJump = false;
        useCooldownTicks = 0;
        takeoffJumpRequested = false;
        takeoffRestockRequested = false;
        takeoffBoostRequested = false;
        poweredFlightRequested = false;
        takeoffFireworkFailed = false;
    }

    public static void requestTakeoffJump() {
        takeoffJumpRequested = true;
    }

    /**
     * Keep rockets in hand during takeoff and optionally fire a boost rocket.
     */
    public static void requestTakeoff(boolean boost) {
        takeoffJumpRequested = true;
        takeoffRestockRequested = true;
        if (boost) {
            takeoffBoostRequested = true;
        }
    }

    public static void requestPoweredFlight() {
        poweredFlightRequested = true;
    }

    public static boolean shouldJumpThisTick() {
        return jumpState == JumpState.HOLD || (jumpState == JumpState.PULSE && pulseJump);
    }

    public static boolean consumeTakeoffFireworkFailure() {
        boolean failed = takeoffFireworkFailed;
        takeoffFireworkFailed = false;
        return failed;
    }

    public static void tick(Minecraft client) {
        takeoffFireworkFailed = false;
        LocalPlayer player = client.player;
        if (player == null) {
            reset();
            return;
        }

        if (client.isPaused()) {
            clearRequests();
            return;
        }

        tickJump(player);
        tickUse(client, player);
        clearRequests();
    }

    private static void clearRequests() {
        takeoffJumpRequested = false;
        takeoffRestockRequested = false;
        takeoffBoostRequested = false;
        poweredFlightRequested = false;
    }

    private static void tickJump(LocalPlayer player) {
        if (!takeoffJumpRequested || player.isFallFlying()) {
            jumpState = JumpState.IDLE;
            pulseJump = false;
            return;
        }

        if (player.onGround()) {
            jumpState = JumpState.HOLD;
            pulseJump = false;
            return;
        }

        if (jumpState != JumpState.PULSE) {
            // First airborne tick releases jump so the next pulse is a rising edge.
            // wasJumping in LocalPlayer.aiStep is sampled *before* input.tick().
            jumpState = JumpState.PULSE;
            pulseJump = false;
        } else {
            pulseJump = !pulseJump;
        }
    }

    private static void tickUse(Minecraft client, LocalPlayer player) {
        if (useCooldownTicks > 0) {
            useCooldownTicks--;
            return;
        }

        boolean wantRocket = takeoffBoostRequested || poweredFlightRequested;
        boolean wantRestock = takeoffRestockRequested || wantRocket;
        if (!wantRestock) {
            return;
        }

        if (isPlayerOccupyingUse(client, player)) {
            return;
        }

        var hand = FireworkHelper.findFireworkHand(player);
        if (hand.isEmpty()) {
            if (!FireworkHelper.tryRestockFirework(player)) {
                if (takeoffRestockRequested || takeoffBoostRequested) {
                    takeoffFireworkFailed = true;
                }
                return;
            }
            hand = FireworkHelper.findFireworkHand(player);
            if (hand.isEmpty()) {
                if (takeoffRestockRequested || takeoffBoostRequested) {
                    takeoffFireworkFailed = true;
                }
                return;
            }
        }

        if (!wantRocket) {
            return;
        }
        if (takeoffBoostRequested && !player.isFallFlying()) {
            return;
        }
        if (client.gui.screen() != null || player.isHandsBusy()) {
            return;
        }
        if (client.gameMode != null && client.gameMode.isDestroying()) {
            return;
        }

        fireRocket(client, player, hand.get());
        useCooldownTicks = USE_COOLDOWN_TICKS;
        FireworkHelper.restoreOriginalItem(player);
    }

    private static boolean isPlayerOccupyingUse(Minecraft client, LocalPlayer player) {
        return client.options.keyUse.isDown() || player.isUsingItem();
    }

    private static void fireRocket(Minecraft client, LocalPlayer player, InteractionHand hand) {
        if (client.gameMode == null) {
            return;
        }
        InteractionResult result = client.gameMode.useItem(player, hand);
        if (result instanceof InteractionResult.Success success
                && success.swingSource() == InteractionResult.SwingSource.CLIENT) {
            player.swing(hand);
        }
    }
}
