package com.teeknox.modtemplate.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Mod Menu integration for config screen access.
 *
 * This class is referenced in fabric.mod.json as the "modmenu" entrypoint.
 * It provides a factory that creates the config screen when the user clicks
 * the config button in Mod Menu.
 *
 * Requirements:
 * - Add modmenu dependency in build.gradle (modCompileOnly or modImplementation)
 * - Add "modmenu" entrypoint in fabric.mod.json
 * - Add "modmenu": "*" to "suggests" in fabric.mod.json for optional dependency
 */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::new;
    }
}
