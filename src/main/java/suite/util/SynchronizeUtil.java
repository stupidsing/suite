package suite.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class SynchronizeUtil {

	public static <I> I proxy(Class<I> interface_, final I object) {
		@SuppressWarnings("unchecked")
		final Class<I> clazz = (Class<I>) object.getClass();
		ClassLoader classLoader = clazz.getClassLoader();
		Class<?> classes[] = { interface_ };

		InvocationHandler handler = new InvocationHandler() {
			public Object invoke(Object proxy, Method method, Object ps[]) throws Exception {
				synchronized (object) {
					try {
						return method.invoke(object, ps);
					} catch (InvocationTargetException ite) {
						Throwable th = ite.getTargetException();
						throw th instanceof Exception ? (Exception) th : ite;
					}
				}
			}
		};

		@SuppressWarnings("unchecked")
		I proxied = (I) Proxy.newProxyInstance(classLoader, classes, handler);
		return proxied;
	}

}
