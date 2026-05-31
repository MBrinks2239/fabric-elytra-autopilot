package net.elytraautopilot.mixin;

import net.elytraautopilot.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import net.elytraautopilot.ElytraAutoPilot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.elytraautopilot.utils.ElytraManager.*;

@Mixin(LocalPlayer.class)
public class ClientPlayerEntityMixin {

    @Inject(method = "aiStep", at = @At(
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/player/LocalPlayer;tryToStartFallFlying()Z"))
    private void onPlayerTickMovement(CallbackInfo ci) {
        if (!ModConfig.INSTANCE.elytraAutoSwap) return;

        LocalPlayer player = (LocalPlayer) (Object) this;
        // Injects when the elytra should be deployed
        if (canGlide(player)) { //&&
            // [Future] Replace with an event that fires before elytra take off.
            equipElytra(player);
        }
    }

    @Inject(method = "aiStep", at = @At(value = "TAIL"))
    private void endTickMovement(CallbackInfo ci) {
        if (!ModConfig.INSTANCE.elytraAutoSwap) return;

        LocalPlayer player = (LocalPlayer) (Object) this;
        MultiPlayerGameMode interactionManager = Minecraft.getInstance().gameMode;
        if (interactionManager != null && (player.onGround() || player.isInWater())) {
            player.tryToStartFallFlying();
            if (autoSwapIsActive) {
               equipChestplate(player);
            }
        }
    }

    @Unique
    private static boolean canGlide(LocalPlayer player) {
        return !player.onGround() &&
                !player.isFallFlying() &&
                !player.hasEffect(MobEffects.LEVITATION) &&
                !player.isInWater() &&
                !player.isInLava() &&
                !player.isPassenger();
    }
}
