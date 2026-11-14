package com.example.pvphud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PvpHud {
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GREEN = 0xFF55FF55;
	private static final int YELLOW = 0xFFFFFF55;
	private static final int RED = 0xFFFF5555;
	private static final int GRAY = 0xFFAAAAAA;

	private static final EquipmentSlot[] ARMOR = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	private record Line(String text, int color) {}

	private PvpHud() {}

	public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
		MinecraftClient mc = MinecraftClient.getInstance();
		ClientPlayerEntity player = mc.player;
		if (!PvpHudClient.enabled || player == null || mc.options.hudHidden) return;

		CombatTracker.pollClicks(mc);
		TextRenderer tr = mc.textRenderer;

		// ---- Sol üst panel ----
		int fps = mc.getCurrentFps();
		int ping = ping(mc, player);

		List<Line> lines = new ArrayList<>();
		lines.add(new Line("FPS: " + fps, fps >= 120 ? GREEN : fps >= 60 ? YELLOW : RED));
		lines.add(new Line(ping < 0 ? "Ping: -" : "Ping: " + ping + " ms",
				ping < 0 ? GRAY : ping <= 60 ? GREEN : ping <= 150 ? YELLOW : RED));
		lines.add(new Line("CPS: " + CombatTracker.leftCps() + " | " + CombatTracker.rightCps(), WHITE));
		lines.add(new Line("Combo: " + CombatTracker.combo, CombatTracker.combo >= 3 ? GREEN : WHITE));
		lines.add(new Line(CombatTracker.lastReach > 0
				? String.format(Locale.ROOT, "Reach: %.2f", CombatTracker.lastReach)
				: "Reach: -", WHITE));

		int x = 4;
		int y = 4;
		int maxW = 0;
		for (Line l : lines) maxW = Math.max(maxW, tr.getWidth(l.text()));
		ctx.fill(x, y, x + maxW + 8, y + lines.size() * 10 + 6, 0x90000000);
		int ty = y + 4;
		for (Line l : lines) {
			ctx.drawTextWithShadow(tr, l.text(), x + 4, ty, l.color());
			ty += 10;
		}

		// ---- Zırh dayanıklılığı ----
		int ay = y + lines.size() * 10 + 10;
		for (EquipmentSlot slot : ARMOR) {
			ItemStack stack = player.getEquippedStack(slot);
			if (stack.isEmpty()) continue;
			ctx.drawItem(stack, x, ay);
			if (stack.isDamageable()) {
				int max = stack.getMaxDamage();
				int pct = (max - stack.getDamage()) * 100 / max;
				ctx.drawTextWithShadow(tr, pct + "%", x + 20, ay + 4, pct > 50 ? GREEN : pct > 25 ? YELLOW : RED);
			}
			ay += 16;
		}

		// ---- Eşya sayaçları (totem, gapple, pearl) ----
		ay += 4;
		ay = drawCount(ctx, tr, new ItemStack(Items.TOTEM_OF_UNDYING),
				player.getInventory().count(Items.TOTEM_OF_UNDYING), x, ay);
		ay = drawCount(ctx, tr, new ItemStack(Items.GOLDEN_APPLE),
				player.getInventory().count(Items.GOLDEN_APPLE)
						+ player.getInventory().count(Items.ENCHANTED_GOLDEN_APPLE), x, ay);
		drawCount(ctx, tr, new ItemStack(Items.ENDER_PEARL),
				player.getInventory().count(Items.ENDER_PEARL), x, ay);

		// ---- Hedef canı (crosshair'ın altı) ----
		LivingEntity t = CombatTracker.target;
		long now = System.currentTimeMillis();
		if (t != null && t.isAlive() && now - CombatTracker.lastAttackMs < 5000) {
			float hp = t.getHealth();
			float maxHp = Math.max(1F, t.getMaxHealth());
			float abs = t.getAbsorptionAmount();
			float ratio = Math.min(1F, hp / maxHp);

			int cx = ctx.getScaledWindowWidth() / 2;
			int cy = ctx.getScaledWindowHeight() / 2 + 24;

			ctx.drawCenteredTextWithShadow(tr, t.getName().getString(), cx, cy, WHITE);
			int barY = cy + 11;
			ctx.fill(cx - 41, barY - 1, cx + 41, barY + 5, 0xC0000000);
			ctx.fill(cx - 40, barY, cx + 40, barY + 4, 0xFF333333);
			ctx.fill(cx - 40, barY, cx - 40 + (int) (80 * ratio), barY + 4,
					ratio > 0.5F ? GREEN : ratio > 0.25F ? YELLOW : RED);

			String hpText = String.format(Locale.ROOT, "%.1f / %.0f HP", hp, maxHp)
					+ (abs > 0 ? String.format(Locale.ROOT, " +%.1f", abs) : "");
			ctx.drawCenteredTextWithShadow(tr, hpText, cx, barY + 8, WHITE);
		}
	}

	private static int drawCount(DrawContext ctx, TextRenderer tr, ItemStack icon, int count, int x, int y) {
		ctx.drawItem(icon, x, y);
		ctx.drawTextWithShadow(tr, "x" + count, x + 20, y + 4, count > 0 ? WHITE : RED);
		return y + 16;
	}

	private static int ping(MinecraftClient mc, ClientPlayerEntity player) {
		if (mc.getNetworkHandler() == null) return -1;
		PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
		return entry == null ? -1 : entry.getLatency();
	}
}
