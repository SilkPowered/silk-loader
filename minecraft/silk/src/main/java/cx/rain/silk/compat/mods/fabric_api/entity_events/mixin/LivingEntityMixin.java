package cx.rain.silk.compat.mods.fabric_api.entity_events.mixin;

import me.modmuss50.optifabric.compat.InterceptingMixin;
import me.modmuss50.optifabric.compat.PlacatingSurrogate;
import me.modmuss50.optifabric.compat.Shim;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
@InterceptingMixin("net/fabricmc/fabric/mixin/entity/event/LivingEntityMixin")
public class LivingEntityMixin {
	@PlacatingSurrogate
	private void onIsSleepingInBed(BlockPos sleepingPos, CallbackInfoReturnable<Boolean> info) {
	}

	@Dynamic("checkBedExists: Synthetic lambda body for Optional.map in checkBedExists")
	@Inject(method = "checkBedExists", at = @At("RETURN"), cancellable = true)
	private void onIsSleepingInBed(Vec3 sleepingPos, CallbackInfoReturnable<Boolean> info) {
		onIsSleepingInBed(new BlockPos(sleepingPos), info);
	}
}
