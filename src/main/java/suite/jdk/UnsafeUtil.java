package suite.jdk;

import suite.util.FunUtil.Source;
import suite.util.Memoize;
import suite.util.Rethrow;
import sun.misc.Unsafe;

public class UnsafeUtil {

	public Class<?> defineClass(byte[] bytes) {
		var unsafe = source.source();
		return unsafe.defineAnonymousClass(getClass(), bytes, null);
	}

	public <T> Class<? extends T> defineClass(Class<T> interfaceClazz, String className, byte[] bytes) {
		return defineClass(interfaceClazz, className, bytes, null);
	}

	public <T> Class<? extends T> defineClass(Class<T> interfaceClazz, String className, byte[] bytes, Object[] array) {
		var unsafe = source.source();
		@SuppressWarnings("unchecked")
		Class<? extends T> clazz = (Class<? extends T>) unsafe.defineAnonymousClass(interfaceClazz, bytes, array);
		return clazz;
	}

	private static Source<Unsafe> source = Memoize.source(() -> Rethrow.ex(() -> {
		var f = Unsafe.class.getDeclaredField("theUnsafe");
		f.setAccessible(true);
		return (Unsafe) f.get(null);
	}));

}
