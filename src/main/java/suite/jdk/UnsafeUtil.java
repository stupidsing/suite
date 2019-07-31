package suite.jdk;

import static primal.statics.Rethrow.ex;

import java.lang.reflect.Method;

public class UnsafeUtil {

	public <T> Class<? extends T> defineClass(Class<T> interfaceClazz, String className, byte[] bytes) {
		return defineClass(interfaceClazz, className, bytes, null);
	}

	public <T> Class<? extends T> defineClass(Class<T> interfaceClazz, String className, byte[] bytes, Object[] array) {
		var unsafe = unsafe();
		@SuppressWarnings("unchecked")
		var clazz = (Class<? extends T>) ex(() -> defineAnonClass.invoke(unsafe, interfaceClazz, bytes, array));
		return clazz;
	}

	private Object unsafe() {
		var unsafeClazz = ex(() -> Class.forName("sun.misc.Unsafe"));
		if (unsafe == null)
			ex(() -> {
				var f = unsafeClazz.getDeclaredField("theUnsafe");
				f.setAccessible(true);
				defineAnonClass = (unsafe = f.get(null)) //
						.getClass() //
						.getMethod("defineAnonymousClass", Class.class, byte[].class, Object[].class);
				return unsafe;
			});
		return unsafe;
	}

	private Object unsafe;
	private Method defineAnonClass;

}
