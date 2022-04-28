package cx.rain.silk.compat.mods.fabric_api.entity_events.fixes;

import cx.rain.silk.patcher.ClassFixer;
import me.modmuss50.optifabric.util.RemappingUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class LivingEntityFix implements ClassFixer {
	@Override
	public void fix(ClassNode classNode, ClassNode old) {
		for (MethodNode methodNode : classNode.methods) {
			for (int i = 0; i < methodNode.instructions.size(); i++) {
				AbstractInsnNode insnNode = methodNode.instructions.get(i);

				if (insnNode instanceof MethodInsnNode) {
					MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;

					if (methodInsnNode.getOpcode() == Opcodes.INVOKEVIRTUAL) {
						if ("lambda$11".equals(methodInsnNode.name)) {

							// class_2338: BlockPos.
							// class_1309: LivingEntity.
							// method_18405: lambda$11.

							String desc = "(Lnet/minecraft/class_2338;)Z";
							String name = RemappingUtils.getMethodName("class_1309", "method_18405", desc);

							System.out.println(String.format("Replaced `onIsSleepingInBed` call: %s.%s.", name, desc));

							//Replaces the method call with the vanilla one, this calls down to the same method just without the forge model data
							methodInsnNode.name = name;
							methodInsnNode.desc = RemappingUtils.mapMethodDescriptor(desc);

							//Remove the model data local load call
							methodNode.instructions.remove(methodNode.instructions.get(i - 1));
						}
					}
				}
			}
		}
	}
}
