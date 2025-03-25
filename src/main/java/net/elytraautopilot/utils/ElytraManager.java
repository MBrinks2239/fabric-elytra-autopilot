package net.elytraautopilot.utils;

import net.elytraautopilot.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.world.World;

public class ElytraManager {
    private static final int CHESTPLATE_INDEX = 6;
    private static int lastUID = -1;
    public static boolean autoSwapIsActive = false;

    public static int getElytraDurability(ClientPlayerEntity player) {
        var elytra = getChestplateSlot(player);
        return elytra.getMaxDamage() - elytra.getDamage();
    }

    public static boolean equipElytra(ClientPlayerEntity player) {
        int elytraIndex = getElytraIndex(player);
        if (elytraIndex != -100) {
            ItemStack stack = player.getEquippedStack(EquipmentSlot.CHEST);
            lastUID = getItemUID(stack);
            swapChestplateSlot(elytraIndex, player);
            autoSwapIsActive = true;
            // Send packet so server knows player is flying
            player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            return true;
        }

        return false;
    }

    public static ItemStack getChestplateSlot(ClientPlayerEntity player) {
        return player.getEquippedStack(EquipmentSlot.CHEST);
    }

    public static boolean equipChestplate(ClientPlayerEntity player) {
        int chestplateIndex = getLastChestplateIndex(player);
        if(chestplateIndex != -100) {
            swapChestplateSlot(chestplateIndex, player);
            lastUID = -100;
            autoSwapIsActive = false;
            return true;
        }

        return false;
    }

    private static void swapChestplateSlot(int slot, ClientPlayerEntity player) {
        var interactionManager = MinecraftClient.getInstance().interactionManager;
        assert interactionManager != null;
        interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, player);
        interactionManager.clickSlot(0, CHESTPLATE_INDEX, 0, SlotActionType.PICKUP, player);
        interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, player);
        player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
    }

    private static int getItemUID(ItemStack stack) {
        if (stack.isEmpty()) return -100;
        return stack.getName().hashCode() + stack.getEnchantments().hashCode() + stack.getDamage();
    }

    private static int getLastChestplateIndex(ClientPlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        if (inv == null) return -100;

        for (int slot : slotArray()) {
            ItemStack stack = inv.getStack(slot);
            if (getItemUID(stack) == lastUID) {
                return DataSlotToNetworkSlot(slot);
            }
        }
        return -100;
    }

    public static int getElytraIndex(PlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        if (inv == null) return -100;

        var world = player.getWorld();

        int bestSlot = -100;
        ItemStack bestItemStack = null;
        int bestPriority = Integer.MAX_VALUE;

        for (int slot : slotArray()) {
            ItemStack stack = inv.getStack(slot);
            if (!stack.isOf(Items.ELYTRA) || stack.getDamage() >= (stack.getMaxDamage() - ModConfig.INSTANCE.elytraReplaceDurability)) {
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

            if (priority < bestPriority || (priority == bestPriority && stack.getDamage() > bestItemStack.getDamage())) {
                bestSlot = slot;
                bestItemStack = stack;
                bestPriority = priority;
            }
        }

        return DataSlotToNetworkSlot(bestSlot);
    }

    private static boolean elytraHasMending(ItemStack elytra, World world) {
        var registry = world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT);
        if(registry.isEmpty() || registry.get().getEntry(Enchantments.MENDING.getValue()).isEmpty())
            return false;

        int res = EnchantmentHelper.getLevel(registry.get().getEntry(Enchantments.MENDING.getValue()).get(), elytra);

        return res > 0;
    }

    private static int elytraGetUnbreakingLevel(ItemStack elytra, World world) {
        var registry = world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT);
        if(registry.isEmpty() || registry.get().getEntry(Enchantments.UNBREAKING.getValue()).isEmpty())
            return 0;

        return EnchantmentHelper.getLevel(registry.get().getEntry(Enchantments.UNBREAKING.getValue()).get(), elytra);
    }

    public static int DataSlotToNetworkSlot(int index) {
        if(index == 100)
            index = 8;
        else if(index == 101)
            index = 7;
        else if(index == 102)
            index = 6;
        else if(index == 103)
            index = 5;
        else if(index == -106 || index == 40)
            index = 45;
        else if(index <= 8 && index != -100)
            index += 36;
        else if(index >= 80 && index <= 83)
            index -= 79;
        return index;
    }

    public static int[] slotArray() {
        int[] range = new int[37];
        for (int i = 0; i < 9; i++) range[i] = 8 - i;
        for (int i = 9; i < 36; i++) range[i] = 35 - (i - 9);
        range[36] = 40;
        return range;
    }
}
