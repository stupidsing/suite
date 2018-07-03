package suite.jdk;

import static suite.util.Friends.rethrow;

import sun.misc.Unsafe;

public class UnsafeUtil {

	public Class<?> defineClass(byte[] bytes) {
		return unsafe().defineAnonymousClass(getClass(), bytes, null);
	}

	public <T> Class<? extends T> defineClass(Class<T> interfaceClazz, String className, byte[] bytes) {
		return defineClass(interfaceClazz, className, bytes, null);
	}

	public <T> Class<? extends T> defineClass(Class<T> interfaceClazz, String className, byte[] bytes, Object[] array) {
		@SuppressWarnings("unchecked")
		var clazz = (Class<? extends T>) unsafe().defineAnonymousClass(interfaceClazz, bytes, array);
		return clazz;
	}

	private Unsafe unsafe() {
		if (unsafe == null)
			rethrow(() -> {
				var f = Unsafe.class.getDeclaredField("theUnsafe");
				f.setAccessible(true);
				return unsafe = (Unsafe) f.get(null);
			});
		return unsafe;
	}

	private Unsafe unsafe;

}
