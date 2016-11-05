package suite.jdk;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.Callable;

import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ClassCreatorTest implements Opcodes {

	@Test
	public void testCreateClass() throws Exception {
		assertEquals("", create());
	}

	private Object create() throws Exception {
		ClassWriter cw = new ClassWriter(0);

		cw.visit(49, ACC_PUBLIC + ACC_SUPER, "Hello", null, "java/lang/Object",
				new String[] { "java/util/concurrent/Callable", });

		{
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}

		{
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "call", "()Ljava/lang/Object;", null, null);
			mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
			mv.visitLdcInsn("hello world");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
			mv.visitLdcInsn("");
			mv.visitInsn(ARETURN);
			mv.visitMaxs(2, 1);
			mv.visitEnd();
		}

		cw.visitEnd();

		byte bytes[] = cw.toByteArray();

		UnsafeUtil unsafeUtil = new UnsafeUtil();
		Callable<?> callable = unsafeUtil.defineClass(Callable.class, "Hello", bytes).newInstance();
		return callable.call();
	}

}
