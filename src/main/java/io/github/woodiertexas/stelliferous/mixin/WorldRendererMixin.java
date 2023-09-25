package io.github.woodiertexas.stelliferous.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferRenderer;
import com.mojang.blaze3d.vertex.Tessellator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormats;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Axis;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static io.github.woodiertexas.stelliferous.Stelliferous.MODID;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
	@Shadow
	private @Nullable ClientWorld world;
	
	@Unique
	private static final Identifier TEST_PLANET = new Identifier(MODID, "textures/environment/lil_pineapple.png");
	
	@Unique
	BufferBuilder bufferBuilder = Tessellator.getInstance().getBufferBuilder();
	
	@Inject(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getStarBrightness(F)F"))
	public void stelliferous$inject(MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta, Camera preStep, boolean skipRendering, Runnable preRender, CallbackInfo ci) {
		matrices.push();
		ClientWorld world = this.world;
		
		assert world != null;
		matrices.multiply(Axis.X_POSITIVE.rotationDegrees(world.getSkyAngle(tickDelta) * 36000.0F + 80));
		matrices.translate(100.0, 0, 0);
		
		Matrix4f matrix4f = matrices.peek().getModel();
		float PLANET_SIZE = 25.0f;
		RenderSystem.setShaderTexture(0, TEST_PLANET);
		
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
		//matrices.multiply(Axis.Y_POSITIVE.rotationDegrees(-90.0F));
		bufferBuilder.vertex(matrix4f, -PLANET_SIZE, -100.0F, PLANET_SIZE).uv(0.0F, 0.0F).next();
		bufferBuilder.vertex(matrix4f, PLANET_SIZE, -100.0F, PLANET_SIZE).uv(1.0F, 0.0F).next();
		bufferBuilder.vertex(matrix4f, PLANET_SIZE, -100.0F, -PLANET_SIZE).uv(1.0F, 1.0F).next();
		bufferBuilder.vertex(matrix4f, -PLANET_SIZE, -100.0F, -PLANET_SIZE).uv(0.0F, 1.0F).next();
		BufferRenderer.drawWithShader(bufferBuilder.end());
		matrices.pop();
	}
}
