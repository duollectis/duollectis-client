package org.duollectis.client.mixin.modules.memoryoptimizer;

import net.minecraft.client.render.chunk.ChunkBuilder;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.modules.MemoryOptimizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ограничивает размер очереди задач перестройки чанков.
 * Каждая задача в очереди удерживает {@code BlockBufferAllocatorStorage} (~2-4 МБ нативной памяти).
 * При переполнении очереди новые задачи отбрасываются — чанки перестроятся позже,
 * когда очередь освободится, что резко снижает пиковое потребление памяти.
 */
@Mixin(ChunkBuilder.class)
public abstract class ChunkBuilderMixin {

	@Shadow
	private net.minecraft.client.render.chunk.ChunkRenderTaskScheduler scheduler;

	@Inject(method = "send", at = @At("HEAD"), cancellable = true)
	private void limitRebuildQueue(ChunkBuilder.BuiltChunk.Task task, CallbackInfo ci) {
		MemoryOptimizer module = ModuleManager.getInstance().get(MemoryOptimizer.class);

		if (module == null || !module.isActive() || !module.getLimitChunkQueue().get()) {
			return;
		}

		if (scheduler.size() >= module.getMaxChunkQueue().get()) {
			ci.cancel();
		}
	}

}
