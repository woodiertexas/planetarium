package com.woodiertexas.planetarium.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.woodiertexas.planetarium.PlanetInfo;
import com.woodiertexas.planetarium.PlanetManager;
import com.woodiertexas.planetarium.Planetarium;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(LevelRenderer.class)
public class WorldRendererMixin {
	@Shadow
	private @Nullable ClientLevel level;

	@Unique
	private PlanetManager planetarium$planetManager;
	
	@Inject(method = "<init>", at = @At("TAIL"))
	private void createPlanetManager(Minecraft client, EntityRenderDispatcher entityRenderDispatcher, BlockEntityRenderDispatcher blockEntityDispatcher, RenderBuffers bufferBuilders, CallbackInfo ci) {
		this.planetarium$planetManager = new PlanetManager();
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(this.planetarium$planetManager);
	}

	@Inject(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getStarBrightness(F)F"))
	private void renderCelestialObjects(Matrix4f modelViewMatrix, Matrix4f projectionMatrix, float tickDelta, Camera preStep, boolean skipRendering, Runnable preRender, CallbackInfo ci) {
		PoseStack matrices = new PoseStack();
		matrices.mulPose(modelViewMatrix);

		assert level != null;
		for (Map.Entry<ResourceLocation, PlanetInfo> entry : planetarium$planetManager.getPlanets().entrySet()) {
			Planetarium.renderPlanet(matrices, entry.getKey(), entry.getValue(), tickDelta, level);
		}
	}
}
