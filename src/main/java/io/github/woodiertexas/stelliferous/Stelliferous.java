package io.github.woodiertexas.stelliferous;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Stelliferous implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("Stelliferous");
	public static final String MODID = "stelliferous";
	
	@Override
	public void onInitialize(ModContainer mod) {
		LOGGER.info("Hello Quilt world from {}!", mod.metadata().name());
	}
}
