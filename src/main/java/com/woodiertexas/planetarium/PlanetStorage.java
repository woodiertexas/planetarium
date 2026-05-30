package com.woodiertexas.planetarium;

import com.mojang.blaze3d.buffers.GpuBuffer;

public record PlanetStorage(PlanetInfo info, GpuBuffer vertices) {
}
