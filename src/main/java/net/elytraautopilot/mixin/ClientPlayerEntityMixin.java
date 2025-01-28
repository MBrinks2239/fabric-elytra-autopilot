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

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    @Unique
    private static final int CHESTPLATE_INDEX = 6;
    @Unique
    private int lastIndex = -1;
    @Unique
    private int lastUID = -1;

    @Inject(method = "tickMovement", at = @At(
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;checkGliding()Z"))
    private void onPlayerTickMovement(CallbackInfo ci) {
        if (!ModConfig.INSTANCE.elytraAutoSwap) return;

        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        ClientPlayerInteractionManager interactionManager = MinecraftClient.getInstance().interactionManager;
        // Injects when the elytra should be deployed
        if (!player.isOnGround() &&
            !player.isGliding() &&
            !player.hasStatusEffect(StatusEffects.LEVITATION)) { //&&
            // [Future] Replace with an event that fires before elytra take off.
            this.equipElytra(player, interactionManager);
        }
    }

    @Inject(method = "tickMovement", at = @At(value = "TAIL"))
    private void endTickMovement(CallbackInfo ci) {
        if (!ModConfig.INSTANCE.elytraAutoSwap) return;

        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        ClientPlayerInteractionManager interactionManager = MinecraftClient.getInstance().interactionManager;
        if (interactionManager != null && (player.isOnGround() || player.isTouchingWater())) {
            player.checkGliding();
            if (this.lastUID != -1) {
                int slot = getLastChestplate(player);
                if (slot != -1) {
                    swap(slot, interactionManager, player);
                    lastIndex = -1;
                }
                this.lastUID = -1;
            }
            if (this.lastIndex != -1) {
                swap(this.lastIndex, interactionManager, player);
                lastIndex = -1;
            }
        }
    }

    @Unique
    private void equipElytra(ClientPlayerEntity player, ClientPlayerInteractionManager interactionManager) {
        int firstElytraIndex = ElytraAutoPilot.getElytraIndex(player);
        if (firstElytraIndex != -1) {
            this.lastIndex = firstElytraIndex;
            ItemStack stack = player.getInventory().armor.get(2);
            this.lastUID = getItemUID(stack);
            swap(firstElytraIndex, interactionManager, player);
            // Send packet so server knows player is falling
            player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        }
    }

    @Unique
    private static void swap(int slot, ClientPlayerInteractionManager interactionManager, ClientPlayerEntity player) {
        interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, player);
        interactionManager.clickSlot(0, CHESTPLATE_INDEX, 0, SlotActionType.PICKUP, player);
        interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, player);
    }

    @Unique
    private static int getItemUID(ItemStack stack) {
        if (stack.isEmpty()) return -1;
        return stack.getName().hashCode() + stack.getEnchantments().hashCode() + stack.getDamage();
    }

    @Unique
    private int getLastChestplate(ClientPlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        if (inv == null) return -1;

        for (int slot : ElytraAutoPilot.slotArray()) {
            ItemStack stack = inv.getStack(slot);
            if (getItemUID(stack) == this.lastUID) {
                return ElytraAutoPilot.DataSlotToNetworkSlot(slot);
            }
        }
        return -1;
    }
}
