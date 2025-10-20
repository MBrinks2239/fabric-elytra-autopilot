package net.elytraautopilot.utils;

import net.elytraautopilot.ElytraAutoPilot;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier; // <-- make sure this import exists
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static KeyBinding configBinding;
    public static KeyBinding landBinding;
    public static KeyBinding takeoffBinding;

    private static KeyBinding.Category CATEGORY;

    public static void init() {
        String modid = ElytraAutoPilot.getModId();

        if (CATEGORY == null) {
            CATEGORY = KeyBinding.Category.create(Identifier.of(modid, "flight"));
        }

        final String key = FabricLoader.getInstance().isModLoaded("cloth-config")
                ? "key." + modid + ".toggle"
                : "key." + modid + ".toggle_no_cloth";

        landBinding = new KeyBinding(
                "key." + modid + ".land",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                CATEGORY
        );

        takeoffBinding = new KeyBinding(
                "key." + modid + ".takeoff",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_ALT,
                CATEGORY
        );

        configBinding = new KeyBinding(
                key,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                CATEGORY
        );

        KeyBindingHelper.registerKeyBinding(configBinding);
        KeyBindingHelper.registerKeyBinding(landBinding);
        KeyBindingHelper.registerKeyBinding(takeoffBinding);
    }
}
