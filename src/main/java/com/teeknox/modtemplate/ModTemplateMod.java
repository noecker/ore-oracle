package com.teeknox.modtemplate;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main mod initializer for common/server-side code.
 * This runs on both client and dedicated server.
 *
 * Use this for:
 * - Registering blocks, items, entities
 * - Server-side commands
 * - Game rules
 * - Anything that needs to run on dedicated servers
 *
 * For client-only code (rendering, keybindings, HUD), use ModTemplateClient instead.
 */
public class ModTemplateMod implements ModInitializer {
    public static final String MOD_ID = "mod-template";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing {} mod", MOD_ID);

        // TODO: Register server-side components here
        // - Blocks: Registry.register(Registries.BLOCK, ...)
        // - Items: Registry.register(Registries.ITEM, ...)
        // - Commands: CommandRegistrationCallback.EVENT.register(...)
    }
}
