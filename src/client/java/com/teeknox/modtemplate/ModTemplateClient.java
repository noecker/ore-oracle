package com.teeknox.modtemplate;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side mod initializer.
 * This runs only on the client, never on dedicated servers.
 *
 * Use this for:
 * - Keybindings
 * - HUD/overlay rendering
 * - Client-side screens and GUIs
 * - Client tick events
 * - Mod Menu integration
 */
public class ModTemplateClient implements ClientModInitializer {
    public static final String MOD_ID = "mod-template";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Create a custom keybinding category for this mod
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(
            Identifier.of(MOD_ID, "category")
    );

    // Example keybinding - unbound by default (GLFW_KEY_UNKNOWN)
    private static KeyBinding exampleKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing {} client", MOD_ID);

        // Register keybindings
        exampleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mod-template.example",      // Translation key
                InputUtil.Type.KEYSYM,           // Input type (keyboard)
                GLFW.GLFW_KEY_UNKNOWN,           // Default key (unbound)
                CATEGORY                          // Keybinding category
        ));

        // Register client tick handler for keybinding processing
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Process keybindings only when no screen is open
            while (exampleKey.wasPressed()) {
                if (client.currentScreen == null) {
                    LOGGER.info("Example key pressed!");
                    // TODO: Handle keybinding action
                }
            }
        });

        // Register HUD render callback for overlay rendering
        HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            // Only render when no screen is open (optional)
            if (client.currentScreen == null) {
                // TODO: Render overlay here
                // Example: drawContext.drawText(client.textRenderer, "Hello!", 10, 10, 0xFFFFFF, true);
            }
        });

        LOGGER.info("{} client initialized successfully", MOD_ID);
    }

    // Getter for keybinding (useful for showing bound key in GUIs)
    public static KeyBinding getExampleKey() {
        return exampleKey;
    }
}
