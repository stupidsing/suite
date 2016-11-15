package suite.jdk;

import java.lang.reflect.Field;

import suite.util.Rethrow;
import sun.misc.Unsafe;

public class UnsafeUtil {

	public Class<?> defineClass(byte bytes[]) {
		Unsafe unsafe = new UnsafeUtil().getUnsafe();
		return unsafe.defineAnonymousClass(getClass(), bytes, null);
	}

	public <T> Class<? extends T> defineClass(Class<T> interfaceClazz, String className, byte bytes[]) {
		Unsafe unsafe = new UnsafeUtil().getUnsafe();
		@SuppressWarnings("unchecked")
		Class<? extends T> clazz = (Class<? extends T>) unsafe.defineClass( //
				className, bytes, 0, bytes.length, getClass().getClassLoader(), null);
		return clazz;
	}

	public Unsafe getUnsafe() {
		return Rethrow.reflectiveOperationException(() -> {
			Field f = Unsafe.class.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			return (Unsafe) f.get(null);
		});
	}

}
