package com.teeknox.oreoracle.gui;

import com.teeknox.oreoracle.config.ServerDataManager;
import com.teeknox.oreoracle.data.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen for selecting which ores to track and setting probability filters.
 * Follows Teeknox STYLE_GUIDE.md specifications.
 */
public class OreSelectorScreen extends Screen {
    // Style constants from STYLE_GUIDE.md
    private static final int SETTINGS_BG = 0x80000000;
    private static final int TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int TEXT_SECONDARY = 0xFF888888;
    private static final int SETTINGS_CHECKBOX_OUTLINE = 0xFFAAAAAA;
    private static final int SETTINGS_CHECKBOX_CHECKED = 0xFF00FF00;
    private static final int SETTINGS_UNSELECTED = 0xFF666666;

    // Scrollbar colors (always visible per STYLE_GUIDE.md)
    private static final int SCROLLBAR_TRACK_MODAL = 0x40FFFFFF;
    private static final int SCROLLBAR_THUMB_MODAL = 0x80FFFFFF;
    private static final int SCROLLBAR_THUMB_MODAL_HOVER = 0xA0FFFFFF;

    // Layout constants
    private static final int COLUMN_WIDTH = 300;
    private static final int HEADER_HEIGHT = 52;
    private static final int FOOTER_HEIGHT = 50;
    private static final int ROW_HEIGHT = 20;
    private static final int CHECKBOX_SIZE = 12;
    private static final int LEFT_MARGIN = 20;
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int SCROLLBAR_MIN_THUMB = 24;
    private static final int FILTER_BUTTON_HEIGHT = 20;
    private static final int FILTER_SECTION_HEIGHT = 30;

    private final Screen parent;
    private List<OreEntry> oreEntries;

    // Scroll state
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;
    private double accumulatedScroll = 0;
    private boolean isDraggingScrollbar = false;
    private int scrollbarDragOffset = 0;

    // Filter buttons
    private ButtonWidget greenFilterBtn;
    private ButtonWidget yellowFilterBtn;
    private ButtonWidget redFilterBtn;

    public OreSelectorScreen(Screen parent) {
        super(Text.translatable("oreoracle.screen.selector.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        buildOreEntries();
        calculateScrollBounds();

        int columnX = (this.width - COLUMN_WIDTH) / 2;
        int buttonY = this.height - FOOTER_HEIGHT + 15;

        // Filter buttons (at top of content area)
        int filterY = HEADER_HEIGHT + 5;
        int filterBtnWidth = 60;
        int filterSpacing = 10;
        int totalFilterWidth = filterBtnWidth * 3 + filterSpacing * 2;
        int filterStartX = (this.width - totalFilterWidth) / 2;

        ProbabilityTier currentFilter = ServerDataManager.getInstance().getProbabilityFilter();

        greenFilterBtn = ButtonWidget.builder(Text.translatable("oreoracle.filter.green"), btn -> setFilter(ProbabilityTier.GREEN))
                .dimensions(filterStartX, filterY, filterBtnWidth, FILTER_BUTTON_HEIGHT)
                .build();
        yellowFilterBtn = ButtonWidget.builder(Text.translatable("oreoracle.filter.yellow"), btn -> setFilter(ProbabilityTier.YELLOW))
                .dimensions(filterStartX + filterBtnWidth + filterSpacing, filterY, filterBtnWidth, FILTER_BUTTON_HEIGHT)
                .build();
        redFilterBtn = ButtonWidget.builder(Text.translatable("oreoracle.filter.all"), btn -> setFilter(ProbabilityTier.RED))
                .dimensions(filterStartX + (filterBtnWidth + filterSpacing) * 2, filterY, filterBtnWidth, FILTER_BUTTON_HEIGHT)
                .build();

        addDrawableChild(greenFilterBtn);
        addDrawableChild(yellowFilterBtn);
        addDrawableChild(redFilterBtn);

        updateFilterButtonStyles();

        // Done button (centered at bottom)
        int buttonWidth = 80;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), btn -> close())
                .dimensions((this.width - buttonWidth) / 2, buttonY, buttonWidth, 20)
                .build());
    }

    private void buildOreEntries() {
        oreEntries = new ArrayList<>();
        MinecraftClient client = MinecraftClient.getInstance();

        // Get current dimension to show relevant ores first
        Dimension currentDimension = null;
        if (client.world != null) {
            currentDimension = Dimension.fromWorld(client.world);
        }

        // Add current dimension ores first
        if (currentDimension != null) {
            for (Ore ore : Ore.values()) {
                if (ore.getDimension() == currentDimension) {
                    oreEntries.add(new OreEntry(ore));
                }
            }
        }

        // Add other dimension ores
        for (Ore ore : Ore.values()) {
            if (currentDimension == null || ore.getDimension() != currentDimension) {
                oreEntries.add(new OreEntry(ore));
            }
        }
    }

    private void calculateScrollBounds() {
        int contentHeight = oreEntries.size() * ROW_HEIGHT;
        int availableHeight = this.height - HEADER_HEIGHT - FILTER_SECTION_HEIGHT - FOOTER_HEIGHT;
        maxScrollOffset = Math.max(0, contentHeight - availableHeight);
    }

    private void setFilter(ProbabilityTier tier) {
        ServerDataManager.getInstance().setProbabilityFilter(tier);
        updateFilterButtonStyles();
        OreOracleOverlay.getInstance().invalidateCache();
    }

    private void updateFilterButtonStyles() {
        ProbabilityTier current = ServerDataManager.getInstance().getProbabilityFilter();
        // Visual feedback - button will appear pressed/active based on current selection
        greenFilterBtn.active = current != ProbabilityTier.GREEN;
        yellowFilterBtn.active = current != ProbabilityTier.YELLOW;
        redFilterBtn.active = current != ProbabilityTier.RED;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dim the background
        context.fill(0, 0, this.width, this.height, SETTINGS_BG);

        // Centered title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 16, TEXT_PRIMARY);

        // Subtitle
        Text subtitle = Text.translatable("oreoracle.screen.selector.subtitle");
        context.drawCenteredTextWithShadow(this.textRenderer, subtitle, this.width / 2, 30, TEXT_SECONDARY);

        // Calculate content area
        int columnX = (this.width - COLUMN_WIDTH) / 2;
        int contentStartY = HEADER_HEIGHT + FILTER_SECTION_HEIGHT;
        int contentHeight = this.height - contentStartY - FOOTER_HEIGHT;
        int contentEndY = contentStartY + contentHeight;

        // Enable scissor for scrollable content
        context.enableScissor(columnX, contentStartY, columnX + COLUMN_WIDTH, contentEndY);

        // Render ore entries
        int entryY = contentStartY - scrollOffset;
        for (OreEntry entry : oreEntries) {
            if (entryY + ROW_HEIGHT > contentStartY && entryY < contentEndY) {
                renderOreEntry(context, entry, columnX + LEFT_MARGIN, entryY, mouseX, mouseY);
            }
            entryY += ROW_HEIGHT;
        }

        context.disableScissor();

        // Render scrollbar (always visible per STYLE_GUIDE.md)
        if (maxScrollOffset > 0) {
            renderScrollbar(context, columnX + COLUMN_WIDTH - SCROLLBAR_WIDTH - 2, contentStartY, contentHeight, mouseX, mouseY);
        }

        // Render widgets (buttons)
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderOreEntry(DrawContext context, OreEntry entry, int x, int y, int mouseX, int mouseY) {
        ServerDataManager serverData = ServerDataManager.getInstance();
        boolean isTracked = serverData.isOreTracked(entry.ore);

        // Checkbox
        int checkboxY = y + (ROW_HEIGHT - CHECKBOX_SIZE) / 2;
        drawCheckbox(context, x, checkboxY, isTracked);

        // Ore name with tier color indicator
        int textX = x + CHECKBOX_SIZE + 8;
        int textColor = isTracked ? TEXT_PRIMARY : SETTINGS_UNSELECTED;

        // Get current tier for this ore
        MinecraftClient client = MinecraftClient.getInstance();
        Identifier biome = BiomeChecker.getCurrentBiome(client);
        int currentY = client.player != null ? (int) client.player.getY() : 0;
        ProbabilityTier tier = OreDistribution.getTier(entry.ore, currentY, biome);

        // Draw tier color dot
        if (tier != ProbabilityTier.NONE) {
            int dotSize = 6;
            int dotY = y + (ROW_HEIGHT - dotSize) / 2;
            context.fill(textX, dotY, textX + dotSize, dotY + dotSize, tier.getColor());
            textX += dotSize + 4;
        }

        // Draw ore name
        String displayName = entry.ore.getDisplayName();
        if (entry.ore.getDimension() != Dimension.OVERWORLD) {
            displayName += " (" + entry.ore.getDimension().name().charAt(0) + ")";
        }
        context.drawText(this.textRenderer, displayName, textX, y + 5, textColor, false);
    }

    private void drawCheckbox(DrawContext context, int x, int y, boolean checked) {
        // Outer border
        context.fill(x, y, x + CHECKBOX_SIZE, y + 1, SETTINGS_CHECKBOX_OUTLINE);
        context.fill(x, y + CHECKBOX_SIZE - 1, x + CHECKBOX_SIZE, y + CHECKBOX_SIZE, SETTINGS_CHECKBOX_OUTLINE);
        context.fill(x, y, x + 1, y + CHECKBOX_SIZE, SETTINGS_CHECKBOX_OUTLINE);
        context.fill(x + CHECKBOX_SIZE - 1, y, x + CHECKBOX_SIZE, y + CHECKBOX_SIZE, SETTINGS_CHECKBOX_OUTLINE);

        // Fill if checked
        if (checked) {
            context.fill(x + 2, y + 2, x + CHECKBOX_SIZE - 2, y + CHECKBOX_SIZE - 2, SETTINGS_CHECKBOX_CHECKED);
        }
    }

    private void renderScrollbar(DrawContext context, int x, int y, int height, int mouseX, int mouseY) {
        // Track
        context.fill(x, y, x + SCROLLBAR_WIDTH, y + height, SCROLLBAR_TRACK_MODAL);

        // Calculate thumb
        int contentHeight = oreEntries.size() * ROW_HEIGHT;
        float viewRatio = (float) height / contentHeight;
        int thumbHeight = Math.max(SCROLLBAR_MIN_THUMB, (int) (height * viewRatio));
        int maxThumbY = height - thumbHeight;
        int thumbY = maxScrollOffset > 0 ? (int) ((float) scrollOffset / maxScrollOffset * maxThumbY) : 0;

        // Thumb color
        boolean hovered = mouseX >= x && mouseX < x + SCROLLBAR_WIDTH &&
                mouseY >= y + thumbY && mouseY < y + thumbY + thumbHeight;
        int thumbColor = (isDraggingScrollbar || hovered) ? SCROLLBAR_THUMB_MODAL_HOVER : SCROLLBAR_THUMB_MODAL;

        context.fill(x, y + thumbY, x + SCROLLBAR_WIDTH, y + thumbY + thumbHeight, thumbColor);
    }

    /**
     * Handle mouse clicks on ore entries and scrollbar.
     */
    private boolean handleClick(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Check ore entry clicks
            int columnX = (this.width - COLUMN_WIDTH) / 2;
            int contentStartY = HEADER_HEIGHT + FILTER_SECTION_HEIGHT;
            int contentHeight = this.height - contentStartY - FOOTER_HEIGHT;

            if (mouseX >= columnX + LEFT_MARGIN && mouseX < columnX + COLUMN_WIDTH - SCROLLBAR_WIDTH - 4) {
                if (mouseY >= contentStartY && mouseY < contentStartY + contentHeight) {
                    int clickedIndex = (int) ((mouseY - contentStartY + scrollOffset) / ROW_HEIGHT);
                    if (clickedIndex >= 0 && clickedIndex < oreEntries.size()) {
                        OreEntry entry = oreEntries.get(clickedIndex);
                        ServerDataManager.getInstance().toggleOreTracked(entry.ore);
                        OreOracleOverlay.getInstance().invalidateCache();
                        return true;
                    }
                }
            }

            // Check scrollbar drag
            if (maxScrollOffset > 0) {
                int scrollbarX = columnX + COLUMN_WIDTH - SCROLLBAR_WIDTH - 2;
                if (mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_WIDTH) {
                    if (mouseY >= contentStartY && mouseY < contentStartY + contentHeight) {
                        isDraggingScrollbar = true;
                        int thumbY = getScrollbarThumbY(contentStartY, contentHeight);
                        scrollbarDragOffset = (int) mouseY - thumbY;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Handle mouse release to stop scrollbar dragging.
     */
    private void handleRelease(int button) {
        if (button == 0) {
            isDraggingScrollbar = false;
        }
    }

    /**
     * Handle mouse dragging for scrollbar.
     */
    private boolean handleDrag(double mouseY) {
        if (isDraggingScrollbar) {
            int contentStartY = HEADER_HEIGHT + FILTER_SECTION_HEIGHT;
            int contentHeight = this.height - contentStartY - FOOTER_HEIGHT;
            int thumbHeight = getScrollbarThumbHeight(contentHeight);
            int maxThumbY = contentHeight - thumbHeight;

            int newThumbY = (int) mouseY - scrollbarDragOffset - contentStartY;
            newThumbY = Math.max(0, Math.min(maxThumbY, newThumbY));

            if (maxThumbY > 0) {
                scrollOffset = (int) ((float) newThumbY / maxThumbY * maxScrollOffset);
            }
            return true;
        }
        return false;
    }

    /**
     * Handle scroll wheel input.
     */
    private void handleScroll(double verticalAmount) {
        accumulatedScroll += verticalAmount;
        int lines = (int) accumulatedScroll;
        if (lines != 0) {
            accumulatedScroll -= lines;
            scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset - lines * ROW_HEIGHT));
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        if (handleClick(click.x(), click.y(), click.button())) {
            return true;
        }
        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseReleased(Click click) {
        handleRelease(click.button());
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (handleDrag(click.y())) {
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        handleScroll(verticalAmount);
        return true;
    }

    private int getScrollbarThumbY(int contentStartY, int contentHeight) {
        int thumbHeight = getScrollbarThumbHeight(contentHeight);
        int maxThumbY = contentHeight - thumbHeight;
        return contentStartY + (maxScrollOffset > 0 ? (int) ((float) scrollOffset / maxScrollOffset * maxThumbY) : 0);
    }

    private int getScrollbarThumbHeight(int contentHeight) {
        int totalContentHeight = oreEntries.size() * ROW_HEIGHT;
        float viewRatio = (float) contentHeight / totalContentHeight;
        return Math.max(SCROLLBAR_MIN_THUMB, (int) (contentHeight * viewRatio));
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private record OreEntry(Ore ore) {}
}
