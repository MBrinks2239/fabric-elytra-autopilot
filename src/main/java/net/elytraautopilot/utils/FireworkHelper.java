package net.elytraautopilot.utils;

import net.elytraautopilot.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

/**
 * Firework inventory helpers for autopilot takeoff and powered flight.
 *
 * <p>
 * Restock uses vanilla {@link ContainerInput#SWAP}: {@code buttonNum} is the
 * destination inventory slot (hotbar {@code 0-8} or offhand {@code 40}) and
 * {@code slotNum} is the firework's container slot. This matches
 * {@code AbstractContainerMenu} (number-key swap while hovering the source
 * stack) and works for fireworks in the main inventory, not only the hotbar.
 */
public final class FireworkHelper {
    private static int restoreDestInvSlot = -1;
    private static int restoreSourceInvSlot = -1;

    private FireworkHelper() {
    }

    public static boolean isFirework(ItemStack stack) {
        return stack.is(Items.FIREWORK_ROCKET);
    }

    /**
     * Prefers the main hand, then the offhand. Empty when neither hand holds a
     * rocket — restock is a separate step.
     */
    public static Optional<InteractionHand> findFireworkHand(Player player) {
        if (isFirework(player.getMainHandItem())) {
            return Optional.of(InteractionHand.MAIN_HAND);
        }
        if (isFirework(player.getOffhandItem())) {
            return Optional.of(InteractionHand.OFF_HAND);
        }
        return Optional.empty();
    }

    /**
     * Moves a rocket from the main inventory into an empty offhand, otherwise into
     * the selected hotbar slot. No-op when hotswap is disabled.
     *
     * <p>
     * When neither hand already held a rocket, the displaced stack is remembered
     * and {@link #restoreOriginalItem(Player)} swaps it back after the rocket is
     * used (or when autopilot input resets).
     *
     * @return {@code true} if a rocket is now in a hand
     */
    public static boolean tryRestockFirework(Player player) {
        if (!ModConfig.INSTANCE.fireworkHotswap) {
            return false;
        }
        if (findFireworkHand(player).isPresent()) {
            return true;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.gameMode == null) {
            return false;
        }

        int destInvSlot;
        if (player.getOffhandItem().isEmpty()) {
            destInvSlot = 40;
        } else {
            destInvSlot = player.getInventory().getSelectedSlot();
        }

        int sourceInvSlot = findFireworkInventorySlot(player, destInvSlot);
        if (sourceInvSlot < 0) {
            return false;
        }

        swapSlots(player, sourceInvSlot, destInvSlot);
        if (findFireworkHand(player).isPresent()) {
            restoreDestInvSlot = destInvSlot;
            restoreSourceInvSlot = sourceInvSlot;
            return true;
        }
        return false;
    }

    /**
     * Swaps the displaced original stack back into the hand that was used as a
     * temporary rocket slot. No-op when restock did not displace anything.
     */
    public static void restoreOriginalItem(Player player) {
        if (restoreDestInvSlot < 0 || player == null) {
            clearRestore();
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.gameMode == null) {
            clearRestore();
            return;
        }
        swapSlots(player, restoreSourceInvSlot, restoreDestInvSlot);
        clearRestore();
    }

    public static void clearRestore() {
        restoreDestInvSlot = -1;
        restoreSourceInvSlot = -1;
    }

    private static void swapSlots(Player player, int sourceInvSlot, int destInvSlot) {
        Minecraft client = Minecraft.getInstance();
        int sourceContainerSlot = ElytraManager.DataSlotToNetworkSlot(sourceInvSlot);
        client.gameMode.handleContainerInput(player.inventoryMenu.containerId, sourceContainerSlot, destInvSlot,
                ContainerInput.SWAP, player);
    }

    private static int findFireworkInventorySlot(Player player, int destInvSlot) {
        var items = player.getInventory().getNonEquipmentItems();
        for (int slot = 0; slot < items.size(); slot++) {
            if (slot == destInvSlot) {
                continue;
            }
            if (isFirework(items.get(slot))) {
                return slot;
            }
        }
        return -1;
    }
}
