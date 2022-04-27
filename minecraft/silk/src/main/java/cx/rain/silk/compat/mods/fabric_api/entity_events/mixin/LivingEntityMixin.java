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
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(LivingEntity.class)
@InterceptingMixin("net/fabricmc/fabric/mixin/entity/event/LivingEntityMixin")
public class LivingEntityMixin {
	@Shim
	private native void onIsSleepingInBed(BlockPos sleepingPos, CallbackInfoReturnable<Boolean> info);

	@PlacatingSurrogate
	private void checkBedExists(BlockPos sleepingPos, CallbackInfoReturnable<Boolean> info) {
	}

	@Inject(method = {"checkBedExists"}, at = @At(value = "RETURN"), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
	@Dynamic(value = "replaceOnIsSleepingInBed: Synthetic lambda body for Optional.map in checkBedExists")
	private void replaceOnIsSleepingInBed(Vec3 sleepingPos, CallbackInfoReturnable<Boolean> info) {
		onIsSleepingInBed(new BlockPos(sleepingPos), info);
	}
}
