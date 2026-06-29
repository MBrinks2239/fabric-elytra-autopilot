package net.elytraautopilot.mixin;

import net.elytraautopilot.ElytraAutoPilot;
import net.elytraautopilot.config.ModConfig;
import net.elytraautopilot.utils.FreeCameraState;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public class CameraMixin {
    @Shadow
    private int matrixPropertiesDirty;

    @Unique
    private static float savedEntityYaw = 0f;
    @Unique
    private static float savedEntityPitch = 0f;

    @Inject(method = "update", at = @At("HEAD"))
    private void beforeCameraUpdate(DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!ElytraAutoPilot.autoFlight)
            return;
        if (!ModConfig.INSTANCE.cameraDecoupled)
            return;
        if (ElytraAutoPilot.isLanding || ElytraAutoPilot.forceLand)
            return;

        LocalPlayer player = ElytraAutoPilot.minecraftClient.player;
        if (player == null)
            return;

        // Save entity rotation so we can restore it after Camera.update()
        savedEntityYaw = player.getYRot();
        savedEntityPitch = player.getXRot();

        // Temporarily set entity rotation to free camera values, so that
        // alignWithEntity() -> setRotation() produces the correct camera quaternion,
        // and the view rotation matrix & frustum are computed from free camera
        // rotation.
        if (ElytraAutoPilot.isflytoActive) {
            player.setYRot(FreeCameraState.yaw);
            player.setXRot(FreeCameraState.pitch);
        } else {
            // Free cruise: pitch decoupled, yaw stays mouse-controlled
            player.setXRot(FreeCameraState.pitch);
        }

        // Dirty the cached view rotation matrix so that getViewRotationMatrix()
        // recomputes it from the (soon-to-be-updated) rotation quaternion.
        this.matrixPropertiesDirty |= 1;
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void afterCameraUpdate(DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!ElytraAutoPilot.autoFlight)
            return;
        if (!ModConfig.INSTANCE.cameraDecoupled)
            return;
        if (ElytraAutoPilot.isLanding || ElytraAutoPilot.forceLand)
            return;

        // Restore entity rotation to mod-controlled values for flight physics
        LocalPlayer player = ElytraAutoPilot.minecraftClient.player;
        if (player == null)
            return;
        player.setYRot(savedEntityYaw);
        player.setXRot(savedEntityPitch);
    }
}
