package suite.jdk;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import suite.util.FunUtil.Fun;
import suite.util.Rethrow;

public class ClassCreator implements Opcodes {

	private AtomicInteger counter = new AtomicInteger();

	public Object create() {
		return Rethrow.ex(() -> create("Fun" + counter.getAndIncrement()));

	}

	public Object create(String name) throws Exception {
		Class<?> sup = Object.class;
		@SuppressWarnings("rawtypes")
		Class<Fun> iface = Fun.class;

		ClassWriter cw = new ClassWriter(0);

		cw.visit(49, //
				ACC_PUBLIC + ACC_SUPER, //
				name, //
				null, //
				Type.getInternalName(sup), //
				new String[] { Type.getInternalName(iface), });

		{
			MethodVisitor mv = cw.visitMethod( //
					ACC_PUBLIC, //
					"<init>", //
					Type.getMethodDescriptor(Type.VOID_TYPE), //
					null, //
					null);

			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(sup), "<init>",
					Type.getConstructorDescriptor(sup.getConstructor()), false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}

		{
			MethodVisitor mv = cw.visitMethod( //
					ACC_PUBLIC, //
					"apply", //
					Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(Object.class)), //
					null, //
					null);

			mv.visitFieldInsn(GETSTATIC, Type.getInternalName(System.class), "out", Type.getDescriptor(PrintStream.class));
			mv.visitVarInsn(ALOAD, 1);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}

		{
			MethodVisitor mv = cw.visitMethod( //
					ACC_PUBLIC, //
					"hello", //
					Type.getMethodDescriptor(Type.VOID_TYPE), //
					null, //
					null);

			mv.visitFieldInsn(GETSTATIC, Type.getInternalName(System.class), "out", Type.getDescriptor(PrintStream.class));
			mv.visitLdcInsn("hello world");
			mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(PrintStream.class), "println",
					Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)), false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(2, 1);
			mv.visitEnd();
		}

		cw.visitEnd();

		byte bytes[] = cw.toByteArray();

		UnsafeUtil unsafeUtil = new UnsafeUtil();
		@SuppressWarnings("unchecked")
		Fun<Object, Object> fun = (Fun<Object, Object>) unsafeUtil.defineClass(iface, name, bytes).newInstance();
		return fun.apply("Hello");
	}

}
