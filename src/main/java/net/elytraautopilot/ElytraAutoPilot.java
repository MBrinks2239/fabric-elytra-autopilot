package net.elytraautopilot;

import com.terraformersmc.modmenu.ModMenu;
import net.elytraautopilot.commands.ClientCommands;
import net.elytraautopilot.config.ModConfig;
import net.elytraautopilot.utils.Hud;
import net.elytraautopilot.utils.KeyBindings;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElytraAutoPilot implements ClientModInitializer {
    public static final String MODID = "elytraautopilot";
    public static final Logger LOGGER = LoggerFactory.getLogger("ElytraAutoPilot");
    private static boolean configPressed = false;
    private static boolean landPressed = false;
    private static boolean takeoffPressed = false;
    public static MinecraftClient minecraftClient;
    public static boolean calculateHud;
    public static boolean autoFlight;
    private static final int TAKEOFF_COOLDOWN_TICKS = 5;
    private static int takeoffCooldown = 0;
    private static boolean onTakeoff;
    public static double pitchMod = 1f;

    public static Vec3d previousPosition;
    public static double currentVelocity;
    public static double currentVelocityHorizontal;

    public static boolean isDescending;
    public static boolean pullUp;
    public static boolean pullDown;

    private static double velHigh = 0f;
    private static double velLow = 0f;

    public static int argXpos;
    public static int argZpos;
    public static boolean isChained = false;
    public static boolean isflytoActive = false;
    public static boolean forceLand = false;
    public static boolean isLanding = false;
    public static float GLIDE_ANGLE = 0.0f;
    public static boolean doGlide = false;
    public static double distance = 0f;
    public static double groundheight;

    @Override
	public void onInitializeClient() {
        minecraftClient = MinecraftClient.getInstance();

        KeyBindings.init();
        HudRenderCallback.EVENT.register((matrixStack, tickDelta) -> ElytraAutoPilot.this.onScreenTick());
        ClientTickEvents.END_CLIENT_TICK.register(e -> this.onClientTick());

        ClientCommands.register(minecraftClient);
    }

    public static String getModId() {
        return MODID;
    }
	public static void takeoff()
    {
        PlayerEntity player = minecraftClient.player;
        if (!onTakeoff) {
            if (player != null) {
                if (ModConfig.INSTANCE.elytraAutoSwap) {
                    int elytraSlot = getElytraIndex(player);
                    if (elytraSlot == -1) {
                        player.sendMessage(Text.translatable("text." + MODID + ".takeoffFail.noElytraInInventory").formatted(Formatting.RED), true);
                        return;
                    }
                } else {
                    Item itemChest = player.getInventory().armor.get(2).getItem();
                    if (itemChest != Items.ELYTRA) {
                        player.sendMessage(Text.translatable("text." + MODID + ".takeoffFail.noElytraEquipped").formatted(Formatting.RED), true);
                        return;
                    }
                    int elytraDamage = player.getInventory().armor.get(2).getMaxDamage() - player.getInventory().armor.get(2).getDamage();
                    if (elytraDamage == 1) {
                        player.sendMessage(Text.translatable("text." + MODID + ".takeoffFail.elytraBroken").formatted(Formatting.RED), true);
                        return;
                    }
                }
                Item itemMain = player.getMainHandStack().getItem();
                Item itemOff = player.getOffHandStack().getItem();
                Item itemChest = player.getInventory().armor.get(2).getItem();
                int elytraDamage = player.getInventory().armor.get(2).getMaxDamage() - player.getInventory().armor.get(2).getDamage();
                if (itemChest != Items.ELYTRA) {
                    player.sendMessage(Text.translatable("text.elytraautopilot.takeoffFail.noElytraEquipped").formatted(Formatting.RED), true);
                    return;
                }
                if (elytraDamage == 1) {
                    player.sendMessage(Text.translatable("text.elytraautopilot.takeoffFail.elytraBroken").formatted(Formatting.RED), true);
                    return;
                }
                if (itemMain != Items.FIREWORK_ROCKET && itemOff != Items.FIREWORK_ROCKET ) {
                    player.sendMessage(Text.translatable("text.elytraautopilot.takeoffFail.fireworkRequired").formatted(Formatting.RED), true);
                    return;
                }

                World world = player.getWorld();
                Vec3d clientPos = player.getPos();
                int l = world.getTopYInclusive();
                int n = 2;
                double c = clientPos.getY();
                for (double i = c; i < l; i++) {
                    BlockPos blockPos = BlockPos.ofFloored(clientPos.getX(), clientPos.getY() + n, clientPos.getZ());
                    if (!world.getBlockState(blockPos).isAir()) {
                        player.sendMessage(Text.translatable("text.elytraautopilot.takeoffFail.clearSkyNeeded").formatted(Formatting.RED), true);
                        return;
                    }
                    n++;
                }
                takeoffCooldown = TAKEOFF_COOLDOWN_TICKS;
                minecraftClient.options.jumpKey.setPressed(true);
            }
            return;
        }
        if (player != null) {
            if (groundheight > ModConfig.INSTANCE.minHeight) {
                onTakeoff = false;
                minecraftClient.options.useKey.setPressed(false);
                minecraftClient.options.jumpKey.setPressed(false);
                autoFlight = true;
                pitchMod = 3f;
                if (isChained) {
                    isflytoActive = true;
                    isChained = false;
                    minecraftClient.inGameHud.getChatHud().addMessage(Text.translatable("text.elytraautopilot.flyto", argXpos, argZpos).formatted(Formatting.GREEN));
                }
                return;
            }
            if (!player.isGliding()) minecraftClient.options.jumpKey.setPressed(!minecraftClient.options.jumpKey.isPressed());
            Item itemMain = player.getMainHandStack().getItem();
            Item itemOff = player.getOffHandStack().getItem();
            boolean hasFirework = (itemMain == Items.FIREWORK_ROCKET  || itemOff == Items.FIREWORK_ROCKET);
            if (!hasFirework) {
                if (!tryRestockFirework(player)) {
                    minecraftClient.options.useKey.setPressed(false);
                    minecraftClient.options.jumpKey.setPressed(false);
                    onTakeoff = false;
                    player.sendMessage(Text.translatable("text.elytraautopilot.takeoffAbort.noFirework").formatted(Formatting.RED), true);
                    doGlide = true;
                }
            }
            else minecraftClient.options.useKey.setPressed(currentVelocity < 0.75f && player.getPitch() == -90f);
        }
    }
	private void onScreenTick() //Once every screen frame
    {
        // Stops logic when paused.
        if (minecraftClient.isPaused()) {
            doGlide = false;
            if (minecraftClient.isInSingleplayer()) return;
        }

        // Player is null when it isn't currently in a world. Optimization spot here.
        PlayerEntity player = minecraftClient.player;
        if (player == null) return;

        //Fps adaptation (not perfect but works nicely most of the time)
        float fps_delta = minecraftClient.getRenderTickCounter().getLastFrameDuration();
        float fps_result = 20/fps_delta;
        double speedMod = 60/fps_result; //Adapt to base 60 FPS

        // Calculate hard coded flight modes based on pitch.
        float pitch = player.getPitch();
//        if (doGlide) {
//            if (pitch < GLIDE_ANGLE) {
//                player.setPitch((float) (pitch + ModConfig.INSTANCE.pullDownSpeed*speedMod*3));
//                pitch = player.getPitch();
//                if (pitch >= GLIDE_ANGLE) {
//                    player.setPitch(GLIDE_ANGLE);
//                    doGlide = false;
//                }
//            }
//            else if (pitch > GLIDE_ANGLE){
//                player.setPitch((float) (pitch - ModConfig.INSTANCE.pullDownSpeed*speedMod)*3);
//                pitch = player.getPitch();
//                if (pitch <= GLIDE_ANGLE) {
//                    player.setPitch(GLIDE_ANGLE);
//                    doGlide = false;
//                }
//            }
//        }
        if (onTakeoff) {
            if (pitch > -90f) {
                player.setPitch((float) (pitch - ModConfig.INSTANCE.takeOffPull*speedMod));
                pitch = player.getPitch();
            }
            if (pitch <= -90f) player.setPitch(-90f); // Very stiff and unnatural movement
        }
        if (autoFlight) {
            // Flyto behavior
            if (isflytoActive || forceLand) {
                if (isLanding || forceLand) {
                    if (!forceLand && !ModConfig.INSTANCE.autoLanding){
                        isflytoActive = false;
                        isLanding = false;
                        return;
                    }
                    isDescending = true;
                    if (ModConfig.INSTANCE.riskyLanding && groundheight > 60) {
                        riskyLanding(player, speedMod);
                    }
                    else {
                        smoothLanding(player, speedMod);
                    }
                }
                else {
                    Vec3d playerPosition = player.getPos();
                    double f = (double) argXpos - playerPosition.x;
                    double d = (double) argZpos - playerPosition.z;
                    float targetYaw = MathHelper.wrapDegrees((float) (MathHelper.atan2(d, f) * 57.2957763671875D) - 90.0F);
                    float yaw = MathHelper.wrapDegrees(player.getYaw());
                    if (Math.abs(yaw-targetYaw) < ModConfig.INSTANCE.turningSpeed*2*speedMod) player.setYaw(targetYaw);
                    else {
                        if (yaw < targetYaw) player.setYaw((float) (yaw + ModConfig.INSTANCE.turningSpeed*speedMod));
                        if (yaw > targetYaw) player.setYaw((float) (yaw - ModConfig.INSTANCE.turningSpeed*speedMod));
                    }
                    distance = Math.sqrt(f * f + d * d);
                    if (distance < 20) {
                        minecraftClient.player.sendMessage(Text.translatable("text.elytraautopilot.landing").formatted(Formatting.BLUE), true);
                        SoundEvent soundEvent = SoundEvent.of(Identifier.of(ModConfig.INSTANCE.playSoundOnLanding));
                        player.playSound(soundEvent, 1.3f, 1f);
                        isLanding = true;
                    }
                }
            }
            // Flight pitch behavior
            if (pullUp && !(isLanding || forceLand)) {
                player.setPitch((float) (pitch - ModConfig.INSTANCE.pullUpSpeed*speedMod));
                pitch = player.getPitch();
                if (pitch <= ModConfig.INSTANCE.pullUpAngle) {
                    player.setPitch((float) ModConfig.INSTANCE.pullUpAngle);
                }
                // Powered flight behavior
                minecraftClient.options.useKey.setPressed(ModConfig.INSTANCE.poweredFlight && currentVelocity < 1.25f);
            }
            if (pullDown && !(isLanding || forceLand)) {
                player.setPitch((float) (pitch + ModConfig.INSTANCE.pullDownSpeed*pitchMod*speedMod));
                pitch = player.getPitch();
                if (pitch >= ModConfig.INSTANCE.pullDownAngle) {
                    player.setPitch((float) ModConfig.INSTANCE.pullDownAngle);
                }
                // Powered flight behavior
                minecraftClient.options.useKey.setPressed(ModConfig.INSTANCE.poweredFlight && currentVelocity < 1.25f);
            }
        }
        else
        {
            velHigh = 0f;
            velLow = 0f;
        	isLanding = false;
            forceLand = false;
            isflytoActive = false;
            pullUp = false;
            pitchMod = 1f;
            pullDown = false;
        }
    }

    private void onClientTick() //20 times a second, before first screen tick
    {
        if (!(minecraftClient.isPaused() && minecraftClient.isInSingleplayer())) Hud.tick();
        double velMod;

        if (ClientCommands.bufferSave) {
            ModConfig.INSTANCE.saveConfig(ModConfig.CONFIG_FILE.toFile());
            ClientCommands.bufferSave = false;
        }

        PlayerEntity player = minecraftClient.player;

        if (player == null){
            autoFlight = false;
            onTakeoff = false;
            return;
        }

        if (player.isGliding())
            calculateHud = true;
        else {
            calculateHud = false;
            autoFlight = false;
            groundheight = -1f;
        }

        double altitude;
        if (autoFlight) {
            // Elytra hotswap
            if (!tryRestockElytra(player) && ModConfig.INSTANCE.emergencyLand) {
                forceLand = true;
            }

            altitude = player.getPos().y;

            if (player.isTouchingWater() || player.isInLava()) {
                isflytoActive = false;
                isLanding = false;
                autoFlight = false;
                return;
            }

            if (isDescending)
            {
                pullUp = false;
                pullDown = true;
                if (altitude > ModConfig.INSTANCE.maxHeight) { //TODO fix inconsistent height behavior
                    velHigh = 0.3f;
                }
                else if (altitude > ModConfig.INSTANCE.maxHeight-10) {
                    velLow = 0.28475f;
                }
                velMod = Math.max(velHigh, velLow);
                if (currentVelocity >= ModConfig.INSTANCE.pullDownMaxVelocity + velMod) {
                    isDescending = false;
                    pullDown = false;
                    pullUp = true;
                    pitchMod = 1f;
                }
            }
            else
            {
                velHigh = 0f;
                velLow = 0f;
                pullUp = true;
                pullDown = false;
                if (currentVelocity <= ModConfig.INSTANCE.pullUpMinVelocity || altitude > ModConfig.INSTANCE.maxHeight-10) {
                    isDescending = true;
                    pullDown = true;
                    pullUp = false;
                }
            }
        }
        if (!takeoffPressed && KeyBindings.takeoffBinding.isPressed()) {
            if (onTakeoff) {
                onTakeoff = false;
                minecraftClient.options.useKey.setPressed(false);
                minecraftClient.options.jumpKey.setPressed(false);
                doGlide = true;
            }
            else {
                takeoff();
            }
        }

        if (!landPressed && KeyBindings.landBinding.isPressed() && autoFlight) {
            player.sendMessage(Text.translatable("text.elytraautopilot.landing").formatted(Formatting.BLUE), true);
            SoundEvent soundEvent = SoundEvent.of(Identifier.of(ModConfig.INSTANCE.playSoundOnLanding));
            player.playSound(soundEvent, 1.3f, 1f);
            minecraftClient.options.useKey.setPressed(false);
            forceLand = true;
        }

        if(!configPressed && KeyBindings.configBinding.isPressed()) {
            if (player.isGliding()) {
                if (!autoFlight && groundheight < ModConfig.INSTANCE.minHeight){
                    player.sendMessage(Text.translatable("text.elytraautopilot.autoFlightFail.tooLow").formatted(Formatting.RED), true);
                    doGlide = true;
                }
                else {
                    // If the player is flying an elytra, we start the auto flight
                    autoFlight = !autoFlight;
                    minecraftClient.options.useKey.setPressed(false);
                    if (autoFlight){
                        isDescending = true;
                        pitchMod = 3f;
                    }
                }
            }
            else {
                // Otherwise, we open the settings if cloth is loaded
                if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
                    Screen configScreen = ModMenu.getConfigScreen(MODID, minecraftClient.currentScreen);
                    minecraftClient.setScreen(configScreen);
                }
            }
        }
        configPressed = KeyBindings.configBinding.isPressed();
        landPressed = KeyBindings.landBinding.isPressed();
        takeoffPressed = KeyBindings.takeoffBinding.isPressed();

	    if (takeoffCooldown > 0) {
           if (--takeoffCooldown == 0) onTakeoff = true;
        }

	    if (onTakeoff) {
            takeoff();
        }

        if (calculateHud) {
            computeVelocity();
            Hud.drawHud(player);
        }
        else {
            previousPosition = null;
            Hud.clearHud();
        }
    }

    private static boolean tryRestockFirework(PlayerEntity player) {
        if(ModConfig.INSTANCE.fireworkHotswap) {
            ItemStack newFirework = null;
            for (ItemStack itemStack : player.getInventory().main) {
                if (itemStack.getItem() == Items.FIREWORK_ROCKET ) {
                    newFirework = itemStack;
                    break;
                }
            }
            if (newFirework != null) {
                int handSlot;
                if (player.getOffHandStack().isEmpty()){
                    handSlot = 45; //Offhand slot refill
                }
                else{
                    handSlot = 36 + player.getInventory().selectedSlot; //Mainhand slot refill
                }

                assert minecraftClient.interactionManager != null;
                minecraftClient.interactionManager.clickSlot(
                        player.playerScreenHandler.syncId,
                        handSlot,
                        player.getInventory().main.indexOf(newFirework),
                        SlotActionType.SWAP,
                        player
                );
                return true;
            }
        }
        return false;
    }

    private static boolean tryRestockElytra(PlayerEntity player) {
        int elytraDurability = player.getInventory().armor.get(2).getMaxDamage() - player.getInventory().armor.get(2).getDamage();
        if (ModConfig.INSTANCE.elytraHotswap) {
            if (elytraDurability <= 5) { // Leave some leeway, so we don't stop flying
                // Optimization: find the first elytra with sufficient durability
                ItemStack newElytra = null;
                int minDurability = 10;
                for (ItemStack itemStack : player.getInventory().main) {
                    if (itemStack.getItem() == Items.ELYTRA) {
                        int itemDurability = itemStack.getMaxDamage() - itemStack.getDamage();
                        if (itemDurability >= minDurability) {
                            newElytra = itemStack;
                            break;
                        }
                    }
                }
                if (newElytra != null) {
                    int chestSlot = 6;
                    assert minecraftClient.interactionManager != null;
                    minecraftClient.interactionManager.clickSlot(
                            player.playerScreenHandler.syncId,
                            chestSlot,
                            player.getInventory().main.indexOf(newElytra),
                            SlotActionType.SWAP,
                            player
                    );
                    player.playSound(SoundEvents.ITEM_ARMOR_EQUIP_ELYTRA.value(), 1.0F, 1.0F);
                    player.sendMessage(Text.translatable("text.elytraautopilot.swappedElytra").formatted(Formatting.GREEN), true);
                }
                else {
                    return false;
                }
            }
        }
        else return elytraDurability > 30;
        return true;
    }

    private void computeVelocity()
    {
        Vec3d newPosition;
        PlayerEntity player = minecraftClient.player;
        if (player != null && !(minecraftClient.isPaused() && minecraftClient.isInSingleplayer())) {
            newPosition = player.getPos();
            if (previousPosition == null)
                previousPosition = newPosition;

            Vec3d difference = new Vec3d(newPosition.x - previousPosition.x, newPosition.y - previousPosition.y, newPosition.z - previousPosition.z);
            Vec3d difference_horizontal = new Vec3d(newPosition.x - previousPosition.x, 0, newPosition.z - previousPosition.z);
            previousPosition = newPosition;

            currentVelocity = difference.length();
            currentVelocityHorizontal = difference_horizontal.length();
        }
    }

    private void smoothLanding(PlayerEntity player, double speedMod)
    {
        float yaw = MathHelper.wrapDegrees(player.getYaw());
        float pitch = MathHelper.wrapDegrees(player.getPitch());
        float fallPitchMax = 50f;
        float fallPitchMin = 30f;
        float fallPitch;
        if (groundheight > 50){
            fallPitch = fallPitchMax;
        }
        else if (groundheight < 20){
            fallPitch = fallPitchMin;
        }
        else {
            fallPitch = (float) ((groundheight-20)/30)*20 + fallPitchMin;
        }
        pitchMod = 3f;
        player.setYaw((float) (yaw + ModConfig.INSTANCE.autoLandSpeed*speedMod));
        player.setPitch((float) (pitch + ModConfig.INSTANCE.pullDownSpeed*pitchMod*speedMod));
        pitch = player.getPitch();
        if (pitch >= fallPitch) {
            player.setPitch(fallPitch);
        }
    }

    private void riskyLanding(PlayerEntity player, double speedMod)
    {
        float pitch = player.getPitch();
        player.setPitch((float) (pitch + ModConfig.INSTANCE.takeOffPull*speedMod));
        pitch = player.getPitch();
        if (pitch > 90f) player.setPitch(90f);
    }

    /**
     * @param player The player
     * @return the first index of an elytra in the specified player's inventory
     */
    public static int getElytraIndex(PlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        if (inv == null) return -1;

        var world = player.getWorld();

        int bestSlot = -1;
        ItemStack bestItemStack = null;
        int bestPriority = Integer.MAX_VALUE;

        for (int slot : slotArray()) {
            ItemStack stack = inv.getStack(slot);
            if (!stack.isOf(Items.ELYTRA) || stack.getDamage() >= stack.getMaxDamage() - 1) {
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
        else if(index <= 8)
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
