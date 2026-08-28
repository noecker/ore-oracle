package com.teeknox.oreoracle.config;

import com.teeknox.oreoracle.gui.OreOracleOverlay;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Configuration screen for Ore Oracle settings.
 * Follows Teeknox STYLE_GUIDE.md specifications.
 */
public class ConfigScreen extends Screen {
    private static final int SETTINGS_BG = 0x80000000;
    private static final int TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int TEXT_SECONDARY = 0xFF888888;
    private static final int TEXT_MUTED = 0xFF666666;

    private static final int COLUMN_WIDTH = 300;
    private static final int HEADER_HEIGHT = 52;
    private static final int FOOTER_HEIGHT = 50;
    private static final int ROW_HEIGHT = 24;
    private static final int LEFT_MARGIN = 20;

    private final Screen parent;
    private final ModConfig config;
    private final boolean mcWidgetsInstalled;

    // Widgets
    private CycleButton<Boolean> enabledButton;
    private CycleButton<Boolean> showHeaderButton;
    private CycleButton<ModConfig.HudPosition> hudPositionButton;
    private CycleButton<ModConfig.VerticalPosition> verticalPositionButton;
    private CycleButton<ModConfig.DisplayMode> displayModeButton;
    private MaxOresSlider maxOresSlider;

    public ConfigScreen(Screen parent) {
        super(Component.translatable("oreoracle.screen.config.title"));
        this.parent = parent;
        this.config = ModConfig.getInstance();
        this.mcWidgetsInstalled = FabricLoader.getInstance().isModLoaded("mc-widgets");
    }

    @Override
    protected void init() {
        int columnX = (this.width - COLUMN_WIDTH) / 2;
        int contentX = columnX + LEFT_MARGIN;
        int buttonWidth = COLUMN_WIDTH - LEFT_MARGIN * 2;

        int currentY = HEADER_HEIGHT;

        // Enabled toggle
        enabledButton = CycleButton.onOffBuilder(config.isEnabled())
                .create(contentX, currentY, buttonWidth, 20,
                        Component.translatable("oreoracle.config.enabled"),
                        (button, value) -> config.setEnabled(value));
        addRenderableWidget(enabledButton);
        currentY += ROW_HEIGHT;

        // Show header toggle
        showHeaderButton = CycleButton.onOffBuilder(config.isShowHudHeader())
                .create(contentX, currentY, buttonWidth, 20,
                        Component.translatable("oreoracle.config.showHeader"),
                        (button, value) -> config.setShowHudHeader(value));
        addRenderableWidget(showHeaderButton);
        currentY += ROW_HEIGHT;

        if (mcWidgetsInstalled) {
            // MC Widgets is installed - show explanation and button instead of position options
            // Space for the explanation text (rendered in render())
            currentY += ROW_HEIGHT;

            // Button to open MC Widgets config
            addRenderableWidget(Button.builder(
                            Component.translatable("oreoracle.config.openMcWidgets"),
                            btn -> openMcWidgetsConfig())
                    .bounds(contentX, currentY, buttonWidth, 20)
                    .build());
            currentY += ROW_HEIGHT;
        } else {
            // MC Widgets not installed - show normal position options
            // HUD horizontal position
            hudPositionButton = CycleButton.<ModConfig.HudPosition>builder(position ->
                            Component.translatable("oreoracle.config.position." + position.name().toLowerCase()),
                            config.getHudPosition())
                    .withValues(ModConfig.HudPosition.values())
                    .create(contentX, currentY, buttonWidth, 20,
                            Component.translatable("oreoracle.config.position"),
                            (button, value) -> config.setHudPosition(value));
            addRenderableWidget(hudPositionButton);
            currentY += ROW_HEIGHT;

            // HUD vertical position
            verticalPositionButton = CycleButton.<ModConfig.VerticalPosition>builder(position ->
                            Component.translatable("oreoracle.config.verticalPosition." + position.name().toLowerCase()),
                            config.getVerticalPosition())
                    .withValues(ModConfig.VerticalPosition.values())
                    .create(contentX, currentY, buttonWidth, 20,
                            Component.translatable("oreoracle.config.verticalPosition"),
                            (button, value) -> config.setVerticalPosition(value));
            addRenderableWidget(verticalPositionButton);
            currentY += ROW_HEIGHT;
        }

        // Display mode
        displayModeButton = CycleButton.<ModConfig.DisplayMode>builder(mode ->
                        Component.translatable("oreoracle.config.displayMode." + mode.name().toLowerCase()),
                        config.getDisplayMode())
                .withValues(ModConfig.DisplayMode.values())
                .create(contentX, currentY, buttonWidth, 20,
                        Component.translatable("oreoracle.config.displayMode"),
                        (button, value) -> config.setDisplayMode(value));
        addRenderableWidget(displayModeButton);
        currentY += ROW_HEIGHT;

        // Max visible ores slider
        maxOresSlider = new MaxOresSlider(contentX, currentY, buttonWidth, 20,
                config.getMaxVisibleOres());
        addRenderableWidget(maxOresSlider);
        currentY += ROW_HEIGHT;

        // Done button
        int buttonY = this.height - FOOTER_HEIGHT + 15;
        int doneButtonWidth = 80;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), btn -> onClose())
                .bounds((this.width - doneButtonWidth) / 2, buttonY, doneButtonWidth, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Dim the background
        context.fill(0, 0, this.width, this.height, SETTINGS_BG);

        // Centered title
        context.centeredText(this.font, this.title, this.width / 2, 16, TEXT_PRIMARY);

        // Subtitle
        Component subtitle = Component.translatable("oreoracle.screen.config.subtitle");
        context.centeredText(this.font, subtitle, this.width / 2, 30, TEXT_SECONDARY);

        // MC Widgets explanation text (if installed)
        if (mcWidgetsInstalled) {
            int explanationY = HEADER_HEIGHT + ROW_HEIGHT * 2 + 4;
            Component explanation = Component.translatable("oreoracle.config.mcWidgetsExplanation");
            context.centeredText(this.font, explanation, this.width / 2, explanationY, TEXT_MUTED);
        }

        // Render widgets
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    private void openMcWidgetsConfig() {
        if (this.minecraft == null) return;

        // Try to open MC Widgets config screen via reflection to avoid hard dependency
        try {
            Class<?> mcWidgetsConfigClass = Class.forName("com.teeknox.mcwidgets.config.McWidgetsConfigScreen");
            var constructor = mcWidgetsConfigClass.getConstructor(Screen.class);
            Screen mcWidgetsScreen = (Screen) constructor.newInstance(this);
            this.minecraft.gui.setScreen(mcWidgetsScreen);
        } catch (Exception e) {
            // Fallback: MC Widgets config screen not accessible
            // Could show a message, but for now just do nothing
        }
    }

    @Override
    public void onClose() {
        config.save();
        OreOracleOverlay.getInstance().invalidateCache();
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * Custom slider for max visible ores setting.
     */
    private class MaxOresSlider extends AbstractSliderButton {
        private static final int MIN_VALUE = 3;
        private static final int MAX_VALUE = 15;

        public MaxOresSlider(int x, int y, int width, int height, int currentValue) {
            super(x, y, width, height,
                    Component.translatable("oreoracle.config.maxOres", currentValue),
                    (currentValue - MIN_VALUE) / (double) (MAX_VALUE - MIN_VALUE));
        }

        @Override
        protected void updateMessage() {
            int value = getValue();
            setMessage(Component.translatable("oreoracle.config.maxOres", value));
        }

        @Override
        protected void applyValue() {
            config.setMaxVisibleOres(getValue());
        }

        private int getValue() {
            return MIN_VALUE + (int) Math.round(this.value * (MAX_VALUE - MIN_VALUE));
        }
    }
}
