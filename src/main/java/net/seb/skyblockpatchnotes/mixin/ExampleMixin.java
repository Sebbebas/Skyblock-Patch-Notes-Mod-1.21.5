package net.seb.skyblockpatchnotes.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ExampleMixin {
	@Inject(at = @At("RETURN"), method = "onGameJoin")
	private void onGameJoin(CallbackInfo ci) {
		// This triggers when the player joins a world/server
		MinecraftClient client = MinecraftClient.getInstance();

		// Build clickable gold text that runs our new command when clicked
		MutableText clickable = Text.literal("[Click to Open]")
				.styled(s -> s.withColor(Formatting.GOLD).withBold(true)
						.withClickEvent(new ClickEvent.RunCommand("/showpatchnotes")));

		// Build the full message with the clickable text
		MutableText message = Text.literal("Skyblock Patch Notes: ")
				.append(clickable);

		// Send the message to the player
		if (client.inGameHud != null) {
			client.inGameHud.getChatHud().addMessage(message);
		}
	}
}