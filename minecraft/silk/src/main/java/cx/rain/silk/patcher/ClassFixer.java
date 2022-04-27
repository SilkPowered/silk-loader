package cx.rain.silk.patcher;

import org.objectweb.asm.tree.ClassNode;

public interface ClassFixer {
	void fix(ClassNode classNode, ClassNode old);
}
