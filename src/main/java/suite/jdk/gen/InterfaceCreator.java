package suite.jdk.gen;

import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import suite.jdk.UnsafeUtil;
import suite.streamlet.Read;

public class InterfaceCreator implements Opcodes {

	private static AtomicInteger counter = new AtomicInteger();

	public static Class<?> of(String parameterTypes[], String returnType) {
		String className = "I" + counter.getAndIncrement();
		Class<Object> superClass = Object.class;

		String md = Type.getMethodDescriptor( //
				Type.getType(returnType), //
				Read.from(parameterTypes).map(Type::getType).toList().toArray(new Type[0]));

		ClassWriter cw = new ClassWriter(0);

		cw.visit(V1_8, //
				ACC_ABSTRACT | ACC_INTERFACE | ACC_PUBLIC, //
				className, //
				null, //
				Type.getInternalName(superClass), //
				new String[0]);

		cw.visitMethod( //
				ACC_ABSTRACT | ACC_PUBLIC, //
				"apply", //
				md, //
				null, //
				null);

		cw.visitEnd();

		byte bytes[] = cw.toByteArray();

		return new UnsafeUtil().defineClass(superClass, className, bytes);
	}

}
