package com.teeknox.modtemplate.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Example mixin for intercepting chat/system messages.
 *
 * Mixins allow you to inject code into Minecraft classes at compile time.
 * This is useful for:
 * - Intercepting network packets
 * - Modifying rendering behavior
 * - Hooking into game events not covered by Fabric API
 *
 * Requirements:
 * - Add mixin config to fabric.mod.json: "mixins": ["mod-template.client.mixins.json"]
 * - Create mixin config file in src/client/resources/
 * - List mixin classes in the config
 *
 * Priority (100-1000):
 * - Lower numbers run first
 * - Default is 1000
 * - Use priority 100 to run before other mods
 */
@Mixin(value = ClientPlayNetworkHandler.class, priority = 1000)
public class ExampleMixin {

    /**
     * Intercept game/system messages before they're processed.
     *
     * @At("HEAD") - inject at the start of the method
     * @At("RETURN") - inject at the end of the method
     * @At("INVOKE") - inject before/after a specific method call
     */
    @Inject(method = "onGameMessage", at = @At("HEAD"))
    private void onGameMessageHead(GameMessageS2CPacket packet, CallbackInfo ci) {
        Text content = packet.content();
        String message = content.getString();

        // Example: Log all system messages
        // ModTemplateMod.LOGGER.debug("System message: {}", message);

        // To cancel the original method, use:
        // ci.cancel();

        // To modify parameters, use @Redirect or @ModifyVariable instead
    }
}
