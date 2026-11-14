package com.example.pvphud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class PvpHudClient implements ClientModInitializer {
	public static final String MOD_ID = "pvphud";
	public static boolean enabled = true;

	@Override
	public void onInitializeClient() {
		CombatTracker.init();

		HudElementRegistry.attachElementAfter(
				VanillaHudElements.MISC_OVERLAYS,
				Identifier.of(MOD_ID, "pvp_hud"),
				PvpHud::render);

		// /pvphud yazarak HUD'u aç/kapat
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommandManager.literal("pvphud").executes(ctx -> {
					enabled = !enabled;
					ctx.getSource().sendFeedback(Text.literal("PvP HUD: " + (enabled ? "açık" : "kapalı")));
					return 1;
				})));
	}
}
