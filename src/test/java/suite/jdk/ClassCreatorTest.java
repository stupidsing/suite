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
		ClassWriter cw = new ClassWriter(0);

		cw.visit(49, //
				ACC_PUBLIC + ACC_SUPER, //
				"HelloSource", //
				null, //
				Type.getInternalName(Object.class), //
				new String[] { Type.getInternalName(Source.class), });

		{
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(Object.class), "<init>", "()V", false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}

		{
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, //
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
		Source<?> callable = unsafeUtil.defineClass(Source.class, "HelloSource", bytes).newInstance();
		return callable.source();
	}

}
