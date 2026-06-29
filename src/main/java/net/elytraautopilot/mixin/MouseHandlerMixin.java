package net.elytraautopilot.mixin;

import net.elytraautopilot.ElytraAutoPilot;
import net.elytraautopilot.config.ModConfig;
import net.elytraautopilot.utils.FreeCameraState;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Unique
    private static boolean shouldRedirect() {
        return ElytraAutoPilot.autoFlight && ModConfig.INSTANCE.cameraDecoupled && !ElytraAutoPilot.isLanding
                && !ElytraAutoPilot.forceLand;
    }

    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void redirectTurn(LocalPlayer player, double deltaYaw, double deltaPitch) {
        if (!shouldRedirect()) {
            player.turn(deltaYaw, deltaPitch);
            return;
        }
        // deltaYaw/deltaPitch are the raw values BEFORE Entity.turn() applies
        // its internal 0.15 scale factor. Replicate that factor here so that
        // the free camera moves at the same sensitivity as normal gameplay.
        float scale = 0.15f;
        if (ElytraAutoPilot.isflytoActive) {
            // Full decouple: both yaw and pitch go to free camera
            FreeCameraState.yaw += (float) deltaYaw * scale;
            FreeCameraState.addPitch((float) deltaPitch * scale);
        } else {
            // Free cruise: pitch to free camera, yaw to entity (controls flight direction)
            FreeCameraState.addPitch((float) deltaPitch * scale);
            player.turn(deltaYaw, 0.0);
        }
    }
}
