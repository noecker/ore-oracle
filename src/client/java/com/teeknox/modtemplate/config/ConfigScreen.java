package com.teeknox.modtemplate.config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Configuration screen for mod settings.
 *
 * This is a basic template - expand with widgets for your specific settings:
 * - SliderWidget for numeric values
 * - ButtonWidget for toggles/actions
 * - TextFieldWidget for text input
 *
 * Minecraft 1.21.9+ notes:
 * - Use DrawContext for all rendering
 * - Widget creation uses builder pattern
 */
public class ConfigScreen extends Screen {
    private final Screen parent;
    private final ModConfig config;

    public ConfigScreen(Screen parent) {
        super(Text.translatable("gui.mod-template.settings"));
        this.parent = parent;
        this.config = ModConfig.getInstance();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 4;

        // Example toggle button
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Enabled: " + (config.isEnabled() ? "ON" : "OFF")),
                button -> {
                    config.setEnabled(!config.isEnabled());
                    button.setMessage(Text.literal("Enabled: " + (config.isEnabled() ? "ON" : "OFF")));
                }
        ).dimensions(centerX - 100, startY, 200, 20).build());

        // Done button
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.done"),
                button -> close()
        ).dimensions(centerX - 100, startY + 50, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw background (semi-transparent dark overlay)
        renderBackground(context, mouseX, mouseY, delta);

        // Draw title centered at top
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                this.title,
                this.width / 2,
                20,
                0xFFFFFF
        );

        // Render all widgets
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
