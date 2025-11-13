package net.seb.skyblockpatchnotes.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.seb.skyblockpatchnotes.scraper.HypixelPatchNotesFetcher;

import java.util.ArrayList;
import java.util.List;

/**
 * A custom screen to display patch notes with scrolling functionality.
 */
public class PatchNotesScreen extends Screen {
    private final Screen parent;
    private double scrollOffset = 0;
    private double targetScrollOffset = 0;
    private static final int LINE_HEIGHT = 12;
    private static final int PADDING = 10;
    private static final double SCROLL_SPEED = 0.2; // Smoothness factor (0-1, lower = smoother)

    // Test data - hardcoded patch notes for the latest update
    private final List<String> patchNotes = new ArrayList<>();
    private boolean isLoading = true;

    public PatchNotesScreen(Screen parent) {
        super(Text.literal("Hypixel SkyBlock Patch Notes"));
        this.parent = parent;
        loadPatchNotes();
    }

    /**
     * Load patch notes from Hypixel forums asynchronously
     */
    private void loadPatchNotes() {
        // Show loading message
        patchNotes.add("§e§lLoading patch notes...");
        patchNotes.add("");
        patchNotes.add("§7Fetching data from Hypixel forums...");

        // Fetch patch notes asynchronously
        HypixelPatchNotesFetcher.fetchLatestPatchNotesAsync().thenAccept(notes -> {
            // This runs when the fetch completes
            patchNotes.clear();
            patchNotes.addAll(notes);
            isLoading = false;
        });
    }

    @Override
    protected void init() {
        super.init();

        // Add Done button at the bottom
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> {
            this.close();
        }).dimensions(this.width / 2 - 100, this.height - 30, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Smoothly interpolate scroll offset
        scrollOffset += (targetScrollOffset - scrollOffset) * SCROLL_SPEED;

        // Render background
        super.render(context, mouseX, mouseY, delta);

        // Draw title at the top
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                this.title,
                this.width / 2,
                10,
                0xFFFFFF
        );

        // Define the scrollable area
        int contentTop = 30;
        int contentBottom = this.height - 40;
        int contentHeight = contentBottom - contentTop;

        // Enable scissor (clipping) so content doesn't render outside the area
        context.enableScissor(
                PADDING,
                contentTop,
                this.width - PADDING,
                contentBottom
        );

        // Render each line of patch notes
        int yPos = contentTop - (int)scrollOffset;
        for (String line : patchNotes) {
            // Only render lines that are visible in the scissor area
            if (yPos + LINE_HEIGHT > contentTop && yPos < contentBottom) {
                context.drawTextWithShadow(
                        this.textRenderer,
                        line,
                        PADDING + 5,
                        yPos,
                        0xFFFFFF
                );
            }
            yPos += LINE_HEIGHT;
        }

        // Disable scissor
        context.disableScissor();

        // Draw scroll indicators if content is scrollable
        int totalContentHeight = patchNotes.size() * LINE_HEIGHT;
        if (totalContentHeight > contentHeight) {
            // Draw scroll bar or indicators
            if (targetScrollOffset > 0) {
                // Show "scroll up" indicator
                context.drawCenteredTextWithShadow(
                        this.textRenderer,
                        Text.literal("▲ Scroll Up").formatted(Formatting.GRAY),
                        this.width / 2,
                        contentTop - 10,
                        0xAAAAAA
                );
            }

            if (targetScrollOffset < totalContentHeight - contentHeight) {
                // Show "scroll down" indicator
                context.drawCenteredTextWithShadow(
                        this.textRenderer,
                        Text.literal("▼ Scroll Down").formatted(Formatting.GRAY),
                        this.width / 2,
                        contentBottom + 2,
                        0xAAAAAA
                );
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Calculate total scrollable height
        int contentTop = 30;
        int contentBottom = this.height - 40;
        int contentHeight = contentBottom - contentTop;
        int totalContentHeight = patchNotes.size() * LINE_HEIGHT;
        int maxScroll = Math.max(0, totalContentHeight - contentHeight);

        // Scroll by 2 lines per scroll notch (adjust for smoothness)
        targetScrollOffset -= verticalAmount * LINE_HEIGHT * 2;

        // Clamp target scroll offset
        targetScrollOffset = Math.max(0, Math.min(targetScrollOffset, maxScroll));

        return true;
    }

    @Override
    public void close() {
        assert this.client != null;
        this.client.setScreen(this.parent);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}