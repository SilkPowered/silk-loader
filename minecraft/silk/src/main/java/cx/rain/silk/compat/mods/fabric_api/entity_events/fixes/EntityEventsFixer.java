package cx.rain.silk.compat.mods.fabric_api.entity_events.fixes;

import cx.rain.silk.patcher.BaseFixer;

public class EntityEventsFixer extends BaseFixer {
	@Override
	public void registerFixes() {
		registerFix("class_1309$method_18405", new LivingEntityFix());
	}
}
