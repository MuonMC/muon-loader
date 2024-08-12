package net.fabricmc.minecraft.test.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.components.SplashRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SplashRenderer.class)
public abstract class Mixin_SplashRenderer {
	private Mixin_SplashRenderer() {}

	@WrapOperation(
			method = "<init>",
			at = @At(
					value = "FIELD",
					target = "Lnet/minecraft/client/gui/components/SplashRenderer;splash:Ljava/lang/String;"
			)
	)
	void changeSplash(SplashRenderer instance, String value, Operation<Void> original) {
		original.call(instance, "Mixin splash test successful!");
	}
}
