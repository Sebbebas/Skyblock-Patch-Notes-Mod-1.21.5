package net.seb.skyblockpatchnotes.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * A custom screen to display patch notes.
 * This is the blank UI you requested, with a "Done" button.
 */
public class PatchNotesScreen extends Screen {

    // The screen that was open before this one (e.g., the Title Screen)
    private final Screen parent;

    /**
     * Constructor for the screen.
     * @param parent The screen to return to when this one is closed.
     */
    public PatchNotesScreen(Screen parent) {
        super(Text.literal("Patch Notes")); // The title of the screen
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        // Add a "Done" button that closes this screen and returns to the parent.
        // It's centered horizontally and near the bottom.
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> {
            this.close(); // Call the close() method when pressed
        }).dimensions(this.width / 2 - 100, this.height - 40, 200, 20).build()); // x, y, width, height
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render the default background (a semi-transparent dark overlay)
        super.render(context, mouseX, mouseY, delta);

        // Render the title text, centered at the top of the screen
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                this.title, // Use the title defined in the constructor
                this.width / 2,
                15,
                0xFFFFFF // White color
        );

        // Render a placeholder message
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("This is the blank UI. Patch notes will go here."),
                this.width / 2,
                this.height / 2 - 4, // Centered vertically
                0xFFFFFF // White color
        );
    }

    @Override
    public void close() {
        // When the screen is closed (by the "Done" button or Esc key),
        // set the client's current screen back to the parent.
        assert this.client != null;
        this.client.setScreen(this.parent);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        // We want the Esc key to close the screen
        return true;
    }
}