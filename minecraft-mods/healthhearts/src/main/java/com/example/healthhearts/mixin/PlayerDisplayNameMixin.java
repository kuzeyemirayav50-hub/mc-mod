package com.example.healthhearts.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Oyuncunun görünen adının sonuna kalp ekler; isim etiketi bunu kullandığı için
 * kalpler kafanın üstünde görünür. Sadece istemci oyuncuları etkilenir.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerDisplayNameMixin {
	@Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
	private void healthhearts$appendHearts(CallbackInfoReturnable<Text> cir) {
		PlayerEntity self = (PlayerEntity) (Object) this;
		// Entegre sunucudaki ServerPlayerEntity'yi (ölüm mesajları vb.) etkileme
		if (!(self instanceof AbstractClientPlayerEntity)) return;

		float hp = self.getHealth();
		float max = self.getMaxHealth();
		float absorption = self.getAbsorptionAmount();

		int total = Math.min(20, Math.max(1, (int) Math.ceil(max / 2.0F)));
		int full = Math.min(total, (int) Math.ceil(hp / 2.0F));
		int gold = Math.min(10, (int) Math.ceil(absorption / 2.0F));

		MutableText out = cir.getReturnValue().copy().append(Text.literal(" "));
		for (int i = 0; i < total; i++) {
			out.append(Text.literal("\u2665").formatted(i < full ? Formatting.RED : Formatting.DARK_GRAY));
		}
		for (int i = 0; i < gold; i++) {
			out.append(Text.literal("\u2665").formatted(Formatting.GOLD));
		}
		cir.setReturnValue(out);
	}
}
