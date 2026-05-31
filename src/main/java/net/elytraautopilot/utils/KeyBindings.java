package net.elytraautopilot.utils;

import com.mojang.blaze3d.platform.InputConstants;
import net.elytraautopilot.ElytraAutoPilot;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static KeyMapping configBinding;
    public static KeyMapping landBinding;
    public static KeyMapping takeoffBinding;

    private static KeyMapping.Category CATEGORY;

    public static void init() {
        String modid = ElytraAutoPilot.getModId();

        if (CATEGORY == null) {
            CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(modid, "flight"));
        }

        final String key = FabricLoader.getInstance().isModLoaded("cloth-config")
                ? "key." + modid + ".toggle"
                : "key." + modid + ".toggle_no_cloth";

        landBinding = new KeyMapping(
                "key." + modid + ".land",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                CATEGORY
        );

        takeoffBinding = new KeyMapping(
                "key." + modid + ".takeoff",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_ALT,
                CATEGORY
        );

        configBinding = new KeyMapping(
                key,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                CATEGORY
        );

        KeyMappingHelper.registerKeyMapping(configBinding);
        KeyMappingHelper.registerKeyMapping(landBinding);
        KeyMappingHelper.registerKeyMapping(takeoffBinding);
    }
}
