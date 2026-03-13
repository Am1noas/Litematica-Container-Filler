package com.mimicenzymes.schematicfiller.mixin;

import com.mimicenzymes.schematicfiller.core.AutoFillerStateMachine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 拦截原版Minecraft客户端的屏幕渲染
@Mixin(MinecraftClient.class)
public class ScreenInterceptorMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void interceptScreen(Screen screen, CallbackInfo ci) {
        if (screen instanceof HandledScreen && AutoFillerStateMachine.getInstance().isSilentlyExtracting()) {
            ci.cancel();
        }
    }
}