package net.seb.skyblockpatchnotes.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.seb.skyblockpatchnotes.SkyblockPatchNotesMod;
import net.seb.skyblockpatchnotes.scraper.HypixelPatchNotesFetcher;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A custom screen to display patch notes with scrolling functionality.
 */
public class PatchNotesScreen extends Screen {
    private final Screen parent;
    private double scrollOffset = 0;
    private double targetScrollOffset = 0;
    private static final int LINE_HEIGHT = 12;
    private static final int PADDING = 10;
    private static final int IMAGE_INITIAL_HEIGHT = 150; // Used while loading
    private static final double SCROLL_SPEED = 0.2;

    private final List<ContentElement> contentElements = new ArrayList<>();
    private String patchUrl = null;
    private String patchTitle = "Hypixel SkyBlock Patch Notes";

    private final Map<String, ImageInfo> loadedImages = new HashMap<>();
    private int imageCounter = 0;

    private static class ContentElement {
        enum Type { TEXT, IMAGE }
        Type type;
        String content;
        int height; // Dynamic height based on scaled image size or LINE_HEIGHT

        ContentElement(Type type, String content) {
            this.type = type;
            this.content = content;
            this.height = type == Type.TEXT ? LINE_HEIGHT : IMAGE_INITIAL_HEIGHT;
        }
    }

    private static class ImageInfo {
        Identifier identifier;
        int width;
        int height;
        boolean loaded;
    }

    public PatchNotesScreen(Screen parent) {
        super(Text.literal("Hypixel SkyBlock Patch Notes"));
        this.parent = parent;
        loadPatchNotes();
    }

    private void loadPatchNotes() {
        contentElements.add(new ContentElement(ContentElement.Type.TEXT, "§e§lLoading patch notes..."));
        contentElements.add(new ContentElement(ContentElement.Type.TEXT, ""));
        contentElements.add(new ContentElement(ContentElement.Type.TEXT, "§7Fetching data from Hypixel forums..."));

        HypixelPatchNotesFetcher.fetchLatestPatchNotesAsync().thenAccept(data -> {
            contentElements.clear();
            patchUrl = data.url;
            patchTitle = data.title;
            parseContent(data.content);
            scrollOffset = 0;
            targetScrollOffset = 0;
        });
    }

    private void parseContent(List<String> rawContent) {
        Pattern imgPattern = Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']");

        for (String line : rawContent) {
            Matcher matcher = imgPattern.matcher(line);
            if (matcher.find()) {
                String imageUrl = matcher.group(1);
                contentElements.add(new ContentElement(ContentElement.Type.IMAGE, imageUrl));
                loadImage(imageUrl);
            } else if (!line.trim().isEmpty()) {
                contentElements.add(new ContentElement(ContentElement.Type.TEXT, line));
            } else {
                contentElements.add(new ContentElement(ContentElement.Type.TEXT, ""));
            }
        }
    }

    private void loadImage(String imageUrl) {
        if (loadedImages.containsKey(imageUrl)) {
            SkyblockPatchNotesMod.LOGGER.info("Image already cached: {}", imageUrl);
            return;
        }

        ImageInfo info = new ImageInfo();
        info.loaded = false;
        loadedImages.put(imageUrl, info);

        new Thread(() -> {
            try {
                SkyblockPatchNotesMod.LOGGER.info("=== Starting image download ===");
                SkyblockPatchNotesMod.LOGGER.info("URL: {}", imageUrl);

                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setInstanceFollowRedirects(true);
                connection.connect();

                int responseCode = connection.getResponseCode();
                SkyblockPatchNotesMod.LOGGER.info("HTTP Response: {}", responseCode);

                if (responseCode == 301 || responseCode == 302) {
                    String newUrl = connection.getHeaderField("Location");
                    if (newUrl != null) {
                        SkyblockPatchNotesMod.LOGGER.info("Redirecting to: " + newUrl);
                        loadImage(newUrl);
                    }
                    return;
                }

                if (responseCode != 200) {
                    SkyblockPatchNotesMod.LOGGER.error("Failed: HTTP {}", responseCode);
                    return;
                }

                InputStream stream = connection.getInputStream();
                SkyblockPatchNotesMod.LOGGER.info("Stream opened, reading image...");

                NativeImage nativeImage;
                try {
                    nativeImage = NativeImage.read(stream);
                } catch (Exception e) {
                    SkyblockPatchNotesMod.LOGGER.error("Failed to parse image data", e);
                    stream.close();
                    return;
                }

                stream.close();

                if (nativeImage == null) {
                    SkyblockPatchNotesMod.LOGGER.error("NativeImage.read returned null");
                    return;
                }

                final int width = nativeImage.getWidth();
                final int height = nativeImage.getHeight();
                final NativeImage finalImage = nativeImage;

                SkyblockPatchNotesMod.LOGGER.info("Image parsed: {}x{}", width, height);
                SkyblockPatchNotesMod.LOGGER.info("Registering on main thread...");

                MinecraftClient.getInstance().execute(() -> {
                    try {
                        Identifier id = Identifier.of("skyblockpatchnotes", "img_" + imageCounter++);
                        SkyblockPatchNotesMod.LOGGER.info("Creating texture: {}", id);

                        MinecraftClient.getInstance().getTextureManager().registerTexture(id,
                                new net.minecraft.client.texture.NativeImageBackedTexture(() -> id.toString(), finalImage));

                        info.identifier = id;
                        info.width = width;
                        info.height = height;
                        info.loaded = true;

                        // Dynamic Height Calculation
                        int maxWidth = this.width - (PADDING * 4);
                        // Calculate scaling factor (constrain to screen width, don't enlarge)
                        float scale = Math.min((float) maxWidth / width, 1.0f);
                        int scaledHeight = (int) (height * scale);

                        // Find and update the content element with the accurate scaled height + PADDING
                        for (ContentElement element : contentElements) {
                            if (element.type == ContentElement.Type.IMAGE && element.content.equals(imageUrl)) {
                                element.height = scaledHeight + PADDING;
                                SkyblockPatchNotesMod.LOGGER.info("Updated element height for {} to {}", imageUrl, element.height);
                                break;
                            }
                        }

                        SkyblockPatchNotesMod.LOGGER.info("✓✓✓ SUCCESS! Image loaded and ready to render! ✓✓✓");
                    } catch (Exception e) {
                        SkyblockPatchNotesMod.LOGGER.error("Failed to register texture", e);
                        e.printStackTrace();
                        finalImage.close();
                    }
                });
            } catch (Exception e) {
                SkyblockPatchNotesMod.LOGGER.error("=== IMAGE LOAD FAILED ===", e);
                e.printStackTrace();
            }
        }, "ImageLoader-" + imageCounter).start();
    }

    @Override
    protected void init() {
        super.init();
        // Only the Done button remains
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> {
            this.close();
        }).dimensions(this.width / 2 - 100, this.height - 30, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        scrollOffset += (targetScrollOffset - scrollOffset) * SCROLL_SPEED;
        super.render(context, mouseX, mouseY, delta);

        int fixedYOffset = 15;

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal(patchTitle).styled(s -> s.withBold(true)),
                this.width / 2,
                fixedYOffset,
                0xFFD700
        );
        fixedYOffset += 20;

        if (patchUrl != null) {
            Text linkText = Text.literal("View on Hypixel Forums")
                    .styled(s -> s.withColor(Formatting.AQUA).withUnderline(true));

            int linkWidth = this.textRenderer.getWidth(linkText);
            int linkX = (this.width - linkWidth) / 2;

            context.drawTextWithShadow(this.textRenderer, linkText, linkX, fixedYOffset, 0x00FFFF);

            if (mouseX >= linkX && mouseX <= linkX + linkWidth &&
                    mouseY >= fixedYOffset && mouseY <= fixedYOffset + LINE_HEIGHT) {
                context.fill(linkX, fixedYOffset + LINE_HEIGHT - 1, linkX + linkWidth, fixedYOffset + LINE_HEIGHT, 0xFF00FFFF);
            }

            fixedYOffset += 20;
        }

        int contentTop = fixedYOffset;
        int contentBottom = this.height - 40;
        int contentHeight = contentBottom - contentTop;

        context.enableScissor(PADDING, contentTop, this.width - PADDING, contentBottom);

        int yPos = contentTop - (int) scrollOffset;

        for (ContentElement element : contentElements) {
            // Check if element is within the visible scroll window
            if (yPos + element.height > contentTop && yPos < contentBottom) {
                if (element.type == ContentElement.Type.TEXT) {
                    context.drawTextWithShadow(this.textRenderer, element.content, PADDING + 5, yPos, 0xFFFFFF);
                } else if (element.type == ContentElement.Type.IMAGE) {
                    ImageInfo img = loadedImages.get(element.content);
                    if (img != null && img.loaded) {
                        int maxWidth = this.width - (PADDING * 4);

                        // Calculate scaling factor: constrain by screen width, but don't enlarge beyond 1.0f
                        float scale = Math.min((float) maxWidth / img.width, 1.0f);
                        int scaledWidth = (int) (img.width * scale);

                        int imageX = (this.width - scaledWidth) / 2;

                        // Add a small vertical offset to prevent clipping right up against text
                        int verticalOffset = PADDING / 2;

                        // Restore robust Matrix Scaling
                        context.getMatrices().push();
                        context.getMatrices().translate(imageX, yPos + verticalOffset, 0);
                        context.getMatrices().scale(scale, scale, 1.0f);

                        // Draw the full texture at (0,0) relative to the translated position
                        context.drawTexture(
                                net.minecraft.client.render.RenderLayer::getGuiTextured,
                                img.identifier,
                                0, 0,
                                0.0f, 0.0f,
                                img.width, img.height,
                                img.width, img.height
                        );

                        context.getMatrices().pop();
                    } else {
                        // Display the "Loading Image..." text in the center of the placeholder area
                        context.drawCenteredTextWithShadow(this.textRenderer,
                                Text.literal("[Loading Image...]").formatted(Formatting.GRAY),
                                this.width / 2, yPos + IMAGE_INITIAL_HEIGHT / 2 - LINE_HEIGHT / 2, 0x888888);
                    }
                }
            }
            yPos += element.height;
        }

        context.disableScissor();

        // Total content height is now calculated accurately using the dynamic heights
        int totalContentHeight = 0;
        for (ContentElement element : contentElements) {
            totalContentHeight += element.height;
        }

        if (totalContentHeight > contentHeight) {
            if (targetScrollOffset > 0) {
                context.drawCenteredTextWithShadow(
                        this.textRenderer,
                        Text.literal("▲ Scroll Up").formatted(Formatting.GRAY),
                        this.width / 2,
                        contentTop - 10,
                        0xAAAAAA
                );
            }

            // Correct check for when to display the "Scroll Down" hint
            if (targetScrollOffset < totalContentHeight - contentHeight) {
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (patchUrl != null && button == 0) {
            int yOffset = 15 + 20;

            Text linkText = Text.literal("View on Hypixel Forums");
            int linkWidth = this.textRenderer.getWidth(linkText);
            int linkX = (this.width - linkWidth) / 2;

            if (mouseX >= linkX && mouseX <= linkX + linkWidth &&
                    mouseY >= yOffset && mouseY <= yOffset + LINE_HEIGHT) {
                net.minecraft.util.Util.getOperatingSystem().open(patchUrl);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int fixedHeight = 15 + 20;
        if (patchUrl != null) fixedHeight += 20;

        int contentTop = fixedHeight;
        int contentBottom = this.height - 40;
        int contentHeight = contentBottom - contentTop;

        int totalContentHeight = 0;
        for (ContentElement element : contentElements) {
            totalContentHeight += element.height;
        }

        int maxScroll = Math.max(0, totalContentHeight - contentHeight);

        targetScrollOffset -= verticalAmount * LINE_HEIGHT * 2;
        targetScrollOffset = Math.max(0, Math.min(targetScrollOffset, maxScroll));

        return true;
    }

    @Override
    public void close() {
        for (ImageInfo img : loadedImages.values()) {
            if (img.loaded && img.identifier != null && this.client != null) {
                this.client.getTextureManager().destroyTexture(img.identifier);
            }
        }
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}