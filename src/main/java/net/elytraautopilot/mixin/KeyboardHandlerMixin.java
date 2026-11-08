package net.elytraautopilot.mixin;

import com.mojang.blaze3d.platform.InputConstants;

import net.elytraautopilot.utils.KeyBindings;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    /**
     * Vanilla registers a key mapping click for both PRESS and REPEAT events once
     * the key reaches game input (no screen open). Only record genuine PRESS
     * events, so a key that was held down in a screen (chat, sign, anvil...) and
     * keeps auto-repeating after the screen closes does not trigger our bindings.
     */
    @Inject(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;click(Lcom/mojang/blaze3d/platform/InputConstants$Key;)V"))
    private void onKeyMappingClick(long handle, int action, KeyEvent event, CallbackInfo ci) {
        if (action == InputConstants.PRESS) {
            KeyBindings.onKeyPressed(InputConstants.getKey(event));
        }
    }
}
