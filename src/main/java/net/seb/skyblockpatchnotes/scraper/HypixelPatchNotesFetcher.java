package net.seb.skyblockpatchnotes.scraper;

import net.seb.skyblockpatchnotes.SkyblockPatchNotesMod;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Fetches and parses Hypixel SkyBlock patch notes from the forums
 */
public class HypixelPatchNotesFetcher {
    private static final String HYPIXEL_FORUMS = "https://hypixel.net/forums/";
    private static final String NEWS_SECTION = "News and Announcements";

    /**
     * Fetches patch notes asynchronously to avoid blocking the game thread
     * @return CompletableFuture containing the patch notes as a list of strings
     */
    public static CompletableFuture<List<String>> fetchLatestPatchNotesAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return fetchLatestPatchNotes();
            } catch (Exception e) {
                SkyblockPatchNotesMod.LOGGER.error("Failed to fetch patch notes", e);
                return getErrorMessage();
            }
        });
    }

    /**
     * Fetches the latest SkyBlock patch notes from Hypixel forums
     * @return List of formatted patch note lines
     */
    private static List<String> fetchLatestPatchNotes() throws IOException {
        List<String> patchNotes = new ArrayList<>();

        SkyblockPatchNotesMod.LOGGER.info("Fetching Hypixel forums homepage...");

        // Step 1: Fetch the main forums page
        Document forumsPage = Jsoup.connect(HYPIXEL_FORUMS)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(10000)
                .get();

        // Step 2: Find the "News and Announcements" section
        String newsUrl = findNewsSectionUrl(forumsPage);
        if (newsUrl == null) {
            throw new IOException("Could not find News and Announcements section");
        }

        SkyblockPatchNotesMod.LOGGER.info("Found News section: {}", newsUrl);

        // Step 3: Fetch the News and Announcements page
        Document newsPage = Jsoup.connect(newsUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(10000)
                .get();

        // Step 4: Find the latest SkyBlock update thread
        String updateUrl = findLatestSkyBlockUpdate(newsPage);
        if (updateUrl == null) {
            throw new IOException("Could not find latest SkyBlock update");
        }

        SkyblockPatchNotesMod.LOGGER.info("Found latest update: {}", updateUrl);

        // Step 5: Fetch the update thread and parse it
        Document updatePage = Jsoup.connect(updateUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(10000)
                .get();

        patchNotes = parseUpdateThread(updatePage, updateUrl);

        return patchNotes;
    }

    /**
     * Finds the URL for the News and Announcements section
     */
    private static String findNewsSectionUrl(Document forumsPage) {
        // Look for links or sections containing "News and Announcements"
        Elements links = forumsPage.select("a[href]");

        for (Element link : links) {
            String text = link.text();
            if (text.contains("News and Announcements") || text.contains("News & Announcements")) {
                String href = link.attr("abs:href");
                if (!href.isEmpty()) {
                    return href;
                }
            }
        }

        // Alternative: Look for forum nodes with the title
        Elements forumNodes = forumsPage.select(".node-title");
        for (Element node : forumNodes) {
            if (node.text().contains("News and Announcements")) {
                Element link = node.selectFirst("a");
                if (link != null) {
                    return link.attr("abs:href");
                }
            }
        }

        return null;
    }

    /**
     * Finds the latest Hypixel SkyBlock update thread
     */
    private static String findLatestSkyBlockUpdate(Document newsPage) {
        // Look for thread titles containing "SkyBlock" or "Hypixel SkyBlock"
        Elements threads = newsPage.select(".structItem-title a");

        for (Element thread : threads) {
            String title = thread.text();
            // Look for threads that mention SkyBlock and version numbers
            if ((title.contains("SkyBlock") || title.contains("Skyblock")) &&
                    (title.matches(".*\\d+\\.\\d+.*") || title.contains("Update"))) {
                return thread.attr("abs:href");
            }
        }

        return null;
    }

    /**
     * Parses the update thread and extracts formatted patch notes
     */
    private static List<String> parseUpdateThread(Document updatePage, String url) {
        List<String> notes = new ArrayList<>();

        // Get the thread title
        Element titleElement = updatePage.selectFirst(".p-title-value");
        String title = titleElement != null ? titleElement.text() : "Hypixel SkyBlock Update";

        notes.add("§6§l" + title);
        notes.add("");
        notes.add("§7Source: §9§n" + url);
        notes.add("");

        // Get the first post content (the announcement)
        Element firstPost = updatePage.selectFirst(".message-body .bbWrapper");

        if (firstPost != null) {
            // Parse the content - convert BB code and HTML to Minecraft formatting
            parseContent(firstPost, notes);
        } else {
            notes.add("§cCould not parse patch notes content");
        }

        return notes;
    }

    /**
     * Parses HTML content and converts it to Minecraft-formatted text
     */
    private static void parseContent(Element content, List<String> notes) {
        // Process each element
        for (Element element : content.children()) {
            String tagName = element.tagName();
            String text = element.text().trim();

            if (text.isEmpty()) continue;

            switch (tagName) {
                case "h1", "h2", "h3":
                    // Headers in gold and bold
                    notes.add("");
                    notes.add("§6§l" + text);
                    notes.add("");
                    break;

                case "b", "strong":
                    // Bold text
                    notes.add("§l" + text);
                    break;

                case "ul", "ol":
                    // Lists
                    Elements listItems = element.select("li");
                    for (Element li : listItems) {
                        notes.add("§7  • " + li.text());
                    }
                    notes.add("");
                    break;

                case "p":
                    // Paragraphs - split long lines
                    String[] lines = wrapText(text, 80);
                    for (String line : lines) {
                        notes.add("§7" + line);
                    }
                    notes.add("");
                    break;

                default:
                    // Default: just add the text
                    if (!text.isEmpty()) {
                        notes.add("§7" + text);
                    }
                    break;
            }
        }
    }

    /**
     * Wraps text to a maximum width
     */
    private static String[] wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > maxWidth) {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                }
            }
            if (currentLine.length() > 0) {
                currentLine.append(" ");
            }
            currentLine.append(word);
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines.toArray(new String[0]);
    }

    /**
     * Returns an error message if fetching fails
     */
    private static List<String> getErrorMessage() {
        List<String> error = new ArrayList<>();
        error.add("§c§lError Loading Patch Notes");
        error.add("");
        error.add("§7Could not fetch patch notes from Hypixel forums.");
        error.add("§7Please check your internet connection and try again.");
        error.add("");
        error.add("§7You can view patch notes directly at:");
        error.add("§9§nhttps://hypixel.net/forums/");
        return error;
    }
}