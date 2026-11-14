package com.example.pvphud;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Optional;

/** Sadece okuma/ölçme yapar: CPS, combo, son vuruş mesafesi ve hedef takibi. */
public final class CombatTracker {
	private static final ArrayDeque<Long> LEFT = new ArrayDeque<>();
	private static final ArrayDeque<Long> RIGHT = new ArrayDeque<>();
	private static boolean leftWas;
	private static boolean rightWas;

	public static int combo;
	public static LivingEntity target;
	public static long lastAttackMs;
	public static long lastHitMs;
	public static double lastReach;

	private static int prevTargetHurt;
	private static int prevSelfHurt;

	private CombatTracker() {}

	public static void init() {
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			MinecraftClient mc = MinecraftClient.getInstance();
			if (world.isClient() && player == mc.player && entity instanceof LivingEntity living) {
				if (living != target) {
					combo = 0;
					prevTargetHurt = living.hurtTime;
				}
				target = living;
				lastAttackMs = System.currentTimeMillis();

				Vec3d eye = player.getEyePos();
				Vec3d end = eye.add(player.getRotationVec(1.0F).multiply(8.0));
				Optional<Vec3d> point = living.getBoundingBox().raycast(eye, end);
				lastReach = point.map(eye::distanceTo)
						.orElse(eye.distanceTo(living.getBoundingBox().getCenter()));
			}
			return ActionResult.PASS;
		});
		ClientTickEvents.END_CLIENT_TICK.register(CombatTracker::tick);
	}

	private static void tick(MinecraftClient mc) {
		if (mc.player == null) {
			combo = 0;
			target = null;
			return;
		}
		long now = System.currentTimeMillis();

		// Vurulunca combo sıfırlanır
		int selfHurt = mc.player.hurtTime;
		if (selfHurt > prevSelfHurt) {
			combo = 0;
		}
		prevSelfHurt = selfHurt;

		// Hedefin hurtTime değeri vuruştan hemen sonra yükselirse vuruş isabet etmiştir
		if (target != null) {
			int th = target.hurtTime;
			if (th > prevTargetHurt && now - lastAttackMs < 700) {
				combo++;
				lastHitMs = now;
			}
			prevTargetHurt = th;
			if (combo > 0 && now - lastHitMs > 2500) {
				combo = 0;
			}
		}
	}

	/** Her karede çağrılır; fare tuşlarının basılma kenarlarını sayar. */
	public static void pollClicks(MinecraftClient mc) {
		long window = mc.getWindow().getHandle();
		long now = System.currentTimeMillis();
		boolean free = mc.currentScreen == null;

		boolean l = free && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean r = free && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
		if (l && !leftWas) LEFT.addLast(now);
		if (r && !rightWas) RIGHT.addLast(now);
		leftWas = l;
		rightWas = r;

		while (!LEFT.isEmpty() && now - LEFT.peekFirst() > 1000) LEFT.pollFirst();
		while (!RIGHT.isEmpty() && now - RIGHT.peekFirst() > 1000) RIGHT.pollFirst();
	}

	public static int leftCps() {
		return LEFT.size();
	}

	public static int rightCps() {
		return RIGHT.size();
	}
}
