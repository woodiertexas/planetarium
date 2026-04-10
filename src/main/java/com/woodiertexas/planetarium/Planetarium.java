package com.woodiertexas.planetarium;

import com.woodiertexas.planetarium.PlanetInfo;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;

public class Planetarium {
	public static final Logger LOGGER = LoggerFactory.getLogger("Planetarium");
	public static final String MOD_ID = "planetarium";

	/**
	 * @param matrices   The matrix stack for rendering.
	 * @param id         The id for the planet. (Ex: Mercury, Venus, Earth, and so on)
	 * @param planetInfo The planet data.
	 * @param tickDelta  Time between ticks.
	 * @param world      The client world to render in.
	 */
	public static void renderPlanet(PoseStack matrices, ResourceLocation id, PlanetInfo planetInfo, float tickDelta, ClientLevel world) {
		matrices.pushPose();
		
		// First, line planet up where the sun is in the sky
		matrices.mulPose(Axis.YP.rotationDegrees(90.0F));
		
		// Second, change the orbital tilt of the planet
		matrices.mulPose(Axis.YP.rotationDegrees(planetInfo.tilt())); // tilt
		
		// Third, set the angle of the planet in the sky and offset it.
		matrices.mulPose(Axis.XP.rotationDegrees(-world.getTimeOfDay(tickDelta) * 360.0F + planetInfo.procession())); // procession
		
		// Fourth, set the inclination of the planet.
		matrices.mulPose(Axis.ZP.rotationDegrees(planetInfo.inclination())); // inclination
		
		// Finally, change the rotation of the planet texture.
		matrices.mulPose(Axis.YP.rotationDegrees(planetInfo.texture_rotation()));
		
		if (world.getDayTime() % 24000L >= 11800) {
			Matrix4f matrix4f = matrices.last().pose();
			RenderSystem.setShaderTexture(0, planetInfo.getTexture(id));
			BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
			bufferBuilder.addVertex(matrix4f, -planetInfo.size(), 99.0F, -planetInfo.size()).setUv(0.0F, 0.0F);
			bufferBuilder.addVertex(matrix4f, planetInfo.size(), 99.0F, -planetInfo.size()).setUv(1.0F, 0.0F); // u: 1.0
			bufferBuilder.addVertex(matrix4f, planetInfo.size(), 99.0F, planetInfo.size()).setUv(1.0F, 1.0F); // u: 1.0, v: 1.0
			bufferBuilder.addVertex(matrix4f, -planetInfo.size(), 99.0F, planetInfo.size()).setUv(0.0F, 1.0F); // v: 1.0
			
			float rainGradient = 1.0f - world.getRainLevel(tickDelta);
			float transparency = 2 * world.getStarBrightness(tickDelta) * rainGradient;
			if (transparency > 0.0f) {
				RenderSystem.setShaderColor(transparency, transparency, transparency, transparency);
			}
			
			BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
		}
		matrices.popPose();
	}
}
