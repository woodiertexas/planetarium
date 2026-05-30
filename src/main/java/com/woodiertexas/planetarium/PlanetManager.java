package com.woodiertexas.planetarium;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class PlanetManager implements ResourceManagerReloadListener {
	private static final Gson GSON = new GsonBuilder().create();
	private Map<Identifier, PlanetInfo> planets;
	
	public Map<Identifier, PlanetInfo> getPlanets() {
		return planets;
	}

	@Override
	public void onResourceManagerReload(ResourceManager resourceManager) {
		Planetarium.deleteAll();
		
		Map<Identifier, PlanetInfo> planets = new HashMap<>();
		
		for (Map.Entry<Identifier, Resource> resourceEntry : resourceManager.listResources("planetarium/planets", i -> i.getPath().contains("json")).entrySet()) {
			Identifier id = resourceEntry.getKey();

			InputStream stream;

			try {
				stream = resourceEntry.getValue().open();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			var json = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
			DataResult<Pair<PlanetInfo, JsonElement>> result = PlanetInfo.CODEC.decode(JsonOps.INSTANCE, json);

			if (result.error().isPresent()) {
				Planetarium.LOGGER.error(String.format("Could not parse planet file %s.\nReason: %s", id, result.error().get().message()));
				continue;
			}

			PlanetInfo planetInfo = null;
			if (result.result().isPresent()) {
				planetInfo = result.result().get().getFirst();
			}

			if (resourceManager.getResource(planetInfo.getTexture(id)).isEmpty()) {
				Planetarium.LOGGER.error("No texture found for planet {}, skipping.", id);
				continue;
			}

			Planetarium.LOGGER.debug("Adding Planet {}: {}", id, planetInfo);
			planets.put(id, planetInfo);
		}
		
		this.planets = Map.copyOf(planets);
	}
}
