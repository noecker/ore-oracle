package com.teeknox.oreoracle.config;

import com.teeknox.oreoracle.gui.OreOracleOverlay;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

/**
 * Configuration screen for Ore Oracle settings.
 * Follows Teeknox STYLE_GUIDE.md specifications.
 */
public class ConfigScreen extends Screen {
    private static final int SETTINGS_BG = 0x80000000;
    private static final int TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int TEXT_SECONDARY = 0xFF888888;

    private static final int COLUMN_WIDTH = 300;
    private static final int HEADER_HEIGHT = 52;
    private static final int FOOTER_HEIGHT = 50;
    private static final int ROW_HEIGHT = 24;
    private static final int LEFT_MARGIN = 20;

    private final Screen parent;
    private final ModConfig config;

    // Widgets
    private CyclingButtonWidget<Boolean> enabledButton;
    private CyclingButtonWidget<Boolean> showHeaderButton;
    private CyclingButtonWidget<ModConfig.HudPosition> hudPositionButton;
    private CyclingButtonWidget<ModConfig.DisplayMode> displayModeButton;
    private MaxOresSlider maxOresSlider;

    public ConfigScreen(Screen parent) {
        super(Text.translatable("oreoracle.screen.config.title"));
        this.parent = parent;
        this.config = ModConfig.getInstance();
    }

    @Override
    protected void init() {
        int columnX = (this.width - COLUMN_WIDTH) / 2;
        int contentX = columnX + LEFT_MARGIN;
        int buttonWidth = COLUMN_WIDTH - LEFT_MARGIN * 2;

        int currentY = HEADER_HEIGHT;

        // Enabled toggle
        enabledButton = CyclingButtonWidget.onOffBuilder(config.isEnabled())
                .build(contentX, currentY, buttonWidth, 20,
                        Text.translatable("oreoracle.config.enabled"),
                        (button, value) -> config.setEnabled(value));
        addDrawableChild(enabledButton);
        currentY += ROW_HEIGHT;

        // Show header toggle
        showHeaderButton = CyclingButtonWidget.onOffBuilder(config.isShowHudHeader())
                .build(contentX, currentY, buttonWidth, 20,
                        Text.translatable("oreoracle.config.showHeader"),
                        (button, value) -> config.setShowHudHeader(value));
        addDrawableChild(showHeaderButton);
        currentY += ROW_HEIGHT;

        // HUD position
        hudPositionButton = CyclingButtonWidget.<ModConfig.HudPosition>builder(position ->
                        Text.translatable("oreoracle.config.position." + position.name().toLowerCase()))
                .values(ModConfig.HudPosition.values())
                .initially(config.getHudPosition())
                .build(contentX, currentY, buttonWidth, 20,
                        Text.translatable("oreoracle.config.position"),
                        (button, value) -> config.setHudPosition(value));
        addDrawableChild(hudPositionButton);
        currentY += ROW_HEIGHT;

        // Display mode
        displayModeButton = CyclingButtonWidget.<ModConfig.DisplayMode>builder(mode ->
                        Text.translatable("oreoracle.config.displayMode." + mode.name().toLowerCase()))
                .values(ModConfig.DisplayMode.values())
                .initially(config.getDisplayMode())
                .build(contentX, currentY, buttonWidth, 20,
                        Text.translatable("oreoracle.config.displayMode"),
                        (button, value) -> config.setDisplayMode(value));
        addDrawableChild(displayModeButton);
        currentY += ROW_HEIGHT;

        // Max visible ores slider
        maxOresSlider = new MaxOresSlider(contentX, currentY, buttonWidth, 20,
                config.getMaxVisibleOres());
        addDrawableChild(maxOresSlider);
        currentY += ROW_HEIGHT;

        // Done button
        int buttonY = this.height - FOOTER_HEIGHT + 15;
        int doneButtonWidth = 80;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), btn -> close())
                .dimensions((this.width - doneButtonWidth) / 2, buttonY, doneButtonWidth, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dim the background
        context.fill(0, 0, this.width, this.height, SETTINGS_BG);

        // Centered title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 16, TEXT_PRIMARY);

        // Subtitle
        Text subtitle = Text.translatable("oreoracle.screen.config.subtitle");
        context.drawCenteredTextWithShadow(this.textRenderer, subtitle, this.width / 2, 30, TEXT_SECONDARY);

        // Render widgets
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        config.save();
        OreOracleOverlay.getInstance().invalidateCache();
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /**
     * Custom slider for max visible ores setting.
     */
    private class MaxOresSlider extends SliderWidget {
        private static final int MIN_VALUE = 3;
        private static final int MAX_VALUE = 15;

        public MaxOresSlider(int x, int y, int width, int height, int currentValue) {
            super(x, y, width, height,
                    Text.translatable("oreoracle.config.maxOres", currentValue),
                    (currentValue - MIN_VALUE) / (double) (MAX_VALUE - MIN_VALUE));
        }

        @Override
        protected void updateMessage() {
            int value = getValue();
            setMessage(Text.translatable("oreoracle.config.maxOres", value));
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
