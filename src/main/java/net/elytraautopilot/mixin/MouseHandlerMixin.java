package net.elytraautopilot.mixin;

import com.mojang.blaze3d.platform.InputConstants;

import net.elytraautopilot.ElytraAutoPilot;
import net.elytraautopilot.utils.FreeCameraState;
import net.elytraautopilot.utils.KeyBindings;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    /**
     * Mirror of KeyboardHandlerMixin for bindings mapped to a mouse button. Vanilla
     * only calls click() here for presses that reach game input; mouse buttons do
     * not auto-repeat.
     */
    @Inject(method = "onButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;click(Lcom/mojang/blaze3d/platform/InputConstants$Key;)V"))
    private void onKeyMappingClick(long handle, MouseButtonInfo rawButtonInfo, int action, CallbackInfo ci) {
        KeyBindings.onKeyPressed(InputConstants.Type.MOUSE.getOrCreate(rawButtonInfo.button()));
    }

    @Unique
    private static boolean shouldRedirect() {
        return FreeCameraState.isActive();
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
            FreeCameraState.addYaw((float) deltaYaw * scale);
            FreeCameraState.addPitch((float) deltaPitch * scale);
        } else {
            // Free cruise: pitch to free camera, yaw to entity (controls flight direction)
            FreeCameraState.addPitch((float) deltaPitch * scale);
            player.turn(deltaYaw, 0.0);
        }
    }
}
