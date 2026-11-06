package net.elytraautopilot.mixin;

import net.elytraautopilot.ElytraAutoPilot;
import net.elytraautopilot.utils.FreeCameraState;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drives the free camera by overriding the <em>camera</em> rotation after
 * vanilla has resolved it, instead of temporarily rewriting the player rotation
 * while {@code Camera.update()} runs.
 *
 * <p>
 * The old approach (set {@code player.setYRot/setXRot} at the head of
 * {@code Camera.update()}, restore at the tail) only worked while
 * {@code LocalPlayer} short-circuited the partial-tick rotation interpolation.
 * Minecraft 26.3 dropped those {@code LocalPlayer#getViewYRot(float)} /
 * {@code getViewXRot(float)} overrides, so {@code Camera.alignWithEntity()} now
 * feeds {@code Mth.rotLerp(partialTick, yRotO, yRot)} into
 * {@code setRotation()}. Since only {@code yRot/xRot} were swapped and never
 * {@code yRotO/xRotO} — and {@code setOldRot()} resets those every tick — the
 * camera received a per-tick blend of the flight pitch and the free-look pitch
 * and visibly snapped back 20 times per second.
 *
 * <p>
 * Overriding the camera rotation itself is interpolation free, cannot corrupt
 * the entity rotation used by the flight controller, and (because it happens
 * before {@code prepareCullFrustum()}) keeps chunk culling aligned with what
 * the player is actually looking at.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    /** Yaw the camera would have used; kept for the pitch-only (cruise) mode. */
    @Shadow
    private float yRot;

    @Invoker("setRotation")
    protected abstract void elytraautopilot$setRotation(float yRot, float xRot);

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void elytraautopilot$applyFreeLook(float partialTicks, CallbackInfo ci) {
        if (!FreeCameraState.isActive()) {
            FreeCameraState.end();
            return;
        }
        // Seed yaw/pitch from the entity the first time free look becomes active
        // so enabling the option mid-flight does not teleport the view.
        FreeCameraState.begin();
        // Fly-to: the autopilot owns the heading, so both axes are free.
        // Cruise: the mouse steers the flight direction, only pitch is free.
        float yaw = ElytraAutoPilot.isflytoActive ? FreeCameraState.yaw : this.yRot;
        this.elytraautopilot$setRotation(yaw, FreeCameraState.pitch);
    }
}
