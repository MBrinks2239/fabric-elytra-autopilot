package net.elytraautopilot.utils;

import net.elytraautopilot.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

public class ElytraManager {
    private static final int CHESTPLATE_INDEX = 6;
    private static int lastUID = -1;
    public static boolean autoSwapIsActive = false;

    public static int getElytraDurability(LocalPlayer player) {
        var elytra = getChestplateSlot(player);
        return elytra.getMaxDamage() - elytra.getDamageValue();
    }

    public static boolean equipElytra(LocalPlayer player) {
        int elytraIndex = getElytraIndex(player);
        if (elytraIndex != -100) {
            ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
            lastUID = getItemUID(stack);
            swapChestplateSlot(elytraIndex, player);
            autoSwapIsActive = true;
            return true;
        }

        return false;
    }

    public static ItemStack getChestplateSlot(LocalPlayer player) {
        return player.getItemBySlot(EquipmentSlot.CHEST);
    }

    public static boolean equipChestplate(LocalPlayer player) {
        int chestplateIndex = getLastChestplateIndex(player);
        if (chestplateIndex != -100) {
            swapChestplateSlot(chestplateIndex, player);
            lastUID = -100;
            autoSwapIsActive = false;
            return true;
        }

        return false;
    }

    private static void swapChestplateSlot(int slot, LocalPlayer player) {
        var interactionManager = Minecraft.getInstance().gameMode;
        assert interactionManager != null;
        interactionManager.handleContainerInput(0, slot, 0, ContainerInput.PICKUP, player);
        interactionManager.handleContainerInput(0, CHESTPLATE_INDEX, 0, ContainerInput.PICKUP, player);
        interactionManager.handleContainerInput(0, slot, 0, ContainerInput.PICKUP, player);
    }

    private static int getItemUID(ItemStack stack) {
        if (stack.isEmpty())
            return -100;
        return stack.getHoverName().hashCode() + stack.getEnchantments().hashCode() + stack.getDamageValue();
    }

    private static int getLastChestplateIndex(LocalPlayer player) {
        Inventory inv = player.getInventory();
        if (inv == null)
            return -100;

        for (int slot : slotArray()) {
            ItemStack stack = inv.getItem(slot);
            if (getItemUID(stack) == lastUID) {
                return DataSlotToNetworkSlot(slot);
            }
        }
        return -100;
    }

    public static int getElytraIndex(Player player) {
        Inventory inv = player.getInventory();
        if (inv == null)
            return -100;

        var world = player.level();

        int bestSlot = -100;
        ItemStack bestItemStack = null;
        int bestPriority = Integer.MAX_VALUE;

        for (int slot : slotArray()) {
            ItemStack stack = inv.getItem(slot);
            if (!stack.is(Items.ELYTRA)
                    || stack.getDamageValue() >= (stack.getMaxDamage() - ModConfig.INSTANCE.elytraReplaceDurability)) {
                continue;
            }

            boolean hasMending = elytraHasMending(stack, world);
            int unbreakingLevel = elytraGetUnbreakingLevel(stack, world);

            int priority;
            if (hasMending && unbreakingLevel > 0) {
                priority = 1;
            } else if (hasMending) {
                priority = 2;
            } else if (unbreakingLevel > 0) {
                priority = 3;
            } else {
                priority = 4;
            }

            if (priority < bestPriority
                    || (priority == bestPriority && stack.getDamageValue() > bestItemStack.getDamageValue())) {
                bestSlot = slot;
                bestItemStack = stack;
                bestPriority = priority;
            }
        }

        return DataSlotToNetworkSlot(bestSlot);
    }

    private static boolean elytraHasMending(ItemStack elytra, Level world) {
        var registry = world.registryAccess().lookup(Registries.ENCHANTMENT);
        if (registry.isEmpty() || registry.get().get(Enchantments.MENDING).isEmpty())
            return false;

        int res = EnchantmentHelper.getItemEnchantmentLevel(registry.get().get(Enchantments.MENDING).get(), elytra);

        return res > 0;
    }

    private static int elytraGetUnbreakingLevel(ItemStack elytra, Level world) {
        var registry = world.registryAccess().lookup(Registries.ENCHANTMENT);
        if (registry.isEmpty() || registry.get().get(Enchantments.UNBREAKING).isEmpty())
            return 0;

        return EnchantmentHelper.getItemEnchantmentLevel(registry.get().get(Enchantments.UNBREAKING).get(), elytra);
    }

    public static int DataSlotToNetworkSlot(int index) {
        if (index == 100)
            index = 8;
        else if (index == 101)
            index = 7;
        else if (index == 102)
            index = 6;
        else if (index == 103)
            index = 5;
        else if (index == -106 || index == 40)
            index = 45;
        else if (index <= 8 && index != -100)
            index += 36;
        else if (index >= 80 && index <= 83)
            index -= 79;
        return index;
    }

    public static int[] slotArray() {
        int[] range = new int[37];
        for (int i = 0; i < 9; i++)
            range[i] = 8 - i;
        for (int i = 9; i < 36; i++)
            range[i] = 35 - (i - 9);
        range[36] = 40;
        return range;
    }
}
