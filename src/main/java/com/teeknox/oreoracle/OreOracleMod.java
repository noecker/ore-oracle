package com.teeknox.oreoracle;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main mod initializer for common/server-side code.
 * This runs on both client and dedicated server.
 *
 * Ore Oracle is a client-only mod, so this initializer is minimal.
 * All functionality is in OreOracleClient.
 */
public class OreOracleMod implements ModInitializer {
    public static final String MOD_ID = "ore-oracle";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing {} mod", MOD_ID);
    }
}
