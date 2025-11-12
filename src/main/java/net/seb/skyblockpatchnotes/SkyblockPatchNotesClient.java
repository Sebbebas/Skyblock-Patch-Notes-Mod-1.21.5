package net.seb.skyblockpatchnotes;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class SkyblockPatchNotesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Build bold base text
            MutableText message = Text.literal("Latest patch version 1.0 2025-11-13 ")
                    .styled(s -> s.withBold(true));

            // Build clickable gold text that runs a command when clicked (sends a chat message)
            MutableText clickable = Text.literal("[Click to Open]")
                    .styled(s -> s.withColor(Formatting.GOLD).withBold(true)
                            .withClickEvent(new ClickEvent.RunCommand("/say Opening patch notes")));

            message.append(clickable);

            // Send the composed message to the client's chat HUD
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null && mc.inGameHud != null) {
                mc.inGameHud.getChatHud().addMessage(message);
            }
        });
    }
}
