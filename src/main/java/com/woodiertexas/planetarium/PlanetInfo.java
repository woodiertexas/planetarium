package com.woodiertexas.planetarium;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

import net.minecraft.resources.Identifier;

/**
 * @param procession       How far along in orbit the planet is.
 * @param tilt             The orbital tilt of the planet.
 * @param inclination
 * @param texture_rotation The rotation of the planet's texture.
 * @param size             The planet's size. (1.0 = very small, 20.0 = big)
 * @param texture_override Optional, but this can be used to override the planet's texture.
 */
public record PlanetInfo(float procession, float tilt, float inclination, float texture_rotation, float size, Optional<Identifier> texture_override) {
	public static final Codec<PlanetInfo> CODEC = RecordCodecBuilder.create(instance ->
		instance.group(
			Codec.floatRange(-360.0f, 360.0f).fieldOf("procession").forGetter(PlanetInfo::procession),
			Codec.floatRange(-360.0f, 360.0f).fieldOf("tilt").forGetter(PlanetInfo::tilt),
			Codec.floatRange(-90.0F, 90.0F).fieldOf("inclination").forGetter(PlanetInfo::inclination),
			Codec.floatRange(-360.0f, 360.0f).fieldOf("texture_rotation").forGetter(PlanetInfo::texture_rotation),
			Codec.floatRange(0.1f, 750.0f).fieldOf("size").forGetter(PlanetInfo::size),
			Identifier.CODEC.optionalFieldOf("texture_override").forGetter(PlanetInfo::texture_override)
		).apply(instance, PlanetInfo::new)
	);

	public Identifier getTexture(Identifier id) {
		id = Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath().replace(".json", ""));
		if (texture_override.isEmpty()) {
			return id.withPrefix("textures/").withSuffix(".png");
		}
		
		return texture_override.get().withPrefix("textures/").withSuffix(".png");
	}
}
