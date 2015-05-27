package suite.jdk;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.Callable;

import org.bridj.relocated.org.objectweb.asm.ClassWriter;
import org.bridj.relocated.org.objectweb.asm.MethodVisitor;
import org.bridj.relocated.org.objectweb.asm.Opcodes;
import org.junit.Test;

public class ClassCreatorTest {

	private class Creator implements Opcodes {
		private Object create() throws Exception {
			ClassWriter cw = new ClassWriter(0);

			cw.visit(49, ACC_PUBLIC + ACC_SUPER, "Hello", null, "java/lang/Object",
					new String[] { "java/util/concurrent/Callable" });
			cw.visitSource("Hello.java", null);

			{
				MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
				mv.visitInsn(RETURN);
				mv.visitMaxs(1, 1);
				mv.visitEnd();
			}

			{
				MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "call", "()Ljava/lang/Object;", null, null);
				mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
				mv.visitLdcInsn("hello world");
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
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

	@Test
	public void testCreateClass() throws Exception {
		assertEquals("", new Creator().create());
	}

}
