package com.woodiertexas.planetarium;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.woodiertexas.planetarium.PlanetInfo;
import com.woodiertexas.planetarium.Planetarium;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public class PlanetManager implements ResourceManagerReloadListener {
	private static final Gson GSON = new GsonBuilder().create();
	private Map<Identifier, com.woodiertexas.planetarium.PlanetInfo> planets;
	
	public Map<Identifier, com.woodiertexas.planetarium.PlanetInfo> getPlanets() {
		return planets;
	}

	@Override
	public void onResourceManagerReload(ResourceManager resourceManager) {
		Planetarium.deleteAll();
		
		Map<Identifier, com.woodiertexas.planetarium.PlanetInfo> planets = new HashMap<>();

		
		for (Map.Entry<Identifier, Resource> resourceEntry : resourceManager.listResources("planetarium/planets", i -> i.getPath().contains("json")).entrySet()) {
			Identifier id = resourceEntry.getKey();

			InputStream stream;
			
			try {
				stream = resourceEntry.getValue().open();  
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			
			var json = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
			DataResult<Pair<com.woodiertexas.planetarium.PlanetInfo, JsonElement>> result = com.woodiertexas.planetarium.PlanetInfo.CODEC.decode(JsonOps.INSTANCE, json);

			if (result.error().isPresent()) {
				com.woodiertexas.planetarium.Planetarium.LOGGER.error(String.format("Could not parse planet file %s.\nReason: %s", id, result.error().get().message()));
				continue;
			}

			PlanetInfo planetInfo = result.result().get().getFirst();

			if (resourceManager.getResource(planetInfo.getTexture(id)).isEmpty()) {
				com.woodiertexas.planetarium.Planetarium.LOGGER.error("No texture found for planet {}, skipping.", id);
				continue;
			}

			com.woodiertexas.planetarium.Planetarium.LOGGER.debug("Adding Planet {}: {}", id, planetInfo);
			planets.put(id, planetInfo);
		}
		
		this.planets = Map.copyOf(planets);
	}
}
