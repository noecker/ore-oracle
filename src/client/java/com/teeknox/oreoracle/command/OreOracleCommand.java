package com.teeknox.oreoracle.command;

import com.mojang.brigadier.CommandDispatcher;
import com.teeknox.oreoracle.config.ModConfig;
import com.teeknox.oreoracle.gui.OreOracleOverlay;
import com.teeknox.oreoracle.gui.OreSelectorScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;

/**
 * Client-side commands for Ore Oracle.
 * - /oreoracle or /oo - Opens the ore selector screen
 * - /oreoracle toggle - Toggles HUD visibility
 */
public class OreOracleCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher,
                                 CommandRegistryAccess registryAccess) {
        // Main command: /oreoracle
        dispatcher.register(
                ClientCommandManager.literal("oreoracle")
                        .executes(context -> openSelector())
                        .then(ClientCommandManager.literal("toggle")
                                .executes(context -> toggleHud(context.getSource())))
        );

        // Alias: /oo
        dispatcher.register(
                ClientCommandManager.literal("oo")
                        .executes(context -> openSelector())
                        .then(ClientCommandManager.literal("toggle")
                                .executes(context -> toggleHud(context.getSource())))
        );
    }

    private static int openSelector() {
        MinecraftClient client = MinecraftClient.getInstance();
        // Schedule for next tick to avoid rendering issues
        client.execute(() -> {
            client.setScreen(new OreSelectorScreen(null));
        });
        return 1;
    }

    private static int toggleHud(FabricClientCommandSource source) {
        ModConfig config = ModConfig.getInstance();
        config.setEnabled(!config.isEnabled());
        config.save();

        OreOracleOverlay.getInstance().invalidateCache();

        String messageKey = config.isEnabled() ? "oreoracle.command.hud.enabled" : "oreoracle.command.hud.disabled";
        source.sendFeedback(Text.translatable(messageKey));

        return 1;
    }
}
