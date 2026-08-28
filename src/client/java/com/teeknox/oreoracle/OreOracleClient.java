package com.teeknox.oreoracle;

import com.mojang.blaze3d.platform.InputConstants;
import com.teeknox.oreoracle.command.OreOracleCommand;
import com.teeknox.oreoracle.config.ModConfig;
import com.teeknox.oreoracle.config.ServerDataManager;
import com.teeknox.oreoracle.gui.OreOracleOverlay;
import com.teeknox.oreoracle.gui.OreSelectorScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side mod initializer for Ore Oracle.
 * Sets up HUD overlay, commands, and keybindings.
 */
public class OreOracleClient implements ClientModInitializer {
    private static final KeyMapping.Category KEYBIND_CATEGORY = KeyMapping.Category.MISC;

    // Keybindings (unbound by default per spec)
    private static KeyMapping openSelectorKey;
    private static KeyMapping toggleHudKey;

    @Override
    public void onInitializeClient() {
        OreOracleMod.LOGGER.info("Initializing {} client", OreOracleMod.MOD_ID);

        // Initialize config
        ModConfig.getInstance();

        // Register server join/leave events for per-server data
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ServerDataManager.getInstance().loadForCurrentServer();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ServerDataManager.getInstance().clear();
        });

        // Save config on client stop
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            ModConfig.getInstance().save();
        });

        // Register HUD overlay only if MC Widgets is not installed
        // When MC Widgets is present, it discovers and renders our widget via OreOracleWidgetProvider
        if (!FabricLoader.getInstance().isModLoaded("mc-widgets")) {
            // Using addLast() to render after vanilla HUD elements and avoid render condition inheritance
            // This fixes the issue where pinned F3 elements would hide the overlay
            HudElementRegistry.addLast(
                    Identifier.fromNamespaceAndPath(OreOracleMod.MOD_ID, "overlay"),
                    (context, tickCounter) -> {
                        OreOracleOverlay.getInstance().render(context, tickCounter.getGameTimeDeltaPartialTick(true));
                    }
            );
        }

        // Register commands
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            OreOracleCommand.register(dispatcher, registryAccess);
        });

        // Register keybindings (unbound by default)
        openSelectorKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.oreoracle.open_selector",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KEYBIND_CATEGORY
        ));

        toggleHudKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.oreoracle.toggle_hud",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KEYBIND_CATEGORY
        ));

        // Handle keybind presses
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openSelectorKey.consumeClick()) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(new OreSelectorScreen(null));
                }
            }

            while (toggleHudKey.consumeClick()) {
                ModConfig config = ModConfig.getInstance();
                config.setEnabled(!config.isEnabled());
                config.save();
                OreOracleOverlay.getInstance().invalidateCache();
            }
        });
    }
}
