package suite.util;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

public class UnsafeUtil {

	public Class<?> defineClass(byte bytes[]) {
		Unsafe unsafe = new UnsafeUtil().getUnsafe();
		return unsafe.defineAnonymousClass(getClass(), bytes, null);
	}

	public Class<?> defineClass(String className, byte bytes[]) {
		Unsafe unsafe = new UnsafeUtil().getUnsafe();
		return unsafe.defineClass(className, bytes, 0, bytes.length, getClass().getClassLoader(), null);
	}

	public Unsafe getUnsafe() {
		try {
			Field f = Unsafe.class.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			return (Unsafe) f.get(null);
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException(ex);
		}
	}
}
