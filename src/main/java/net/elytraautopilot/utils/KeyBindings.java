package net.elytraautopilot.utils;

import com.mojang.blaze3d.platform.InputConstants;

import net.elytraautopilot.ElytraAutoPilot;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class KeyBindings {

    public static KeyMapping configBinding;
    public static KeyMapping landBinding;
    public static KeyMapping takeoffBinding;

    private static boolean configPressed;
    private static boolean landPressed;
    private static boolean takeoffPressed;

    private static KeyMapping.Category CATEGORY;

    /**
     * Called from KeyboardHandlerMixin / MouseHandlerMixin for genuine (non-repeat)
     * key or mouse button presses that reach game input.
     */
    public static void onKeyPressed(InputConstants.Key key) {
        if (configBinding == null)
            return;
        if (configBinding.matches(key))
            configPressed = true;
        if (landBinding.matches(key))
            landPressed = true;
        if (takeoffBinding.matches(key))
            takeoffPressed = true;
    }

    public static boolean consumeConfigPress() {
        boolean pressed = configPressed;
        configPressed = false;
        return pressed;
    }

    public static boolean consumeLandPress() {
        boolean pressed = landPressed;
        landPressed = false;
        return pressed;
    }

    public static boolean consumeTakeoffPress() {
        boolean pressed = takeoffPressed;
        takeoffPressed = false;
        return pressed;
    }

    public static void init() {
        String modid = ElytraAutoPilot.getModId();

        if (CATEGORY == null) {
            CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(modid, "flight"));
        }

        final String key = FabricLoader.getInstance().isModLoaded("cloth-config")
                ? "key." + modid + ".toggle"
                : "key." + modid + ".toggle_no_cloth";

        landBinding = new KeyMapping("key." + modid + ".land", InputConstants.Type.KEYBOARD, InputConstants.KEY_RSHIFT,
                CATEGORY);

        takeoffBinding = new KeyMapping("key." + modid + ".takeoff", InputConstants.Type.KEYBOARD,
                InputConstants.KEY_RALT, CATEGORY);

        configBinding = new KeyMapping(key, InputConstants.Type.KEYBOARD, InputConstants.KEY_R, CATEGORY);

        KeyMappingHelper.registerKeyMapping(configBinding);
        KeyMappingHelper.registerKeyMapping(landBinding);
        KeyMappingHelper.registerKeyMapping(takeoffBinding);

    }
}
