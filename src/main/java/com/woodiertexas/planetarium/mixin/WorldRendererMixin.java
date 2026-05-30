package com.woodiertexas.planetarium.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.woodiertexas.planetarium.PlanetInfo;
import com.woodiertexas.planetarium.PlanetManager;
import com.woodiertexas.planetarium.Planetarium;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.OptionalInt;

@Mixin(LevelRenderer.class)
public class WorldRendererMixin {
	@Shadow
	private @Nullable ClientLevel level;

	@Unique
	private static PlanetManager planetarium$planetManager;
	
	static {
		planetarium$planetManager = new PlanetManager();
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(Identifier.fromNamespaceAndPath("planetarium", "listener"), planetarium$planetManager);
	}

	@Unique
	// Stolen from lowercasebtw on Fabricord; unsure if this is correct...
	private static float getTimeOfDay(Level level) {
		float sunAngle = Mth.frac(level.environmentAttributes().getDimensionValue(net.minecraft.world.attribute.EnvironmentAttributes.SUN_ANGLE) / 360.0F);
		double frac = Mth.frac(sunAngle - 0.25);
		return (float) (frac * 2.0 + (0.5 - Math.cos(frac * Math.PI) / 2.0)) / 3.0F;
	}

	@Inject(method = "lambda$addSkyPass$0", at = @At(value = "RETURN"))
	private static void renderCelestialObjects(GpuBufferSlice skyFog, SkyRenderState state, SkyRenderer skyRenderer, CallbackInfo ci) {
		PoseStack matrices = new PoseStack();
		matrices.mulPose(Minecraft.getInstance().gameRenderer.getGameRenderState().levelRenderState.cameraRenderState.viewRotationMatrix);

		var array = planetarium$planetManager.getPlanets().entrySet().toArray(Map.Entry[]::new);
		var array2 = new GpuBufferSlice[array.length];

		var tickDelta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);

		float rainGradient = 1.0f - Minecraft.getInstance().level.getRainLevel(tickDelta);
		float transparency = 2 * Minecraft.getInstance().gameRenderer.getMainCamera().attributeProbe().getValue(EnvironmentAttributes.STAR_BRIGHTNESS, tickDelta) * rainGradient;

		for (int i = 0; i < array.length; i++) {
			Map.Entry<Identifier, PlanetInfo> entry = array[i];
			Planetarium.preparePlanet(entry);
			
			matrices.pushPose();
			
			var planetInfo = entry.getValue();

			// First, line planet up where the sun is in the sky
			matrices.mulPose(Axis.YP.rotationDegrees(90.0F));

			// Second, change the orbital tilt of the planet
			matrices.mulPose(Axis.YP.rotationDegrees(planetInfo.tilt())); // tilt

			// Third, set the angle of the planet in the sky and offset it.
			matrices.mulPose(Axis.XP.rotationDegrees(-getTimeOfDay(Minecraft.getInstance().level) * 360.0F + planetInfo.procession())); // procession

			// Fourth, set the inclination of the planet.
			matrices.mulPose(Axis.ZP.rotationDegrees(planetInfo.inclination())); // inclination

			// Finally, change the rotation of the planet texture.
			matrices.mulPose(Axis.YP.rotationDegrees(planetInfo.texture_rotation()));

			array2[i] = RenderSystem.getDynamicUniforms()
				.writeTransform(matrices.last().pose(), transparency <= 0.0f ? new Vector4f(1.0F, 1.0F, 1.0F, 1.0f) : new Vector4f(transparency, transparency, transparency, transparency), new Vector3f(), new Matrix4f());

			var tex = Minecraft.getInstance().getTextureManager().getTexture(planetInfo.getTexture(entry.getKey())); // This acts as preloading, and is required.

			matrices.popPose();
		}

		
		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Planet pass", Minecraft.getInstance().getMainRenderTarget().getColorTextureView(), OptionalInt.empty())) {
			pass.setPipeline(RenderPipelines.CELESTIAL);
			RenderSystem.bindDefaultUniforms(pass);

			for (int i = 0; i < array.length; i++) {
				Map.Entry<Identifier, PlanetInfo> entry = array[i];
				pass.setUniform("DynamicTransforms", array2[i]);
				Planetarium.renderPlanet(pass, entry.getKey(), entry.getValue(), Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false), Minecraft.getInstance().level);
			}
		}
	}
}
