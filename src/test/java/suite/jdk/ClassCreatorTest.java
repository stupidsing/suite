package suite.jdk;

import static org.junit.Assert.assertEquals;

import java.io.PrintStream;

import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import suite.util.FunUtil.Source;

public class ClassCreatorTest implements Opcodes {

	@Test
	public void testCreateClass() throws Exception {
		assertEquals("", create());
	}

	private Object create() throws Exception {
		Class<?> sup = Object.class;
		@SuppressWarnings("rawtypes")
		Class<Source> iface = Source.class;

		ClassWriter cw = new ClassWriter(0);

		cw.visit(49, //
				ACC_PUBLIC + ACC_SUPER, //
				"HelloSource", //
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
					"source", //
					Type.getMethodDescriptor(Type.getType(Object.class)), //
					null, //
					null);

			mv.visitFieldInsn(GETSTATIC, Type.getInternalName(System.class), "out",
					Type.getDescriptor(PrintStream.class));
			mv.visitLdcInsn("hello world");
			mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(PrintStream.class), "println",
					Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)), false);
			mv.visitLdcInsn("");
			mv.visitInsn(ARETURN);
			mv.visitMaxs(2, 1);
			mv.visitEnd();
		}

		cw.visitEnd();

		byte bytes[] = cw.toByteArray();

		UnsafeUtil unsafeUtil = new UnsafeUtil();
		return unsafeUtil.defineClass(iface, "HelloSource", bytes).newInstance().source();
	}

}
