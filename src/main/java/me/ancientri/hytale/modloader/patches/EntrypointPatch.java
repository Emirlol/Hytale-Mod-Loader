package me.ancientri.hytale.modloader.patches;

import me.ancientri.hytale.modloader.HytaleHooks;
import net.fabricmc.loader.impl.game.patch.GamePatch;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.function.Function;

public class EntrypointPatch extends GamePatch {
	private final String lateMain;

	public EntrypointPatch(String lateMain) {
		this.lateMain = lateMain;
	}

	@Override
	public void process(FabricLauncher launcher, Function<String, ClassNode> classSource, Consumer<ClassNode> classEmitter) {
		ClassNode mainClass = classSource.apply(lateMain);
		if (mainClass == null) {
			throw new RuntimeException("Could not load late main class `" + lateMain + "`!");
		}

		MethodNode mainMethod = findMethod(mainClass, methodNode -> methodNode.name.equals("lateMain"));
		if (mainMethod == null) {
			throw new RuntimeException("Could not find `lateMain` method in class `" + lateMain + "`!");
		}
		var iterator = mainMethod.instructions.iterator();

		patchPreInit(mainMethod, iterator);

		classEmitter.accept(mainClass);
	}

	private MethodInsnNode findStaticMethodCall(MethodNode methodNode, String methodName) {
		var node = findInsn(methodNode,
				abstractInsnNode ->
						abstractInsnNode instanceof MethodInsnNode methodInsnNode
								&& methodInsnNode.getOpcode() == Opcodes.INVOKESTATIC
								&& methodInsnNode.name.equals(methodName)
				, false);

		if (!(node instanceof MethodInsnNode methodInsnNode)) { // Doubles as a null check
			throw new RuntimeException("Could not find `" + methodName + "` method call in start method `" + methodNode.name + "`!");
		}
		return methodInsnNode;
	}

	private MethodInsnNode findCtor(MethodNode methodNode, String ownerName, String methodName) {
		var node = findInsn(methodNode,
				abstractInsnNode ->
						abstractInsnNode instanceof MethodInsnNode methodInsnNode
								&& methodInsnNode.getOpcode() == Opcodes.INVOKESPECIAL
								&& methodInsnNode.owner.equals(ownerName)
								&& methodInsnNode.name.equals(methodName)
				, false);

		if (!(node instanceof MethodInsnNode methodInsnNode)) { // Doubles as a null check
			throw new RuntimeException("Could not find `" + methodName + "` method call in start method `" + methodNode.name + "`!");
		}
		return methodInsnNode;
	}


	private MethodInsnNode createStaticMethodCall(String methodName) {
		return new MethodInsnNode(Opcodes.INVOKESTATIC, HytaleHooks.INTERNAL_NAME, methodName, "()V", false);
	}

	private void patchPreInit(MethodNode mainMethod, ListIterator<AbstractInsnNode> iterator) {
		var node = findCtor(mainMethod, "com/hypixel/hytale/server/core/HytaleServer", "<init>"); // new HytaleServer();
		moveBefore(iterator, node);
		var preInitCall = createStaticMethodCall("preInit");
		iterator.add(preInitCall);
	}
}
