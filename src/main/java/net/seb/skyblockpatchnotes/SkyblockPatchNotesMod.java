package net.seb.skyblockpatchnotes;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.seb.skyblockpatchnotes.gui.PatchNotesScreen; // Correct import
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

// Changed to ClientModInitializer
public class SkyblockPatchNotesMod implements ClientModInitializer {
    public static final String MOD_ID = "skyblockpatchnotes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        // This method is now used because we implement ClientModInitializer
        LOGGER.info("Initializing Skyblock Patch Notes Mod (Client)");

        // Register the client-side command
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    // Create a literal command "/showpatchnotes"
                    literal("showpatchnotes")
                            .executes(context -> {
                                // This code runs when the command is executed

                                // We need to run this on the main client thread
                                // as it opens a screen
                                MinecraftClient.getInstance().execute(() -> {
                                    // Get the current screen (to use as the parent)
                                    Screen currentScreen = MinecraftClient.getInstance().currentScreen;
                                    // Open our new PatchNotesScreen, passing the current screen as the parent
                                    MinecraftClient.getInstance().setScreen(new PatchNotesScreen(currentScreen));
                                });

                                return 1; // Indicate successful execution
                            })
            );
        });
    }
}