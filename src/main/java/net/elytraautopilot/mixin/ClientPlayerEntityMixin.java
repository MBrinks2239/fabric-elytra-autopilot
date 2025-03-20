package net.elytraautopilot.mixin;

import net.elytraautopilot.config.ModConfig;
import net.elytraautopilot.ElytraAutoPilot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.elytraautopilot.utils.ElytraManager.*;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    @Inject(method = "tickMovement", at = @At(
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;checkGliding()Z"))
    private void onPlayerTickMovement(CallbackInfo ci) {
        if (!ModConfig.INSTANCE.elytraAutoSwap) return;

        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        // Injects when the elytra should be deployed
        if (canGlide(player)) { //&&
            // [Future] Replace with an event that fires before elytra take off.
            equipElytra(player);
        }
    }

    @Inject(method = "tickMovement", at = @At(value = "TAIL"))
    private void endTickMovement(CallbackInfo ci) {
        if (!ModConfig.INSTANCE.elytraAutoSwap) return;

        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        ClientPlayerInteractionManager interactionManager = MinecraftClient.getInstance().interactionManager;
        if (interactionManager != null && (player.isOnGround() || player.isTouchingWater())) {
            player.checkGliding();
            if (autoSwapIsActive) {
               equipChestplate(player);
            }
        }
    }

    @Unique
    private static boolean canGlide(ClientPlayerEntity player) {
        return !player.isOnGround() &&
                !player.isGliding() &&
                !player.hasStatusEffect(StatusEffects.LEVITATION) &&
                !player.isTouchingWater() &&
                !player.isInLava() &&
                !player.hasVehicle();
    }
}
