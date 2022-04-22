package cx.rain.silk.mixin.entity;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.EntityLiving;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityLiving.class)
public abstract class MixinLivingEntity implements IMixinLivingEntity {
	@Shadow
	protected abstract boolean E();

	@Override
	public boolean isSleepingInBed(BlockPosition pos) {
		return E();
	}
}
