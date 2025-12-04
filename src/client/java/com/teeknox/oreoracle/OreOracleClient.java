package com.teeknox.oreoracle;

import com.teeknox.oreoracle.command.OreOracleCommand;
import com.teeknox.oreoracle.config.ModConfig;
import com.teeknox.oreoracle.config.ServerDataManager;
import com.teeknox.oreoracle.gui.OreOracleOverlay;
import com.teeknox.oreoracle.gui.OreSelectorScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side mod initializer for Ore Oracle.
 * Sets up HUD overlay, commands, and keybindings.
 */
public class OreOracleClient implements ClientModInitializer {
    private static final KeyBinding.Category KEYBIND_CATEGORY = KeyBinding.Category.MISC;

    // Keybindings (unbound by default per spec)
    private static KeyBinding openSelectorKey;
    private static KeyBinding toggleHudKey;

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

        // Register HUD overlay
        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            OreOracleOverlay.getInstance().render(context, tickCounter.getTickProgress(true));
        });

        // Register commands
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            OreOracleCommand.register(dispatcher, registryAccess);
        });

        // Register keybindings (unbound by default)
        openSelectorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.oreoracle.open_selector",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KEYBIND_CATEGORY
        ));

        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.oreoracle.toggle_hud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KEYBIND_CATEGORY
        ));

        // Handle keybind presses
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openSelectorKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new OreSelectorScreen(null));
                }
            }

            while (toggleHudKey.wasPressed()) {
                ModConfig config = ModConfig.getInstance();
                config.setEnabled(!config.isEnabled());
                config.save();
                OreOracleOverlay.getInstance().invalidateCache();
            }
        });
    }
}
