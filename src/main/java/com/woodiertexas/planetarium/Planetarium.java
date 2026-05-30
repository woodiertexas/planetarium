package com.woodiertexas.planetarium;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class Planetarium {
	public static final Logger LOGGER = LoggerFactory.getLogger("Planetarium");
	public static final String MOD_ID = "planetarium";

	private static final Map<Identifier, PlanetStorage> planets = new HashMap<>();

	public static void deleteAll() {
		planets.values().forEach(i -> i.vertices().close());
		planets.clear();
	}



	public static void preparePlanet(Map.Entry<Identifier, PlanetInfo> entry) {
		if (planets.containsKey(entry.getKey())) {
			return;
		}

		var planetInfo = entry.getValue();

		BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		bufferBuilder.addVertex(-planetInfo.size(), 99.0F, -planetInfo.size()).setUv(0.0F, 0.0F);
		bufferBuilder.addVertex(planetInfo.size(), 99.0F, -planetInfo.size()).setUv(1.0F, 0.0F); // u: 1.0
		bufferBuilder.addVertex(planetInfo.size(), 99.0F, planetInfo.size()).setUv(1.0F, 1.0F); // u: 1.0, v: 1.0
		bufferBuilder.addVertex(-planetInfo.size(), 99.0F, planetInfo.size()).setUv(0.0F, 1.0F); // v: 1.0

		var meshData = bufferBuilder.buildOrThrow();

		PlanetStorage storage = new PlanetStorage(planetInfo, RenderSystem.getDevice().createBuffer(() -> "Planet " + entry.getKey(), GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, meshData.vertexBuffer()));

		meshData.close();

		planets.put(entry.getKey(), storage);
	}

	/**
	 * @param pass       The rendering pass.
	 * @param id         The id for the planet. (Ex: Mercury, Venus, Earth, and so on)
	 * @param planetInfo The planet data.
	 * @param tickDelta  Time between ticks.
	 * @param world      The client world to render in.
	 */
	public static void renderPlanet(RenderPass pass, Identifier id, PlanetInfo planetInfo, float tickDelta, ClientLevel world) {
		var storage = planets.get(id);
		
		if (world.getDefaultClockTime() % 24000L >= 11800) {
			var tex = Minecraft.getInstance().getTextureManager().getTexture(planetInfo.getTexture(id));
			
			pass.bindTexture("Sampler0", tex.getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));

			pass.setVertexBuffer(0, storage.vertices());
			pass.setIndexBuffer(RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS).getBuffer(6), RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS).type());

			pass.drawIndexed(0, 0, 6, 1);
		}
	}
}
