package cx.rain.silk.mixin.entity;

import net.minecraft.core.BlockPosition;

public interface IMixinLivingEntity {
	boolean isSleepingInBed(BlockPosition pos);
}
